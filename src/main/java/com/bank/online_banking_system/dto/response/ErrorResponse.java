package com.bank.online_banking_system.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    @Builder.Default
    private final boolean success = false;

    private final String message;
    private final String errorCode;

    /** Present only for field-level validation failures. */
    private final Map<String, String> errors;

    @Builder.Default
    private final Instant timestamp = Instant.now();

    private final String path;
}
