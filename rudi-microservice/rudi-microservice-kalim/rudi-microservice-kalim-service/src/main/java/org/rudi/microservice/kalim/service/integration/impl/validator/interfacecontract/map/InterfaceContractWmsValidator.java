package org.rudi.microservice.kalim.service.integration.impl.validator.interfacecontract.map;

import static org.rudi.microservice.kalim.service.integration.impl.validator.interfacecontract.map.parameter.MapConnectorParametersConstants.WMS_MANDATORY_PARAMS;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.rudi.facet.dataset.bean.InterfaceContract;
import org.rudi.microservice.kalim.service.integration.impl.validator.interfacecontract.InterfaceContractConnectorValidator;
import org.rudi.microservice.kalim.storage.entity.integration.IntegrationRequestErrorEntity;
import org.springframework.stereotype.Component;

@Component
public class InterfaceContractWmsValidator implements InterfaceContractConnectorValidator {

	@Override
	public InterfaceContract getInterfaceContract() {
		return InterfaceContract.WMS;
	}

	@Override
	public void validate(List<String> connectorParametersKeys, Set<IntegrationRequestErrorEntity> errorEntities) {
		// Demande Sonar : transformation en HashSet pour des raisons de performance
		Set<String> connectorParametersKeySet = new HashSet<>(connectorParametersKeys);

		if(!connectorParametersKeySet.containsAll(WMS_MANDATORY_PARAMS)){
			errorEntities.add(buildError405());
		}
	}
}
