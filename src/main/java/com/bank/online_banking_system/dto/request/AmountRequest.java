package com.bank.online_banking_system.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** Shared body for deposit and withdraw. */
@Getter
@Setter
public class AmountRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    @Digits(integer = 17, fraction = 2, message = "Amount may have at most 2 decimal places")
    private BigDecimal amount;

    @Size(max = 200, message = "Description is too long")
    private String description;
}
