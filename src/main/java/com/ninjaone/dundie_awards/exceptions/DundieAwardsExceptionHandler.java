package com.ninjaone.dundie_awards.exceptions;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * Global exception handler for Dundie Awards application
 */
@Slf4j
@ControllerAdvice
public class DundieAwardsExceptionHandler {

    /**
     * Handle LookupException - usually indicates resource not found
     * 
     * @param ex the exception
     * @param request the HTTP request
     * @return a ResponseEntity containing the error details
     */
    @ExceptionHandler(LookupException.class)
    public ResponseEntity<Map<String, Object>> handleLookupException(LookupException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.NOT_FOUND;
        Map<String, Object> errorResponse = createErrorResponse(ex.getMessage(), status, request);
        return ResponseEntity.status(status).body(errorResponse);
    }

    /**
     * Handle InvalidArgumentException - usually indicates bad input
     * 
     * @param ex the exception
     * @param request the HTTP request
     * @return a ResponseEntity containing the error details
     */
    @ExceptionHandler(InvalidArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidArgumentException(InvalidArgumentException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        Map<String, Object> errorResponse = createErrorResponse(ex.getMessage(), status, request);
        return ResponseEntity.status(status).body(errorResponse);
    }

    /**
     * Handle HttpMessageNotReadableException - usually indicates a field was null when it shouldn't be
     * 
     * @param ex the exception
     * @param request the HTTP request
     * @return a ResponseEntity containing the error details
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        Map<String, Object> errorResponse = createErrorResponse("Error reading the JSON body. Please make sure all required fields are provided and correctly formatted: " + ex.getMessage(), status, request);
        return ResponseEntity.status(status).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            FieldError field = ((FieldError) error);
            errors.put(field.getField(), field.getDefaultMessage());
        });
        Map<String, Object> errorResponse = createErrorResponse("Error validating objects", status, errors, request);
        return ResponseEntity.status(status).body(errorResponse);
    }

    /**
     * Handle DataAccessException - usually indicates DB error
     * 
     * @param ex the exception
     * @param request the HTTP request
     * @return a ResponseEntity containing the error details
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, Object>> handleDataAccessException(DataAccessException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        Map<String, Object> errorResponse = createErrorResponse("A database error occurred. Cannot process request", status, request);
        return ResponseEntity.status(status).body(errorResponse);
    }

    /**
     * Handle CannotCreateTransactionException - usually indicates DB is down
     * 
     * @param ex the exception
     * @param request the HTTP request
     * @return a ResponseEntity containing the error details
     */
    @ExceptionHandler(CannotCreateTransactionException.class)
    public ResponseEntity<Map<String, Object>> handleTransactionException(CannotCreateTransactionException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.SERVICE_UNAVAILABLE;
        Map<String, Object> errorResponse = createErrorResponse("Cannot connect to the database. Please try again later.", status, request);
        return ResponseEntity.status(status).body(errorResponse);
    }

    /**
     * Handle NoResourceFoundException - usually indicates resource not found
     * 
     * @param ex the exception
     * @param request the HTTP request
     * @return a ResponseEntity containing the error details
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResourceFoundException(NoResourceFoundException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.NOT_FOUND;
        Map<String, Object> errorResponse = createErrorResponse(ex.getMessage(), status, request);
        return ResponseEntity.status(status).body(errorResponse);
    }

    /**
     * Handle IllegalStateException - usually indicates missing parameters
     * 
     * @param ex the exception
     * @param request the HTTP request
     * @return a ResponseEntity containing the error details
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalStateException(IllegalStateException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        Map<String, Object> errorResponse = createErrorResponse("Not all expected parameters were provided. Please make sure they are all included", status, request);
        return ResponseEntity.status(status).body(errorResponse);
    }

    /**
     * Handle generic Exception and log it so that it can be investigated
     * 
     * @param ex the exception
     * @param request the HTTP request
     * @return a ResponseEntity containing the error details
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        Map<String, Object> errorResponse = createErrorResponse("An unexpected error occurred: " + ex.getMessage(), status, request);
        return ResponseEntity.status(status).body(errorResponse);
    }

    private Map<String, Object> createErrorResponse(String message, HttpStatus status, HttpServletRequest request) {
        return createErrorResponse(message, status, null, request);
    }

    /**
     * Create a standardized error response map
     * 
     * @param message the error message
     * @param status the HTTP status
     * @param request the HTTP request
     * @return a map containing error details
     */ 
    private Map<String, Object> createErrorResponse(String message, HttpStatus status, Map<String, String> errors, HttpServletRequest request) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", status.getReasonPhrase());
        errorResponse.put("status", String.valueOf(status.value()));
        errorResponse.put("message", message);
        errorResponse.put("timestamp", DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(java.time.LocalDateTime.now()));
        errorResponse.put("path", request.getRequestURI());
        if (errors != null && !errors.isEmpty()) {
            errorResponse.put("fields", errors);
        }
        return errorResponse;
    }
}
