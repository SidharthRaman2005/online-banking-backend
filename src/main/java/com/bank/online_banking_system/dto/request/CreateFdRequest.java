package com.bank.online_banking_system.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateFdRequest {
    @NotNull
    @DecimalMin(value = "100.00", message = "Minimum deposit is 100")
    private BigDecimal amount;

    @Min(value = 1, message = "Term must be at least 1 month")
    private int termMonths;
}
