package com.bank.online_banking_system.repository;

import com.bank.online_banking_system.entity.BankAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BankAccountRepository extends JpaRepository<BankAccount, Long>,
        JpaSpecificationExecutor<BankAccount> {

    Optional<BankAccount> findByAccountNumber(String accountNumber);

    Optional<BankAccount> findByUpiId(String upiId);

    Optional<BankAccount> findByUserId(Long userId);

    boolean existsByAccountNumber(String accountNumber);

    boolean existsByUpiId(String upiId);

    /**
     * Row-level write lock. Money-moving code must load accounts through this method, ordered by
     * ascending id, so two users transferring to each other at the same time cannot deadlock.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from BankAccount a where a.id = :id")
    Optional<BankAccount> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from BankAccount a where a.user.id = :userId")
    Optional<BankAccount> findByUserIdForUpdate(@Param("userId") Long userId);

    @Query("select coalesce(sum(a.balance), 0) from BankAccount a")
    BigDecimal sumAllBalances();

    @Query("""
            select a.accountNumber as accountNumber, u.username as username
            from BankAccount a join a.user u
            where a.accountNumber in :accountNumbers
            """)
    List<AccountHolderView> findHolderNames(@Param("accountNumbers") Collection<String> accountNumbers);
}
