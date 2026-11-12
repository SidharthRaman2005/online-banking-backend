package com.bank.online_banking_system.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GoogleVerificationRequest {

    @NotBlank(message = "Google credential is required")
    private String credential;
}
