package com.bank.online_banking_system.exception;

import org.springframework.http.HttpStatus;

public class InvalidTransactionException extends ApiException {
    public InvalidTransactionException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "INVALID_TRANSACTION");
    }
}
