package com.bank.online_banking_system.exception;

import org.springframework.http.HttpStatus;

public class TransactionFailedException extends ApiException {
    public TransactionFailedException(String message) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR, "TRANSACTION_FAILED");
    }
}
