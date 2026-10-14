package com.bank.online_banking_system.dto.response;

import com.bank.online_banking_system.entity.BankAccount;
import com.bank.online_banking_system.entity.User;
import com.bank.online_banking_system.enums.AccountStatus;
import com.bank.online_banking_system.enums.Role;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Builder
public class AdminUserResponse {

    private final Long id;
    private final String username;
    private final String email;
    private final Role role;
    private final AccountStatus status;
    private final String accountNumber;
    private final String upiId;
    private final BigDecimal balance;
    private final Instant createdAt;

    public static AdminUserResponse from(User user, BankAccount account) {
        return AdminUserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .status(user.getStatus())
                .accountNumber(account == null ? null : account.getAccountNumber())
                .upiId(account == null ? null : account.getUpiId())
                .balance(account == null ? null : account.getBalance())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
