package com.jobpulse.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when external service fails (AI service, job APIs, etc.)
 */
public class ExternalServiceException extends JobPulseException {
    public ExternalServiceException(String message) {
        super(message, "EXTERNAL_SERVICE_ERROR", HttpStatus.SERVICE_UNAVAILABLE.value());
    }

    public ExternalServiceException(String serviceName, String message) {
        super(String.format("External service '%s' error: %s", serviceName, message),
                "EXTERNAL_SERVICE_ERROR",
                HttpStatus.SERVICE_UNAVAILABLE.value());
    }

    public ExternalServiceException(String serviceName, Throwable cause) {
        super(String.format("External service '%s' failed", serviceName),
                cause,
                "EXTERNAL_SERVICE_ERROR",
                HttpStatus.SERVICE_UNAVAILABLE.value());
    }
}

