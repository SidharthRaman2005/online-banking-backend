package com.bank.online_banking_system.dto.response;

import com.bank.online_banking_system.entity.Transaction;
import com.bank.online_banking_system.enums.TransactionDirection;
import com.bank.online_banking_system.enums.TransactionStatus;
import com.bank.online_banking_system.enums.TransactionType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Builder
public class TransactionResponse {

    private final String transactionReference;
    private final TransactionType type;

    /** Resolved for the viewer, so the UI never has to re-derive credit vs debit. */
    private final TransactionDirection direction;

    private final BigDecimal amount;
    private final String senderAccountNumber;
    private final String receiverAccountNumber;

    /** Username of the other party, resolved server-side to avoid N lookups from the client. */
    private final String counterpartyName;

    private final TransactionStatus status;
    private final String description;
    private final BigDecimal balanceAfterTransaction;
    private final Instant createdAt;

    public static TransactionResponse from(Transaction txn, String viewerAccountNumber,
                                           String counterpartyName) {
        return TransactionResponse.builder()
                .transactionReference(txn.getTransactionReference())
                .type(txn.getType())
                .direction(resolveDirection(txn, viewerAccountNumber))
                .amount(txn.getAmount())
                .senderAccountNumber(txn.getSenderAccountNumber())
                .receiverAccountNumber(txn.getReceiverAccountNumber())
                .counterpartyName(counterpartyName)
                .status(txn.getStatus())
                .description(txn.getDescription())
                .balanceAfterTransaction(txn.getBalanceAfterTransaction())
                .createdAt(txn.getCreatedAt())
                .build();
    }

    private static TransactionDirection resolveDirection(Transaction txn, String viewerAccountNumber) {
        return switch (txn.getType()) {
            case DEPOSIT -> TransactionDirection.CREDIT;
            case WITHDRAW -> TransactionDirection.DEBIT;
            case TRANSFER -> viewerAccountNumber.equals(txn.getReceiverAccountNumber())
                    ? TransactionDirection.CREDIT
                    : TransactionDirection.DEBIT;
        };
    }
}
