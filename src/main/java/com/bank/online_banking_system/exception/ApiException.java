package com.bank.online_banking_system.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base for every business exception. Carries the HTTP status and the stable {@code errorCode}
 * the frontend switches on, so {@link GlobalExceptionHandler} needs one handler for all of them.
 */
@Getter
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    protected ApiException(String message, HttpStatus status, String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }
}
