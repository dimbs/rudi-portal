package org.rudi.microservice.kalim.service.admin.impl;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.ObjectUtils;
import org.rudi.common.facade.util.UtilPageable;
import org.rudi.common.service.exception.AppServiceException;
import org.rudi.facet.acl.bean.Role;
import org.rudi.facet.acl.bean.User;
import org.rudi.facet.acl.bean.UserType;
import org.rudi.facet.acl.helper.ACLHelper;
import org.rudi.facet.apigateway.exceptions.DeleteApiException;
import org.rudi.facet.dataverse.api.dataset.DatasetOperationAPI;
import org.rudi.facet.dataverse.api.exceptions.DataverseAPIException;
import org.rudi.facet.dataverse.api.exceptions.DataverseMappingException;
import org.rudi.facet.dataverse.bean.DatasetVersion;
import org.rudi.facet.dataverse.bean.SearchDatasetInfo;
import org.rudi.facet.dataverse.bean.SearchType;
import org.rudi.facet.dataverse.model.search.SearchElements;
import org.rudi.facet.dataverse.model.search.SearchParams;
import org.rudi.facet.dataverse.utils.MetadataListUtils;
import org.rudi.facet.dataverse.utils.SearchElementsUtils;
import org.rudi.facet.kaccess.bean.DatasetSearchCriteria;
import org.rudi.facet.kaccess.bean.Metadata;
import org.rudi.facet.kaccess.bean.MetadataListFacets;
import org.rudi.facet.kaccess.bean.ReferenceDates;
import org.rudi.facet.kaccess.helper.dataset.metadatablock.MetadataBlockHelper;
import org.rudi.facet.kaccess.service.dataset.DatasetService;
import org.rudi.facet.organization.bean.Organization;
import org.rudi.facet.organization.bean.OrganizationMember;
import org.rudi.facet.organization.bean.OrganizationRole;
import org.rudi.facet.organization.helper.OrganizationHelper;
import org.rudi.facet.organization.helper.exceptions.AddUserToOrganizationException;
import org.rudi.facet.organization.helper.exceptions.GetOrganizationException;
import org.rudi.facet.providers.helper.ProviderHelper;
import org.rudi.microservice.apigateway.core.bean.Api;
import org.rudi.microservice.kalim.core.bean.IntegrationRequest;
import org.rudi.microservice.kalim.core.bean.IntegrationRequestSearchCriteria;
import org.rudi.microservice.kalim.core.bean.IntegrationStatus;
import org.rudi.microservice.kalim.core.bean.Method;
import org.rudi.microservice.kalim.core.bean.OrganizationsReparationReport;
import org.rudi.microservice.kalim.service.admin.AdminService;
import org.rudi.microservice.kalim.service.helper.apigateway.ApiGatewayManagerHelper;
import org.rudi.microservice.kalim.service.integration.IntegrationRequestService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminServiceImpl implements AdminService {
	private final DatasetOperationAPI datasetOperationAPI;
	private final Collection<ResourceRepairer> resourceRepairers;
	private final MetadataBlockHelper metadataBLockHelper;
	private final DatasetService datasetService;
	private final ApiGatewayManagerHelper apiGatewayManagerHelper;
	private final ProviderHelper providerHelper;
	private final IntegrationRequestService integrationRequestService;
	private final UtilPageable utilPageable;
	private final OrganizationHelper organizationHelper;
	private final ACLHelper aclHelper;

	@Value("${dataverse.api.rudi.data.alias}")
	private String rudiAlias;

	@Value("${default.organization.roles:USER,ORGANIZATION}")
	private List<String> defaultOrganizationRoles;

	@Value("${default.organization.password:12345678Mm$}")
	private String defaultOrganizationPassword;

	@Override
	public void repairResources() throws DataverseAPIException {

		final Map<String, DatasetVersion> repairedResourcesByPersistentId = new HashMap<>();

		for (final var resourceRepairer : resourceRepairers) {
			log.info("Launching repair with " + resourceRepairer.getClass().getName());

			final var query = resourceRepairer.getQuery();

			final var searchDatasetInfos = SearchElementsUtils
					.fetchAllElementsUsing(offset -> fetchPageAtOffset(query, offset));
			log.info(searchDatasetInfos.size() + " resource(s) to repair...");
			for (final var searchDatasetInfo : searchDatasetInfos) {
				final var persistentId = searchDatasetInfo.getGlobalId();

				log.info("Repairing resource " + persistentId);

				final DatasetVersion datasetVersion;
				if (repairedResourcesByPersistentId.containsKey(persistentId)) {
					datasetVersion = repairedResourcesByPersistentId.get(persistentId);
				} else {
					final var dataset = datasetOperationAPI.getDataset(persistentId);
					datasetVersion = dataset.getLatestVersion();
				}

				resourceRepairer.repairResource(datasetVersion);
				log.info("Resource repaired and waiting to be updated");

				repairedResourcesByPersistentId.put(persistentId, datasetVersion);

			}
		}

		updateRepairedResources(repairedResourcesByPersistentId);

	}

	@Override
	public OrganizationsReparationReport repairOrganizations() throws AppServiceException {

		log.info("Launching repair organzations");

		OrganizationsReparationReport report = new OrganizationsReparationReport();
		report.setOrganizationsRepaired(new ArrayList<>());
		report.setPasswordUsed(defaultOrganizationPassword);

		List<Organization> organizations;
		try {
			organizations = organizationHelper.getAllOrganizations();
		} catch (GetOrganizationException e) {
			throw new AppServiceException("Erreur while repairing organizations", e);
		}

		for (Organization organization : organizations) {
			String organizationUuid = organization.getUuid().toString();
			User organizationUser = aclHelper.getUserByLogin(organizationUuid);
			if (organizationUser == null) {
				log.info("Repairing organization : " + organizationUuid);
				report.getOrganizationsRepaired().add(organization.getUuid());
				User createdUser = new User();
				createdUser.setLogin(organizationUuid);
				createdUser.setPassword(report.getPasswordUsed());
				createdUser.setType(UserType.ROBOT);
				List<Role> roles = aclHelper.searchRoles().stream()
						.filter(element -> defaultOrganizationRoles.contains(element.getCode()))
						.collect(Collectors.toList());
				createdUser.setRoles(roles);
				aclHelper.createUser(createdUser);
				organizationUser = createdUser;
			}

			OrganizationMember member = new OrganizationMember().userUuid(organizationUser.getUuid())
					.role(OrganizationRole.EDITOR).addedDate(LocalDateTime.now());
			try {
				organizationHelper.addMemberToOrganization(member, organization.getUuid());
			} catch (AddUserToOrganizationException exception) {
				log.info("L'utilisateur ROBOT existe déjà pour cette organisation");
			}
		}

		log.info("End of organizations reparation, users created having password : " + report.getPasswordUsed());
		log.info("organizations repaired : " + report.getOrganizationsRepaired().toString());
		return report;
	}

	private void updateRepairedResources(Map<String, DatasetVersion> repairedResourcesByPersistentId) {
		val total = repairedResourcesByPersistentId.size();
		int i = 1;
		for (final var entry : repairedResourcesByPersistentId.entrySet()) {
			final var persistentId = entry.getKey();
			final var repairedDatasetVersion = entry.getValue();

			try {
				log.info("Updating resource {}/{}: {}", i, total, persistentId);

				final var updatableDatasetVersion = makeResourceUpdatable(repairedDatasetVersion, persistentId);

				datasetOperationAPI.updateDataset(updatableDatasetVersion, persistentId);

				log.info("Resource updated!");
			} catch (Exception e) {
				log.error("Failed to make repaired resource updatable : " + persistentId, e);
			}

			++i;
		}
	}

	private SearchElements<SearchDatasetInfo> fetchPageAtOffset(String query, Integer offset) {
		final var searchParams = SearchParams.builder().q(query).type(EnumSet.of(SearchType.DATASET)).subtree(rudiAlias)
				.start(offset).build();

		final SearchElements<SearchDatasetInfo> searchDatasetInfoElements;
		try {
			searchDatasetInfoElements = datasetOperationAPI.searchDataset(searchParams);
		} catch (DataverseAPIException e) {
			log.error("Failed to fetch page with offset=" + offset);
			throw new RuntimeException(e);
		}
		return searchDatasetInfoElements;
	}

	/**
	 * Corriger les champs, si nécessaire, pour permettre la sauvegarde de la ressource précédemment réparée
	 */
	private DatasetVersion makeResourceUpdatable(DatasetVersion repairedDatasetVersion, String persistentId)
			throws DataverseMappingException {

		final var metadata = metadataBLockHelper
				.datasetMetadataBlockToMetadata(repairedDatasetVersion.getMetadataBlocks(), persistentId);
		boolean modified = false;

		for (final var media : metadata.getAvailableFormats()) {
			if (media.getMediaDates() == null) {
				media.setMediaDates(new ReferenceDates());
				modified = true;
			}
			final var mediaDates = media.getMediaDates();

			if (mediaDates.getCreated() == null) {
				mediaDates.setCreated(OffsetDateTime.now());
				modified = true;
			}
			if (mediaDates.getUpdated() == null) {
				mediaDates.setUpdated(OffsetDateTime.now());
				modified = true;
			}
		}

		if (modified) {
			final var datasetMetadataBlock = metadataBLockHelper.metadataToDatasetMetadataBlock(metadata);
			return new DatasetVersion().id(repairedDatasetVersion.getId())
					.datasetId(repairedDatasetVersion.getDatasetId())
					.datasetPersistentId(repairedDatasetVersion.getDatasetPersistentId())
					.versionState(repairedDatasetVersion.getVersionState())
					.createTime(repairedDatasetVersion.getCreateTime())
					.lastUpdateTime(repairedDatasetVersion.getLastUpdateTime())
					.versionNumber(repairedDatasetVersion.getVersionNumber())
					.versionMinorNumber(repairedDatasetVersion.getVersionMinorNumber())
					.metadataBlocks(datasetMetadataBlock).files(repairedDatasetVersion.getFiles());
		} else {
			return repairedDatasetVersion;
		}
	}

	@Override
	public void createMissingApis(List<UUID> globalIds) {
		log.info("Starting createMissingApis...");
		fetchAllMetadata(globalIds).map(this::synchronizeApis).blockLast();
		log.info("createMissingApis terminated.");
	}

	@Nonnull
	private Flux<Metadata> fetchAllMetadata(List<UUID> globalIds) {
		final Function<MetadataListFacets, List<Metadata>> pageContentGetter = page -> page.getMetadataList()
				.getItems();
		return MetadataListUtils.fluxAllElementsUsing(offset -> fetchMetadataPage(globalIds, offset), pageContentGetter,
				this::computeNextPageOffset);
	}

	@Nullable
	private Integer computeNextPageOffset(MetadataListFacets page) {
		final var size = page.getMetadataList().getItems().size();
		if (size == 0) {
			return null;
		}
		return Math.toIntExact(page.getMetadataList().getOffset() + size);
	}

	private Mono<MetadataListFacets> fetchMetadataPage(List<UUID> globalIds, int offset) {
		final var datasetSearchCriteria = new DatasetSearchCriteria()
				.globalIds(CollectionUtils.isEmpty(globalIds) ? null : globalIds).offset(offset);
		final List<String> facets = Collections.emptyList();
		try {
			log.info("Fetching Datasets at offset {}...", offset);
			return Mono.just(datasetService.searchDatasets(datasetSearchCriteria, facets));
		} catch (DataverseAPIException e) {
			log.error("Cannot fetch Datasets at offset {}", offset, e);
			throw new RuntimeException(e);
		}
	}

	private List<Api> synchronizeApis(Metadata metadata) {
		try {
			log.info("Synchronize apis for dataset {}.", metadata.getGlobalId());

			final var medias = metadata.getAvailableFormats();
			if (CollectionUtils.isEmpty(medias)) {
				return Collections.emptyList();
			}

			List<Api> apis = apiGatewayManagerHelper.synchronizeMedias(metadata);

			log.info("Synchronize apis {} for dataset {} done.", apis.size(), metadata.getGlobalId());

			return apis;

		} catch (Exception e) {
			log.error("Failed to synchronize apis for dataset {}.", metadata.getGlobalId(), e);
			return Collections.emptyList();
		}
	}


	@Nonnull
	private UUID getProviderUuid(Metadata metadata) {
		return ObjectUtils.getFirstNonNull(() -> getProviderUuidFromMetadataInfo(metadata),
				() -> getProviderUuidFromIntegrationRequest(metadata), providerHelper::getDefaultProviderUuid);
	}

	@Nullable
	private UUID getProviderUuidFromMetadataInfo(Metadata metadata) {
		final var metadataProvider = metadata.getMetadataInfo().getMetadataProvider();
		return metadataProvider != null ? metadataProvider.getOrganizationId() : null;
	}

	@Nullable
	private UUID getProviderUuidFromIntegrationRequest(Metadata metadata) {
		final var integrationRequestSearchCriteria = IntegrationRequestSearchCriteria.builder()
				.globalId(metadata.getGlobalId()).integrationStatus(IntegrationStatus.OK).build();

		final var limit = 100;
		final var pageable = utilPageable.getPageable(0, limit, null);

		final var integrationRequests = integrationRequestService
				.searchIntegrationRequests(integrationRequestSearchCriteria, pageable);

		if (integrationRequests.getTotalElements() > limit) {
			throw new NotImplementedException("Pagination not implemented");
		}

		final var integrationRequest = integrationRequests.getContent().stream()
				.filter(req -> req.getMethod() == Method.POST || req.getMethod() == Method.PUT)
				.max(Comparator.comparing(IntegrationRequest::getTreatmentDate)).orElse(null);

		return integrationRequest != null ? integrationRequest.getNodeProviderId() : null;
	}

	@Override
	public void deleteAllApis() {
		try {
			apiGatewayManagerHelper.deleteApis();
		} catch (DeleteApiException e) {
			log.error("Failed to delete all apis", e);
		}

	}
}
