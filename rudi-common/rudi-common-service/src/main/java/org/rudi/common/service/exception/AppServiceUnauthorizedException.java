package org.rudi.common.service.exception;

public class AppServiceUnauthorizedException extends AppServiceException {
	public AppServiceUnauthorizedException(String message) {
		super(message, AppServiceExceptionsStatus.UNAUTHORIZE);
	}
}
