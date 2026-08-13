package com.bank.online_banking_system.exception;

import org.springframework.http.HttpStatus;

public class InsufficientBalanceException extends ApiException {
    public InsufficientBalanceException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "INSUFFICIENT_BALANCE");
    }
}
