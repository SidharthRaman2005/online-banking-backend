package com.bank.online_banking_system.service;

import com.bank.online_banking_system.dto.response.TransactionResponse;
import com.bank.online_banking_system.entity.BankAccount;
import com.bank.online_banking_system.entity.Transaction;
import com.bank.online_banking_system.repository.AccountHolderView;
import com.bank.online_banking_system.repository.BankAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Turns ledger rows into viewer-specific DTOs. Counterparty names are resolved with a single batch
 * query so a page of transactions never triggers one lookup per row.
 */
@Component
@RequiredArgsConstructor
public class TransactionMapper {

    private final BankAccountRepository bankAccountRepository;

    public List<TransactionResponse> toResponses(List<Transaction> transactions, BankAccount viewer) {
        if (transactions.isEmpty()) {
            return List.of();
        }

        String own = viewer.getAccountNumber();
        Set<String> counterpartyNumbers = new HashSet<>();
        for (Transaction txn : transactions) {
            counterpartyNumbers.add(counterpartyNumberOf(txn, own));
        }
        counterpartyNumbers.remove(null);

        Map<String, String> namesByAccount = counterpartyNumbers.isEmpty()
                ? Map.of()
                : bankAccountRepository.findHolderNames(counterpartyNumbers).stream()
                        .collect(Collectors.toMap(AccountHolderView::getAccountNumber,
                                AccountHolderView::getUsername, (a, b) -> a));

        return transactions.stream()
                .map(txn -> {
                    // Deposits and withdrawals have no counterparty, and Map.of() throws on a
                    // null key rather than returning null, so guard before the lookup.
                    String counterparty = counterpartyNumberOf(txn, own);
                    String name = counterparty == null ? null : namesByAccount.get(counterparty);
                    return TransactionResponse.from(txn, own, name);
                })
                .collect(Collectors.toList());
    }

    public TransactionResponse toResponse(Transaction transaction, BankAccount viewer) {
        return toResponses(List.of(transaction), viewer).get(0);
    }

    /** Null for deposits and withdrawals — those have no second party. */
    private String counterpartyNumberOf(Transaction txn, String ownAccountNumber) {
        return switch (txn.getType()) {
            case DEPOSIT, WITHDRAW -> null;
            case TRANSFER -> ownAccountNumber.equals(txn.getSenderAccountNumber())
                    ? txn.getReceiverAccountNumber()
                    : txn.getSenderAccountNumber();
        };
    }
}
