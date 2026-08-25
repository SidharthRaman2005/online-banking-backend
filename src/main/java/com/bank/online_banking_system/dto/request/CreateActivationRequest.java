package com.bank.online_banking_system.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class CreateActivationRequest {
    @NotBlank
    private String message;

    private String email;

    private String password;
}
