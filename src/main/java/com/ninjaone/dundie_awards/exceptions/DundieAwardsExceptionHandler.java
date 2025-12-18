package com.ninjaone.dundie_awards.exceptions;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ControllerAdvice
public class DundieAwardsExceptionHandler {

    @ExceptionHandler(LookupException.class)
    public ResponseEntity<Map<String, String>> handleLookupException(AbstractDundieRuntimeException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.NOT_FOUND;
        Map<String, String> errorResponse = createErrorResponse(ex.getMessage(), status, request);
        return ResponseEntity.status(status).body(errorResponse);
    }

    @ExceptionHandler(InvalidArgumentException.class)
    public ResponseEntity<Map<String, String>> handleInvalidArgumentException(AbstractDundieRuntimeException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        Map<String, String> errorResponse = createErrorResponse(ex.getMessage(), status, request);
        return ResponseEntity.status(status).body(errorResponse);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, String>> handleDataAccessException(DataAccessException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        Map<String, String> errorResponse = createErrorResponse("A database error occurred. Cannot process request", status, request);
        return ResponseEntity.status(status).body(errorResponse);
    }

    @ExceptionHandler(CannotCreateTransactionException.class)
    public ResponseEntity<Map<String, String>> handleTransactionException(CannotCreateTransactionException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.SERVICE_UNAVAILABLE;
        Map<String, String> errorResponse = createErrorResponse("Cannot connect to the database. Please try again later.", status, request);
        return ResponseEntity.status(status).body(errorResponse);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, String>> handleNoResourceFoundException(NoResourceFoundException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.NOT_FOUND;
        Map<String, String> errorResponse = createErrorResponse(ex.getMessage(), status, request);
        return ResponseEntity.status(status).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        Map<String, String> errorResponse = createErrorResponse("An unexpected error occurred: " + ex.getMessage(), status, request);
        return ResponseEntity.status(status).body(errorResponse);
    }

    private Map<String, String> createErrorResponse(String message, HttpStatus status, HttpServletRequest request) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", status.getReasonPhrase());
        errorResponse.put("status", String.valueOf(status.value()));
        errorResponse.put("message", message);
        errorResponse.put("timestamp", DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(java.time.LocalDateTime.now()));
        errorResponse.put("path", request.getRequestURI());
        return errorResponse;
    }
}
