package com.bank.online_banking_system.dto.response;

import com.bank.online_banking_system.entity.Transaction;
import com.bank.online_banking_system.enums.TransactionStatus;
import com.bank.online_banking_system.enums.TransactionType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Admin view of a ledger row. Unlike the user-facing DTO there is no {@code direction}, because an
 * admin has no "own account" to judge it against. A transfer therefore appears as two rows sharing
 * a reference — one per side — which is what a ledger should show.
 */
@Getter
@Builder
public class AdminTransactionResponse {

    private final Long id;
    private final String transactionReference;
    private final TransactionType type;
    private final BigDecimal amount;
    private final String senderAccountNumber;
    private final String receiverAccountNumber;

    /** The account whose ledger this row belongs to. */
    private final String ledgerAccountNumber;
    private final String ledgerAccountHolder;

    private final TransactionStatus status;
    private final String description;
    private final BigDecimal balanceAfterTransaction;
    private final Instant createdAt;

    public static AdminTransactionResponse from(Transaction txn) {
        return AdminTransactionResponse.builder()
                .id(txn.getId())
                .transactionReference(txn.getTransactionReference())
                .type(txn.getType())
                .amount(txn.getAmount())
                .senderAccountNumber(txn.getSenderAccountNumber())
                .receiverAccountNumber(txn.getReceiverAccountNumber())
                .ledgerAccountNumber(txn.getAccount().getAccountNumber())
                .ledgerAccountHolder(txn.getAccount().getUser().getUsername())
                .status(txn.getStatus())
                .description(txn.getDescription())
                .balanceAfterTransaction(txn.getBalanceAfterTransaction())
                .createdAt(txn.getCreatedAt())
                .build();
    }
}
