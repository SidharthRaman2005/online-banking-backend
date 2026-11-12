package com.bank.online_banking_system.exception;

import org.springframework.http.HttpStatus;

public class GoogleVerificationException extends ApiException {

    public GoogleVerificationException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "GOOGLE_VERIFICATION_FAILED");
    }
}
