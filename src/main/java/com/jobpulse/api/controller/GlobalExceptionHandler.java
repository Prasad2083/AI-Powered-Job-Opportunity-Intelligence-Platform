package com.jobpulse.api.controller;

import com.jobpulse.common.exception.JobPulseException;
import com.jobpulse.common.exception.ResourceNotFoundException;
import com.jobpulse.common.exception.ValidationException;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for all REST endpoints.
 * Provides consistent error response format across the application.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle JobPulse custom exceptions
     */
    @ExceptionHandler(JobPulseException.class)
    public ResponseEntity<ErrorResponse> handleJobPulseException(JobPulseException ex, WebRequest request) {
        ErrorResponse error = new ErrorResponse(
            ex.getErrorCode(),
            ex.getMessage(),
            HttpStatus.valueOf(ex.getHttpStatus()).getReasonPhrase(),
            ex.getHttpStatus(),
            Instant.now()
        );
        return new ResponseEntity<>(error, HttpStatus.valueOf(ex.getHttpStatus()));
    }

    /**
     * Handle validation errors from @Valid annotation
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex, WebRequest request) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
            fieldErrors.put(error.getField(), error.getDefaultMessage())
        );

        ErrorResponse error = new ErrorResponse(
            "VALIDATION_ERROR",
            "Validation failed",
            "Bad Request",
            400,
            Instant.now(),
            fieldErrors
        );
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle resource not found exceptions
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex, WebRequest request) {
        ErrorResponse error = new ErrorResponse(
            ex.getErrorCode(),
            ex.getMessage(),
            "Not Found",
            404,
            Instant.now()
        );
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String,String>> handleMalformedJson(Exception ex) {
        return ResponseEntity.badRequest().body(Map.of("message","Malformed JSON or unsupported field value"));
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<Map<String,String>> handleMissingRoute(Exception ex) {
        return ResponseEntity.status(404).body(Map.of("message","Resource not found"));
    }

    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleStatus(org.springframework.web.server.ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of("message", ex.getReason() == null ? "Request failed" : ex.getReason()));
    }

    @ExceptionHandler({IllegalArgumentException.class, org.springframework.web.multipart.MaxUploadSizeExceededException.class})
    public ResponseEntity<Map<String, String>> handleInput(Exception ex) {
        return ResponseEntity.badRequest().body(Map.of("message", "Invalid input: " + ex.getMessage()));
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleConflict(Exception ex) {
        return ResponseEntity.status(409).body(Map.of("message", "A record with these unique values already exists"));
    }

    /**
     * Handle all other exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, WebRequest request) {
        ErrorResponse error = new ErrorResponse(
            "INTERNAL_ERROR",
            "An unexpected error occurred",
            "Internal Server Error",
            500,
            Instant.now()
        );
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Error response DTO
     */
    @Data
    @AllArgsConstructor
    public static class ErrorResponse {
        private String errorCode;
        private String message;
        private String status;
        private int statusCode;
        private Instant timestamp;
        private Map<String, String> fieldErrors;

        public ErrorResponse(String errorCode, String message, String status, int statusCode, Instant timestamp) {
            this.errorCode = errorCode;
            this.message = message;
            this.status = status;
            this.statusCode = statusCode;
            this.timestamp = timestamp;
            this.fieldErrors = new HashMap<>();
        }
    }
}

