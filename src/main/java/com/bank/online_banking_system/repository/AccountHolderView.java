package com.bank.online_banking_system.repository;

/** Projection used to resolve counterparty names in one query instead of one per transaction. */
public interface AccountHolderView {

    String getAccountNumber();

    String getUsername();
}
