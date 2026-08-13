package com.bank.online_banking_system.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;

/** Simulated opening balance, drawn once at registration. */
@Component
public class InitialBalanceGenerator {

    private final SecureRandom random = new SecureRandom();

    @Value("${app.account.initial-balance.min}")
    private long min;

    @Value("${app.account.initial-balance.max}")
    private long max;

    public BigDecimal generate() {
        long value = random.nextLong(min, max + 1);
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }
}
