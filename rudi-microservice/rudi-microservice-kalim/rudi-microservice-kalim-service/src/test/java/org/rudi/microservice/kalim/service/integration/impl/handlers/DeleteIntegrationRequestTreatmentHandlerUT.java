package org.rudi.microservice.kalim.service.integration.impl.handlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rudi.common.core.json.JsonResourceReader;
import org.rudi.facet.apigateway.exceptions.DeleteApiException;
import org.rudi.facet.dataverse.api.exceptions.DatasetNotFoundException;
import org.rudi.facet.dataverse.api.exceptions.DataverseAPIException;
import org.rudi.facet.kaccess.bean.Metadata;
import org.rudi.facet.kaccess.helper.dataset.metadatadetails.MetadataDetailsHelper;
import org.rudi.facet.kaccess.service.dataset.DatasetService;
import org.rudi.microservice.kalim.core.bean.IntegrationStatus;
import org.rudi.microservice.kalim.core.bean.Method;
import org.rudi.microservice.kalim.core.bean.ProgressStatus;
import org.rudi.microservice.kalim.service.helper.ApiManagerHelper;
import org.rudi.microservice.kalim.service.helper.Error500Builder;
import org.rudi.microservice.kalim.service.integration.impl.validator.authenticated.DatasetCreatorIsAuthenticatedValidator;
import org.rudi.microservice.kalim.storage.entity.integration.IntegrationRequestEntity;

import com.fasterxml.jackson.core.JsonProcessingException;

@ExtendWith(MockitoExtension.class)
class DeleteIntegrationRequestTreatmentHandlerUT {

	private final Error500Builder error500Builder = new Error500Builder();
	private final JsonResourceReader jsonResourceReader = new JsonResourceReader();
	private AbstractIntegrationRequestTreatmentHandler handler;
	@Mock
	private DatasetService datasetService;
	@Mock
	private ApiManagerHelper apigatewayManagerHelper;
	@Mock
	private DatasetCreatorIsAuthenticatedValidator datasetCreatorIsAuthenticatedValidator;
	@Mock
	private MetadataDetailsHelper metadataDetailsHelper;

	@BeforeEach
	void setUp() {
		handler = new DeleteIntegrationRequestTreatmentHandler(datasetService, apigatewayManagerHelper, error500Builder,
				datasetCreatorIsAuthenticatedValidator, metadataDetailsHelper);
	}

	@Test
	@DisplayName("non existing metadata ⇒ error")
	void createIntegrationRequestDeleteNonExistingMetadata()
			throws DataverseAPIException, JsonProcessingException, DeleteApiException {

		final Metadata metadataToDelete = buildMetadataToDelete();
		final String metadataJson = jsonResourceReader.getObjectMapper().writeValueAsString(metadataToDelete);
		final IntegrationRequestEntity integrationRequest = IntegrationRequestEntity.builder().method(Method.DELETE)
				.uuid(UUID.randomUUID()).globalId(metadataToDelete.getGlobalId()).progressStatus(ProgressStatus.CREATED)
				.errors(new HashSet<>()).file(metadataJson).build();

		when(datasetService.getDataset(metadataToDelete.getGlobalId()))
				.thenThrow(DatasetNotFoundException.fromGlobalId(metadataToDelete.getGlobalId()));

		handler.handle(integrationRequest);

		assertThat(integrationRequest.getIntegrationStatus()).isEqualTo(IntegrationStatus.OK);

		// No more interactions with DataSet
		verifyNoMoreInteractions(datasetService);

		// API is deleted
		verify(apigatewayManagerHelper).deleteApis(integrationRequest);
	}

	private Metadata buildMetadataToDelete() {
		return new Metadata().globalId(UUID.randomUUID());
	}

	/**
	 * RUDI-541 : On doit pouvoir supprimer un JDD sans devoir envoyer tout son contenu JSON
	 * 
	 * @throws DeleteApiException
	 */
	@Test
	@DisplayName("existing metadata ⇒ dataset archived and API deleted")
	void createIntegrationRequestDeleteExistingMetadata()
			throws DataverseAPIException, JsonProcessingException, DeleteApiException {

		final Metadata metadataToDelete = new Metadata().globalId(UUID.randomUUID());
		final String metadataJson = jsonResourceReader.getObjectMapper().writeValueAsString(metadataToDelete);
		final IntegrationRequestEntity integrationRequest = IntegrationRequestEntity.builder().method(Method.DELETE)
				.uuid(UUID.randomUUID()).globalId(metadataToDelete.getGlobalId()).progressStatus(ProgressStatus.CREATED)
				.file(metadataJson).errors(new HashSet<>()).build();

		when(datasetService.getDataset(metadataToDelete.getGlobalId())).thenReturn(metadataToDelete);

		handler.handle(integrationRequest);

		assertThat(integrationRequest.getIntegrationStatus()).isEqualTo(IntegrationStatus.OK);

		// DataSet is archived
		verify(datasetService).archiveDataset(metadataToDelete.getDataverseDoi());

		// API is deleted
		verify(apigatewayManagerHelper).deleteApis(integrationRequest);
	}

}
