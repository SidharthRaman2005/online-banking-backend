package com.bank.online_banking_system.exception;

import org.springframework.http.HttpStatus;

public class AccountDeactivatedException extends ApiException {
    public AccountDeactivatedException(String message) {
        super(message, HttpStatus.FORBIDDEN, "ACCOUNT_DEACTIVATED");
    }
}
