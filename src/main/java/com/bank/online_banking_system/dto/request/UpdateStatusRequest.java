package com.bank.online_banking_system.dto.request;

import com.bank.online_banking_system.enums.AccountStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateStatusRequest {

    @NotNull(message = "Status is required")
    private AccountStatus status;
}
