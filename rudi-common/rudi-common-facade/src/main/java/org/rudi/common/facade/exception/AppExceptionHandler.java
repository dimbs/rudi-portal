package org.rudi.common.facade.exception;

import javax.validation.ValidationException;

import org.rudi.common.service.exception.AppServiceException;
import org.rudi.common.service.exception.AppServiceExceptionsStatus;
import org.rudi.common.service.exception.AppServiceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import com.fasterxml.jackson.core.JsonParseException;

import lombok.Data;
import lombok.val;

@ControllerAdvice
public class AppExceptionHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(AppExceptionHandler.class);

	// TODO où déplacer cette classe ?
	@Data
	private static class ApiError {
		private final String code;
		private final String label;
	}

	@ExceptionHandler(org.rudi.common.service.exception.AppServiceException.class)
	protected ResponseEntity<Object> handleExceptionService(final AppServiceException ex) {

		LOGGER.error(ex.getMessage(), ex);

		final AppServiceExceptionsStatus appExceptionStatusCode = ex.getAppExceptionStatusCode();
		if (appExceptionStatusCode != null) {

			val httpStatus = appExceptionStatusCode.getHttpStatus();
			if (httpStatus != null) {
				if (httpStatus.is4xxClientError()) {
					return ResponseEntity.status(httpStatus).body(buildBody(ex, httpStatus));
				} else {
					return ResponseEntity.status(httpStatus).build();
				}
			}

			final int customHttpStatusCode = appExceptionStatusCode.getCustomHttpStatusCode();
			if (customHttpStatusCode > 0) {
				return ResponseEntity.status(customHttpStatusCode).body(buildBody(ex, customHttpStatusCode));
			}
		}

		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
	}

	@ExceptionHandler({ AccessDeniedException.class, AuthenticationCredentialsNotFoundException.class,
			LockedException.class, BadCredentialsException.class })
	protected ResponseEntity<Object> handleAccessDeniedException(final Exception ex, final WebRequest request) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
	}

	@ExceptionHandler({ AppServiceNotFoundException.class })
	protected ResponseEntity<Object> handleNotFoundException(final Exception ex, final WebRequest request) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
	}

	@ExceptionHandler({ IllegalAccessException.class })
	protected ResponseEntity<Object> handleIllegalAccessException(final Exception ex, final WebRequest request) {
		LOGGER.error("Ressource not authorized");
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
	}

	@ExceptionHandler({ ValidationException.class, JsonParseException.class, HttpMessageNotReadableException.class,
			MethodArgumentNotValidException.class, IllegalArgumentException.class })
	protected ResponseEntity<Object> handleValidationException(final Exception ex, final WebRequest request) {
		LOGGER.error("Ressource not valid");
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(buildBody(ex, HttpStatus.BAD_REQUEST));
	}

	@ExceptionHandler({ DataIntegrityViolationException.class })
	protected ResponseEntity<Object> handleConflictException(final Exception ex, final WebRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT).build();
	}

	@ExceptionHandler(Exception.class)
	protected ResponseEntity<Object> handleException(final Exception ex, final WebRequest request) {
		LOGGER.error("Unknown error", ex);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
	}

	private ApiError buildBody(Exception ex, HttpStatus status) {
		return new ApiError(status.toString(), ex.getLocalizedMessage());
	}

	private ApiError buildBody(Exception ex, int customHttpStatusCode) {
		return new ApiError("HTTP " + customHttpStatusCode, ex.getLocalizedMessage());
	}
}
