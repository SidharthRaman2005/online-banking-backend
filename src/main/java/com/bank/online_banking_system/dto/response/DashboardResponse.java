package com.bank.online_banking_system.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter
@Builder
public class DashboardResponse {

    private final String username;
    private final String email;
    private final String accountNumber;
    private final String upiId;
    private final BigDecimal balance;
    private final Instant accountCreatedAt;

    /** Money in and out for the current calendar month. */
    private final BigDecimal totalIncome;
    private final BigDecimal totalExpense;

    private final List<TransactionResponse> recentTransactions;
}
