package org.rudi.microservice.projekt.facade.controller;

import java.util.UUID;

import org.rudi.common.facade.util.UtilPageable;
import org.rudi.common.service.exception.AppServiceException;
import org.rudi.microservice.projekt.core.bean.Confidentiality;
import org.rudi.microservice.projekt.core.bean.PagedConfidentialityList;
import org.rudi.microservice.projekt.core.bean.criteria.ConfidentialitySearchCriteria;
import org.rudi.microservice.projekt.facade.controller.api.ConfidentialitiesApi;
import org.rudi.microservice.projekt.service.confidentiality.ConfidentialityService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import lombok.RequiredArgsConstructor;
import lombok.val;
import static org.rudi.common.core.security.QuotedRoleCodes.ADMINISTRATOR;
import static org.rudi.common.core.security.QuotedRoleCodes.MODULE_PROJEKT;
import static org.rudi.common.core.security.QuotedRoleCodes.MODULE_PROJEKT_ADMINISTRATOR;

@RestController
@RequiredArgsConstructor
public class ConfidentialityController implements ConfidentialitiesApi {

	private final ConfidentialityService confidentialityService;
	private final UtilPageable utilPageable;

	@Override
	@PreAuthorize("hasAnyRole(" + ADMINISTRATOR + ", " + MODULE_PROJEKT_ADMINISTRATOR + ", " + MODULE_PROJEKT + ")")
	public ResponseEntity<Confidentiality> createConfidentiality(Confidentiality confidentiality) throws AppServiceException {
		val createdConfidentiality = confidentialityService.createConfidentiality(confidentiality);
		val location = ServletUriComponentsBuilder
				.fromCurrentRequest()
				.path("/{uuid}")
				.buildAndExpand(createdConfidentiality.getUuid())
				.toUri();
		return ResponseEntity.created(location).body(createdConfidentiality);
	}

	@Override
	public ResponseEntity<Confidentiality> getConfidentiality(UUID uuid) {
		return ResponseEntity.ok(confidentialityService.getConfidentiality(uuid));
	}

	/**
	 * GET /confidentialities : Liste tous les niveaux de confidentialité
	 * Liste tous les niveaux de confidentialité
	 *
	 * @param isPrivate (optional)
	 * @param active    (optional)
	 * @param limit     Le nombre de résultats à retourner par page (optional)
	 * @param offset    Index de début (positionne le curseur pour parcourir les résultats de la recherche) (optional)
	 * @param order     (optional)
	 * @return OK (status code 200)
	 * or Internal server error (status code 500)
	 */
	@Override
	public ResponseEntity<PagedConfidentialityList> searchConfidentialities(Boolean isPrivate, Boolean active, Integer limit, Integer offset, String order) throws Exception {
		val searchCriteria = ConfidentialitySearchCriteria.builder().active(active).isPrivate(isPrivate).build();

		val pageable = utilPageable.getPageable(offset, limit, order);
		val page = confidentialityService.searchConfidentialities(searchCriteria, pageable);
		return ResponseEntity.ok(new PagedConfidentialityList()
				.total(page.getTotalElements())
				.elements(page.getContent()));
	}

	@Override
	@PreAuthorize("hasAnyRole(" + ADMINISTRATOR + ", " + MODULE_PROJEKT_ADMINISTRATOR + ", " + MODULE_PROJEKT + ")")
	public ResponseEntity<Void> updateConfidentiality(UUID uuid, Confidentiality confidentiality) throws Exception {
		confidentiality.setUuid(uuid);
		confidentialityService.updateConfidentiality(confidentiality);
		return ResponseEntity.noContent().build();
	}

	@Override
	@PreAuthorize("hasAnyRole(" + ADMINISTRATOR + ", " + MODULE_PROJEKT_ADMINISTRATOR + ", " + MODULE_PROJEKT + ")")
	public ResponseEntity<Void> deleteConfidentiality(UUID uuid) {
		confidentialityService.deleteConfidentiality(uuid);
		return ResponseEntity.noContent().build();
	}
}
