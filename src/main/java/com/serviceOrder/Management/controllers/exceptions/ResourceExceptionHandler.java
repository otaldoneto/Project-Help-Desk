package com.serviceOrder.Management.controllers.exceptions;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.dao.OptimisticLockingFailureException;

import java.time.Instant;
import java.util.stream.Collectors;

@ControllerAdvice
public class ResourceExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<StandardError> resourceNotFound(ResourceNotFoundException e, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "Resource not found", e.getMessage(), request);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<StandardError> businessRule(BusinessRuleException e, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "Business rule violation", e.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<StandardError> validation(MethodArgumentNotValidException e, HttpServletRequest request) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .sorted()
                .collect(Collectors.joining("; "));
        if (message.isEmpty()) {
            message = "Validation error";
        }
        return build(HttpStatus.BAD_REQUEST, "Validation error", message, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<StandardError> unreadable(HttpMessageNotReadableException e, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Malformed request",
                "Request body is missing, malformed or contains an invalid value", request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<StandardError> typeMismatch(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Invalid parameter",
                "Invalid value '" + e.getValue() + "' for parameter '" + e.getName() + "'", request);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<StandardError> optimisticLock(OptimisticLockingFailureException e,
                                                        HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "Concurrent modification",
                "The resource was modified by another request. Reload it and try again", request);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<StandardError> invalidCredentials(InvalidCredentialsException e,
                                                            HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "Unauthorized", e.getMessage(), request);
    }

    private ResponseEntity<StandardError> build(HttpStatus status, String error, String message,
                                                HttpServletRequest request) {
        StandardError err = new StandardError(Instant.now(), status.value(), error, message, request.getRequestURI());
        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<StandardError> dataIntegrity(DataIntegrityViolationException e, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "Data conflict",
                "The request conflicts with existing data, such as a duplicated unique value", request);
    }

    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<StandardError> invalidSortProperty(PropertyReferenceException e, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Invalid parameter",
                "Invalid sort property: '" + e.getPropertyName() + "'", request);
    }
}