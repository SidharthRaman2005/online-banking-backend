package com.bank.online_banking_system.service;

import com.bank.online_banking_system.entity.BankAccount;
import com.bank.online_banking_system.exception.AccountNotFoundException;
import com.bank.online_banking_system.repository.BankAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Shared account lookups. Kept in one place so every module resolves accounts the same way. */
@Service
@RequiredArgsConstructor
public class AccountService {

    private final BankAccountRepository bankAccountRepository;

    public BankAccount requireAccountOf(Long userId) {
        return bankAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new AccountNotFoundException("No bank account found for this user"));
    }

    public BankAccount requireByAccountNumber(String accountNumber) {
        return bankAccountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(
                        "No account found with number " + accountNumber));
    }

    public BankAccount requireByUpiId(String upiId) {
        return bankAccountRepository.findByUpiId(upiId.toLowerCase())
                .orElseThrow(() -> new AccountNotFoundException("No account found for UPI ID " + upiId));
    }
}
