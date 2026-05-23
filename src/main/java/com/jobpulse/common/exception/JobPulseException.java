package com.jobpulse.common.exception;

/**
 * Base exception class for JobPulse application.
 * All custom exceptions should extend this class.
 */
public class JobPulseException extends RuntimeException {
    private final String errorCode;
    private final int httpStatus;

    public JobPulseException(String message, String errorCode, int httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public JobPulseException(String message, Throwable cause, String errorCode, int httpStatus) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}

