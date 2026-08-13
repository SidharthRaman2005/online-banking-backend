package com.bank.online_banking_system.util;

import com.bank.online_banking_system.exception.TransactionFailedException;
import com.bank.online_banking_system.repository.BankAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Builds {@code username@obs}. Generated once at registration and never regenerated afterwards —
 * a UPI ID is a payment address other people save, so silently re-pointing it would break payers.
 */
@Component
@RequiredArgsConstructor
public class UpiIdGenerator {

    private static final int MAX_ATTEMPTS = 5;

    private final SecureRandom random = new SecureRandom();
    private final BankAccountRepository bankAccountRepository;

    @Value("${app.upi.domain}")
    private String domain;

    public String generate(String username) {
        String handle = sanitize(username);
        String candidate = handle + "@" + domain;

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            if (!bankAccountRepository.existsByUpiId(candidate)) {
                return candidate;
            }
            candidate = handle + random.nextInt(100, 1000) + "@" + domain;
        }
        throw new TransactionFailedException("Could not allocate a UPI ID. Please try again.");
    }

    private String sanitize(String username) {
        String handle = username.toLowerCase().replaceAll("[^a-z0-9._]", "");
        if (handle.length() < 3) {
            handle = "user" + handle;
        }
        return handle.length() > 20 ? handle.substring(0, 20) : handle;
    }
}
