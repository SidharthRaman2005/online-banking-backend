package com.bank.online_banking_system.service;

import com.bank.online_banking_system.dto.response.UpiResolveResponse;
import com.bank.online_banking_system.entity.BankAccount;
import com.bank.online_banking_system.exception.InvalidTransactionException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

/**
 * Module 6 — simulated UPI directory. Nothing here touches the real UPI network.
 * <p>
 * These methods are transactional because resolving a payee walks from the account to its lazily
 * loaded user; with {@code open-in-view=false} that association can only be traversed inside an
 * active session.
 */
@Service
@RequiredArgsConstructor
public class UpiService {

    private static final Pattern UPI_FORMAT = Pattern.compile("^[A-Za-z0-9._]{3,20}@obs$");

    private final AccountService accountService;

    @Transactional(readOnly = true)
    public String myUpiId(Long userId) {
        return accountService.requireAccountOf(userId).getUpiId();
    }

    @Transactional(readOnly = true)
    public UpiResolveResponse resolve(String upiId) {
        String candidate = upiId == null ? "" : upiId.trim();
        if (!UPI_FORMAT.matcher(candidate).matches()) {
            throw new InvalidTransactionException("UPI ID must look like username@obs");
        }

        BankAccount account = accountService.requireByUpiId(candidate);
        return UpiResolveResponse.builder()
                .upiId(account.getUpiId())
                .accountHolderName(account.getUser().getUsername())
                .valid(true)
                .build();
    }
}
