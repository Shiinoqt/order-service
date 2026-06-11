package com.its.gestioneordinirestclient.exception;

import com.its.gestioneordinirestclient.exception.AppException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Handles all your custom exceptions (NotFoundException,
    // UsernameAlreadyExistsException, AccessDeniedException)
    // since they all extend AppException
    @ExceptionHandler(AppException.class)
    public ResponseEntity<String> handleAppException(AppException e) {
        return ResponseEntity.status(e.getStatus()).body(e.getMessage());
    }

    // Handles @Valid failures on request bodies
    // Returns a map of field -> error message
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : e.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        return ResponseEntity.status(400).body(errors);
    }

    // Catches anything unexpected so the app never returns a raw 500 stack trace
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGeneric(Exception e) {
        return ResponseEntity.status(500).body("An unexpected error occurred");
    }

    // Catches missing headers
    @ExceptionHandler(org.springframework.web.bind.MissingRequestHeaderException.class)
    public ResponseEntity<String> handleMissingHeader(org.springframework.web.bind.MissingRequestHeaderException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Missing required header: " + e.getHeaderName());
    }
}