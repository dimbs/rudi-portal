package org.rudi.microservice.kalim.service.integration.impl.validator.extractor;

import org.rudi.facet.dataverse.api.exceptions.DataverseAPIException;
import org.rudi.facet.kaccess.bean.DatasetSearchCriteria;
import org.rudi.facet.kaccess.service.dataset.DatasetService;
import org.rudi.microservice.kalim.service.integration.impl.validator.metadata.AbstractUniqueMetadataIdValidator;
import org.springframework.stereotype.Component;

@Component
public class UniqueLocalIdValidator extends AbstractUniqueMetadataIdValidator<String> {

	private final DatasetService datasetService;

	public UniqueLocalIdValidator(LocalIdExtractor fieldExtractor, DatasetService datasetService) {
		super(fieldExtractor);
		this.datasetService = datasetService;
	}

	@Override
	protected boolean datasetAlreadyExistsWithFieldValue(String localId) throws DataverseAPIException {
		final DatasetSearchCriteria datasetSearchCriteria = new DatasetSearchCriteria().localId(localId);
		return datasetService.datasetExists(datasetSearchCriteria);
	}
}
