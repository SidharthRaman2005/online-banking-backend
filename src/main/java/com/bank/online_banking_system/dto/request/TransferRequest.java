package com.bank.online_banking_system.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Exactly one of {@code receiverAccountNumber} or {@code receiverUpiId} must be supplied.
 * Both routes go through the same transfer code path; only the lookup differs.
 */
@Getter
@Setter
public class TransferRequest {

    @Pattern(regexp = "^[1-9]\\d{9}$", message = "Account number must be 10 digits")
    private String receiverAccountNumber;

    @Pattern(regexp = "^[A-Za-z0-9._]{3,20}@obs$", message = "UPI ID must look like username@obs")
    private String receiverUpiId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    @Digits(integer = 17, fraction = 2, message = "Amount may have at most 2 decimal places")
    private BigDecimal amount;

    @Size(max = 200, message = "Description is too long")
    private String description;

    @JsonIgnore
    @AssertTrue(message = "Provide either a receiver account number or a receiver UPI ID, not both")
    public boolean isExactlyOneReceiverSupplied() {
        boolean byAccount = receiverAccountNumber != null && !receiverAccountNumber.isBlank();
        boolean byUpi = receiverUpiId != null && !receiverUpiId.isBlank();
        return byAccount ^ byUpi;
    }
}
