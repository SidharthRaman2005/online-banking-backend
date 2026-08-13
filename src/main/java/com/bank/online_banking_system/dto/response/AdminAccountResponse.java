package com.bank.online_banking_system.dto.response;

import com.bank.online_banking_system.entity.BankAccount;
import com.bank.online_banking_system.enums.AccountStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Builder
public class AdminAccountResponse {

    private final Long id;
    private final String accountNumber;
    private final String upiId;
    private final BigDecimal balance;
    private final AccountStatus status;
    private final String holderName;
    private final String holderEmail;
    private final Instant createdAt;

    public static AdminAccountResponse from(BankAccount account) {
        return AdminAccountResponse.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .upiId(account.getUpiId())
                .balance(account.getBalance())
                .status(account.getStatus())
                .holderName(account.getUser().getUsername())
                .holderEmail(account.getUser().getEmail())
                .createdAt(account.getCreatedAt())
                .build();
    }
}
