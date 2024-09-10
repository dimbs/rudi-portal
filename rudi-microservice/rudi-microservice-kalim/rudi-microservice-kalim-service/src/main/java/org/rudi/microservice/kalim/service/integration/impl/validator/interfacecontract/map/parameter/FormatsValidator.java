package org.rudi.microservice.kalim.service.integration.impl.validator.interfacecontract.map.parameter;

import static org.rudi.microservice.kalim.service.integration.impl.validator.interfacecontract.map.parameter.MapConnectorParametersConstants.ALPHANUMERIC_REGEX;
import static org.rudi.microservice.kalim.service.integration.impl.validator.interfacecontract.map.parameter.MapConnectorParametersConstants.APP_JSON_FORMAT;
import static org.rudi.microservice.kalim.service.integration.impl.validator.interfacecontract.map.parameter.MapConnectorParametersConstants.FORMATS_PARAMETER;
import static org.rudi.microservice.kalim.service.integration.impl.validator.interfacecontract.map.parameter.MapConnectorParametersConstants.GML2_FORMAT;
import static org.rudi.microservice.kalim.service.integration.impl.validator.interfacecontract.map.parameter.MapConnectorParametersConstants.GML3_FORMAT;

import java.util.HashSet;
import java.util.Set;

import org.rudi.facet.dataset.bean.InterfaceContract;
import org.rudi.facet.kaccess.bean.ConnectorConnectorParametersInner;
import org.rudi.microservice.kalim.storage.entity.integration.IntegrationRequestErrorEntity;
import org.springframework.stereotype.Component;

@Component
public class FormatsValidator extends AbstractMapConnectorParametersValidator {

	@Override
	public Set<IntegrationRequestErrorEntity> validate(ConnectorConnectorParametersInner connectorConnectorParametersInner, String interfaceContract) {
		Set<IntegrationRequestErrorEntity> integrationRequestsErrors = new HashSet<>();
		final String value = connectorConnectorParametersInner.getValue();

		InterfaceContract interfaceContractEnum = InterfaceContract.from(interfaceContract,interfaceContract, (contract -> interfaceContract.equalsIgnoreCase(contract.getCode())), true);


		if (interfaceContractEnum == InterfaceContract.WFS) { // WFS
			if (!(value.equals(APP_JSON_FORMAT) || value.equals(GML2_FORMAT) || value.equals(GML3_FORMAT))) {
				integrationRequestsErrors.add(
						buildError302(FORMATS_PARAMETER, value, String.join(",", APP_JSON_FORMAT, GML2_FORMAT, GML3_FORMAT))
				);
			}
		} else { // WMS, WMTS
			String[] splitValue = value.split("/");
			if (splitValue.length != 2) { // Un format MIME à 2 parties séparées par un slash (type/sous-type)
				integrationRequestsErrors.add(buildError307(FORMATS_PARAMETER, value));
			} else {
				boolean matchType = splitValue[0].matches(ALPHANUMERIC_REGEX);
				boolean matchSubType = splitValue[1].matches(ALPHANUMERIC_REGEX);
				if (!matchType || !matchSubType) { // Si le type ou le sous-type ne matche pas la REGEX
					integrationRequestsErrors.add(buildError307(FORMATS_PARAMETER, value));
				}
			}
		}
		return integrationRequestsErrors;
	}

	@Override
	public boolean accept(ConnectorConnectorParametersInner connectorConnectorParametersInner) {
		return connectorConnectorParametersInner.getKey().equals(FORMATS_PARAMETER);
	}
}
