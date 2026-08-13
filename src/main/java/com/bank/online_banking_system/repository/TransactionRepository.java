package com.bank.online_banking_system.repository;

import com.bank.online_banking_system.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long>,
        JpaSpecificationExecutor<Transaction> {

    boolean existsByTransactionReference(String transactionReference);

    List<Transaction> findTop5ByAccountIdOrderByCreatedAtDescIdDesc(Long accountId);

    List<Transaction> findByTransactionReferenceAndAccountId(String reference, Long accountId);

    /** A transfer writes two rows under one reference, so operations are counted by distinct ref. */
    @Query("select count(distinct t.transactionReference) from Transaction t")
    long countDistinctOperations();

    /**
     * Money in: deposits plus transfers where this account was the receiver.
     * Aggregated in the database — never load the rows just to sum them.
     */
    @Query("""
            select coalesce(sum(t.amount), 0) from Transaction t
            where t.account.id = :accountId
              and t.status = com.bank.online_banking_system.enums.TransactionStatus.SUCCESS
              and t.createdAt >= :from and t.createdAt < :to
              and (t.type = com.bank.online_banking_system.enums.TransactionType.DEPOSIT
                   or (t.type = com.bank.online_banking_system.enums.TransactionType.TRANSFER
                       and t.receiverAccountNumber = :accountNumber))
            """)
    BigDecimal sumIncome(@Param("accountId") Long accountId,
                         @Param("accountNumber") String accountNumber,
                         @Param("from") Instant from,
                         @Param("to") Instant to);

    /** Money out: withdrawals plus transfers where this account was the sender. */
    @Query("""
            select coalesce(sum(t.amount), 0) from Transaction t
            where t.account.id = :accountId
              and t.status = com.bank.online_banking_system.enums.TransactionStatus.SUCCESS
              and t.createdAt >= :from and t.createdAt < :to
              and (t.type = com.bank.online_banking_system.enums.TransactionType.WITHDRAW
                   or (t.type = com.bank.online_banking_system.enums.TransactionType.TRANSFER
                       and t.senderAccountNumber = :accountNumber))
            """)
    BigDecimal sumExpense(@Param("accountId") Long accountId,
                          @Param("accountNumber") String accountNumber,
                          @Param("from") Instant from,
                          @Param("to") Instant to);
}
