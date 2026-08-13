package com.bank.online_banking_system.service;

import com.bank.online_banking_system.dto.request.AmountRequest;
import com.bank.online_banking_system.dto.response.TransactionResponse;
import com.bank.online_banking_system.entity.BankAccount;
import com.bank.online_banking_system.entity.Transaction;
import com.bank.online_banking_system.enums.AccountStatus;
import com.bank.online_banking_system.enums.TransactionStatus;
import com.bank.online_banking_system.enums.TransactionType;
import com.bank.online_banking_system.exception.AccountInactiveException;
import com.bank.online_banking_system.exception.AccountNotFoundException;
import com.bank.online_banking_system.exception.InsufficientBalanceException;
import com.bank.online_banking_system.repository.BankAccountRepository;
import com.bank.online_banking_system.repository.TransactionRepository;
import com.bank.online_banking_system.util.ReferenceGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Deposits and withdrawals, plus the shared ledger writer used by transfers.
 * <p>
 * Both operations are simulated — no payment gateway is involved. The account row is loaded under
 * a pessimistic write lock, so two concurrent operations on the same account are serialised by the
 * database and the balance can never be read-modify-written from a stale value.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final BankAccountRepository bankAccountRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final ReferenceGenerator referenceGenerator;
    private final NotificationService notificationService;

    @Transactional
    public TransactionResponse deposit(Long userId, AmountRequest request) {
        BigDecimal amount = normalise(request.getAmount());
        BankAccount account = lockAccountOf(userId);

        account.setBalance(account.getBalance().add(amount));
        bankAccountRepository.save(account);

        Transaction txn = record(account, TransactionType.DEPOSIT, amount,
                null, account.getAccountNumber(),
                defaultDescription(request.getDescription(), "Deposit"),
                referenceGenerator.generate());

        notificationService.notify(account.getUser(),
                "%s deposited successfully.".formatted(money(amount)));

        log.info("Deposit of {} into account {}", amount, account.getAccountNumber());
        return transactionMapper.toResponse(txn, account);
    }

    @Transactional
    public TransactionResponse withdraw(Long userId, AmountRequest request) {
        BigDecimal amount = normalise(request.getAmount());
        BankAccount account = lockAccountOf(userId);

        if (account.getBalance().compareTo(amount) < 0) {
            // Thrown before any mutation, so the rollback has nothing to undo.
            throw new InsufficientBalanceException("Insufficient balance for this withdrawal");
        }

        account.setBalance(account.getBalance().subtract(amount));
        bankAccountRepository.save(account);

        Transaction txn = record(account, TransactionType.WITHDRAW, amount,
                account.getAccountNumber(), null,
                defaultDescription(request.getDescription(), "Withdrawal"),
                referenceGenerator.generate());

        notificationService.notify(account.getUser(),
                "Withdrawal of %s successful.".formatted(money(amount)));

        log.info("Withdrawal of {} from account {}", amount, account.getAccountNumber());
        return transactionMapper.toResponse(txn, account);
    }

    /**
     * Writes one ledger row for the given account. {@code balanceAfterTransaction} is read from the
     * account after its balance has already been updated, so it is always true for this account.
     * A transfer calls this twice with the same reference — once per side.
     */
    Transaction record(BankAccount account, TransactionType type, BigDecimal amount,
                       String senderAccountNumber, String receiverAccountNumber,
                       String description, String reference) {
        return transactionRepository.save(Transaction.builder()
                .transactionReference(reference)
                .type(type)
                .amount(amount)
                .senderAccountNumber(senderAccountNumber)
                .receiverAccountNumber(receiverAccountNumber)
                .status(TransactionStatus.SUCCESS)
                .description(description)
                .balanceAfterTransaction(account.getBalance())
                .account(account)
                .build());
    }

    private BankAccount lockAccountOf(Long userId) {
        BankAccount account = bankAccountRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new AccountNotFoundException("No bank account found for this user"));
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountInactiveException("This account is not active");
        }
        return account;
    }

    /** Money is stored to 2 decimals; @Digits has already rejected anything finer. */
    static BigDecimal normalise(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    static String defaultDescription(String supplied, String fallback) {
        return (supplied == null || supplied.isBlank()) ? fallback : supplied.trim();
    }

    /** Indian digit grouping, matching how the frontend renders every other amount. */
    static String money(BigDecimal amount) {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.forLanguageTag("en-IN"));
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        return "₹" + format.format(amount);
    }
}
