package com.jobpulse.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when invalid input data is provided.
 */
public class ValidationException extends JobPulseException {
    public ValidationException(String message) {
        super(message, "VALIDATION_ERROR", HttpStatus.BAD_REQUEST.value());
    }

    public ValidationException(String field, String message) {
        super(String.format("Validation error on field '%s': %s", field, message),
                "VALIDATION_ERROR",
                HttpStatus.BAD_REQUEST.value());
    }
}

