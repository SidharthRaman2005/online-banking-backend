package com.bank.online_banking_system.dto.response;

import com.bank.online_banking_system.entity.BankAccount;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Returned once, at registration. This is where the user first sees their account number and UPI
 * ID, so the frontend must display both before redirecting to login.
 */
@Getter
@Builder
public class RegisterResponse {

    private final Long userId;
    private final String username;
    private final String email;
    private final String accountNumber;
    private final String upiId;
    private final BigDecimal balance;
    private final Instant accountCreatedAt;

    public static RegisterResponse from(BankAccount account) {
        return RegisterResponse.builder()
                .userId(account.getUser().getId())
                .username(account.getUser().getUsername())
                .email(account.getUser().getEmail())
                .accountNumber(account.getAccountNumber())
                .upiId(account.getUpiId())
                .balance(account.getBalance())
                .accountCreatedAt(account.getCreatedAt())
                .build();
    }
}
