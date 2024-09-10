package org.rudi.microservice.kalim.service.integration.impl.validator.map;


import java.io.IOException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.rudi.common.core.json.JsonResourceReader;
import org.rudi.microservice.kalim.service.KalimSpringBootTest;
import org.rudi.microservice.kalim.service.integration.impl.validator.interfacecontract.map.parameter.FormatsValidator;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.RequiredArgsConstructor;
import lombok.val;
import static org.assertj.core.api.Assertions.assertThat;
import static org.rudi.microservice.kalim.service.integration.impl.validator.interfacecontract.map.parameter.MapConnectorParametersConstants.FORMATS_PARAMETER;

@KalimSpringBootTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class FormatsValidatorUT extends AbstractValidatorUT {
	private final FormatsValidator formatsValidator;
	private final JsonResourceReader jsonResourceReader;

	@Test
	@DisplayName("Format bien formé")
	void validate_format_ok() throws IOException {
		val connector = createConnectorFromJson(jsonResourceReader, JSON_CONNECTOR_OK);
		val formatDto = getConnectorParameter(connector.getConnectorParameters(), FORMATS_PARAMETER);
		val errors = formatsValidator.validate(formatDto, connector.getInterfaceContract());

		assertThat(errors.size())
				.as("Il n'y a pas d'erreurs sur le champ format")
				.isEqualTo(0);
	}

	@Test
	@DisplayName("Format mal formé")
	void validate_format_nok() throws IOException {
		val connector = createConnectorFromJson(jsonResourceReader, JSON_CONNECTOR_NOK);
		val formatDto = getConnectorParameter(connector.getConnectorParameters(), FORMATS_PARAMETER);
		val errors = formatsValidator.validate(formatDto, connector.getInterfaceContract());

		assertThat(errors.size())
				.as("Il y a des erreurs sur le champ format")
				.isGreaterThan(0);
	}
}
