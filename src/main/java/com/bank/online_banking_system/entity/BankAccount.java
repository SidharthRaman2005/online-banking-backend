package com.bank.online_banking_system.entity;

import com.bank.online_banking_system.enums.AccountStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "bank_accounts", uniqueConstraints = {
        @UniqueConstraint(name = "uk_accounts_number", columnNames = "account_number"),
        @UniqueConstraint(name = "uk_accounts_upi", columnNames = "upi_id"),
        @UniqueConstraint(name = "uk_accounts_user", columnNames = "user_id")
})
@Check(constraints = "balance >= 0")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", nullable = false, length = 10)
    private String accountNumber;

    /** Permanent payment address. Never regenerated, not even when the username changes. */
    @Column(name = "upi_id", nullable = false, length = 40)
    private String upiId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private AccountStatus status = AccountStatus.ACTIVE;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Optimistic locking guard against lost updates on concurrent balance changes. */
    @Version
    private Long version;
}
