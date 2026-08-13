package com.bank.online_banking_system.exception;

import org.springframework.http.HttpStatus;

public class SelfTransferException extends ApiException {
    public SelfTransferException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "SELF_TRANSFER_NOT_ALLOWED");
    }
}
