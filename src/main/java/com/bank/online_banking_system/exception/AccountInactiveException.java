package com.bank.online_banking_system.exception;

import org.springframework.http.HttpStatus;

public class AccountInactiveException extends ApiException {
    public AccountInactiveException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "ACCOUNT_INACTIVE");
    }
}
