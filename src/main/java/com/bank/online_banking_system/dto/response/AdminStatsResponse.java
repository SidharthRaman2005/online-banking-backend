package com.bank.online_banking_system.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class AdminStatsResponse {

    private final long totalUsers;
    private final long activeUsers;
    private final long deactivatedUsers;
    private final long totalAccounts;

    /** Counted by distinct reference, so one transfer counts once rather than twice. */
    private final long totalTransactions;

    private final BigDecimal totalMoneyInSystem;
}
