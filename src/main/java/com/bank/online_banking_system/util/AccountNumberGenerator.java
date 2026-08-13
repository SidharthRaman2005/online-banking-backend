package com.bank.online_banking_system.util;

import com.bank.online_banking_system.exception.TransactionFailedException;
import com.bank.online_banking_system.repository.BankAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * 10-digit account numbers from {@link SecureRandom}. Deliberately not derived from the user id,
 * which would make every other account trivially guessable.
 */
@Component
@RequiredArgsConstructor
public class AccountNumberGenerator {

    private static final int MAX_ATTEMPTS = 5;

    private final SecureRandom random = new SecureRandom();
    private final BankAccountRepository bankAccountRepository;

    public String generate() {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String candidate = build();
            if (!bankAccountRepository.existsByAccountNumber(candidate)) {
                return candidate;
            }
        }
        throw new TransactionFailedException("Could not allocate an account number. Please try again.");
    }

    private String build() {
        StringBuilder sb = new StringBuilder(10);
        sb.append(random.nextInt(1, 10)); // first digit never zero
        for (int i = 1; i < 10; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}
