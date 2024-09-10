/**
 * RUDI Portail
 */
package org.rudi.microservice.apigateway.facade.config.gateway.interfacecontract.parameter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import org.rudi.microservice.apigateway.facade.config.gateway.interfacecontract.SwaggerFormat;
import org.rudi.microservice.apigateway.facade.config.gateway.interfacecontract.SwaggerType;

import io.swagger.models.parameters.SerializableParameter;

/**
 * @author FNI18300
 *
 */
public class StringParameterDataValidator extends AbstractParameterDataValidator {

	public StringParameterDataValidator() {
		super(SwaggerType.STRING);
	}

	@Override
	protected boolean internalValidate(SerializableParameter parameter, String value) {
		boolean result = false;
		SwaggerFormat format = SwaggerFormat.lookupType(parameter.getFormat());
		switch (format) {
		case UUID:
			try {
				UUID.fromString(value);
				result = true;
			} catch (Exception e) {
				// rien
			}
			break;
		case DATE_TIME:
			try {
				LocalDateTime.parse(value);
				result = true;
			} catch (Exception e) {
				// rien
			}
			break;
		case DATE:
			try {
				LocalDate.parse(value);
				result = true;
			} catch (Exception e) {
				// rien
			}
			break;
		default:
			result = true;
			break;
		}
		return result;
	}

}
