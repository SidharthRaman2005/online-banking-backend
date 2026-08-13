package com.bank.online_banking_system.util;

import com.bank.online_banking_system.exception.TransactionFailedException;
import com.bank.online_banking_system.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/** Human-quotable transaction references: {@code TXN} + 10 digits. */
@Component
@RequiredArgsConstructor
public class ReferenceGenerator {

    private static final int MAX_ATTEMPTS = 5;

    private final SecureRandom random = new SecureRandom();
    private final TransactionRepository transactionRepository;

    public String generate() {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            StringBuilder sb = new StringBuilder("TXN");
            for (int i = 0; i < 10; i++) {
                sb.append(random.nextInt(10));
            }
            String candidate = sb.toString();
            if (!transactionRepository.existsByTransactionReference(candidate)) {
                return candidate;
            }
        }
        throw new TransactionFailedException("Could not allocate a transaction reference. Please try again.");
    }
}
