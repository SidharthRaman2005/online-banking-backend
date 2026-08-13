package com.bank.online_banking_system.entity;

import com.bank.online_banking_system.enums.TransactionStatus;
import com.bank.online_banking_system.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One ledger row per account per operation.
 * <p>
 * A TRANSFER writes two rows sharing the same {@code transactionReference}: one owned by the
 * sender's account and one owned by the receiver's account. Each row records the balance of
 * <em>its own</em> account after the operation, so both parties see a figure that is true for them.
 */
@Entity
@Table(name = "transactions",
        uniqueConstraints = @UniqueConstraint(name = "uk_txn_ref_account",
                columnNames = {"transaction_reference", "account_id"}),
        indexes = {
                @Index(name = "idx_txn_account_created", columnList = "account_id, created_at DESC"),
                @Index(name = "idx_txn_reference", columnList = "transaction_reference")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_reference", nullable = false, length = 20)
    private String transactionReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransactionType type;

    /** Always positive. Direction comes from the type plus which account owns this row. */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "sender_account_number", length = 10)
    private String senderAccountNumber;

    @Column(name = "receiver_account_number", length = 10)
    private String receiverAccountNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransactionStatus status;

    @Column(length = 200)
    private String description;

    @Column(name = "balance_after_transaction", nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceAfterTransaction;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** The account this ledger row belongs to. History is always filtered on this column. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private BankAccount account;
}
