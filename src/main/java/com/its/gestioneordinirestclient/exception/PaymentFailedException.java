package com.its.gestioneordinirestclient.exception;

import org.springframework.http.HttpStatus;

public class PaymentFailedException extends AppException {
    public PaymentFailedException(String message) {
        super(message, HttpStatus.PAYMENT_REQUIRED);
    }
}
