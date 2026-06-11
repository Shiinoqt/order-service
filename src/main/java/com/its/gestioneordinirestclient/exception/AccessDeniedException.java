package com.its.gestioneordinirestclient.exception;

import org.springframework.http.HttpStatus;

public class AccessDeniedException extends AppException {
    public AccessDeniedException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}
