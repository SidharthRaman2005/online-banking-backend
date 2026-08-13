package com.bank.online_banking_system.exception;

import org.springframework.http.HttpStatus;

public class AccountNotFoundException extends ApiException {
    public AccountNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND");
    }
}
