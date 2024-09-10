package org.rudi.microservice.kalim.service.integration.impl.validator.extractor;

import java.util.UUID;

import org.rudi.facet.dataverse.api.exceptions.DataverseAPIException;
import org.rudi.facet.kaccess.service.dataset.DatasetService;
import org.rudi.microservice.kalim.service.integration.impl.validator.metadata.AbstractUniqueMetadataIdValidator;
import org.springframework.stereotype.Component;

@Component
public class UniqueGlobalIdValidator extends AbstractUniqueMetadataIdValidator<UUID> {

	private final DatasetService datasetService;

	public UniqueGlobalIdValidator(GlobalIdExtractor fieldExtractor, DatasetService datasetService) {
		super(fieldExtractor);
		this.datasetService = datasetService;
	}

	@Override
	protected boolean datasetAlreadyExistsWithFieldValue(UUID globalId) throws DataverseAPIException {
		return datasetService.datasetExists(globalId);
	}
}
