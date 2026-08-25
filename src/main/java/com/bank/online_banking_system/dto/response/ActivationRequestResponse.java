package com.bank.online_banking_system.dto.response;

import com.bank.online_banking_system.entity.ActivationRequest;
import com.bank.online_banking_system.enums.ActivationRequestStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class ActivationRequestResponse {
    private Long id;
    private Long userId;
    private String username;
    private String message;
    private ActivationRequestStatus status;
    private Instant createdAt;

    public static ActivationRequestResponse from(ActivationRequest r) {
        return ActivationRequestResponse.builder()
                .id(r.getId())
                .userId(r.getUser().getId())
                .username(r.getUser().getUsername())
                .message(r.getMessage())
                .status(r.getStatus())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
