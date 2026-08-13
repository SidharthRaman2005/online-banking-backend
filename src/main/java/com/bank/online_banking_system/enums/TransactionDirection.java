package com.bank.online_banking_system.enums;

/**
 * Direction of a transaction from the point of view of the account that owns the ledger row.
 * Computed per viewer so the UI never has to re-derive it.
 */
public enum TransactionDirection {
    CREDIT,
    DEBIT
}
