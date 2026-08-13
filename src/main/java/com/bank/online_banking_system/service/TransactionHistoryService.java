package com.bank.online_banking_system.service;

import com.bank.online_banking_system.dto.response.PageResponse;
import com.bank.online_banking_system.dto.response.TransactionResponse;
import com.bank.online_banking_system.entity.BankAccount;
import com.bank.online_banking_system.entity.Transaction;
import com.bank.online_banking_system.enums.TransactionType;
import com.bank.online_banking_system.exception.AccountNotFoundException;
import com.bank.online_banking_system.exception.InvalidTransactionException;
import com.bank.online_banking_system.repository.TransactionRepository;
import com.bank.online_banking_system.repository.spec.TransactionSpecs;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/** Module 5 — transaction history with filtering and pagination. */
@Service
@RequiredArgsConstructor
public class TransactionHistoryService {

    private static final int MAX_PAGE_SIZE = 100;

    private final TransactionRepository transactionRepository;
    private final AccountService accountService;
    private final TransactionMapper transactionMapper;

    @Value("${app.timezone}")
    private String timezone;

    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> history(Long userId, String type, LocalDate from,
                                                     LocalDate to, String search, int page, int size) {
        BankAccount account = accountService.requireAccountOf(userId);
        ZoneId zone = ZoneId.of(timezone);

        Instant fromInstant = from == null ? null : from.atStartOfDay(zone).toInstant();
        // "to" is inclusive for the user, so the exclusive bound is the start of the next day.
        Instant toInstant = to == null ? null : to.plusDays(1).atStartOfDay(zone).toInstant();

        if (fromInstant != null && toInstant != null && !fromInstant.isBefore(toInstant)) {
            throw new InvalidTransactionException("The 'from' date must not be after the 'to' date");
        }

        Specification<Transaction> spec = Specification.where(TransactionSpecs.ownedBy(account.getId()))
                .and(TransactionSpecs.ofType(parseType(type)))
                .and(TransactionSpecs.createdFrom(fromInstant))
                .and(TransactionSpecs.createdBefore(toInstant))
                .and(TransactionSpecs.matching(search));

        PageRequest pageable = PageRequest.of(Math.max(page, 0), clampSize(size),
                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")));

        Page<Transaction> found = transactionRepository.findAll(spec, pageable);
        return PageResponse.of(found, transactionMapper.toResponses(found.getContent(), account));
    }

    @Transactional(readOnly = true)
    public TransactionResponse byReference(Long userId, String reference) {
        BankAccount account = accountService.requireAccountOf(userId);
        List<Transaction> rows =
                transactionRepository.findByTransactionReferenceAndAccountId(reference, account.getId());
        if (rows.isEmpty()) {
            // Also the answer when the reference belongs to someone else — never confirm it exists.
            throw new AccountNotFoundException("Transaction not found");
        }
        return transactionMapper.toResponse(rows.get(0), account);
    }

    /** {@code null} or "ALL" means no type filter. */
    private TransactionType parseType(String type) {
        if (type == null || type.isBlank() || type.equalsIgnoreCase("ALL")) {
            return null;
        }
        try {
            return TransactionType.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new InvalidTransactionException(
                    "Unknown transaction type. Use ALL, DEPOSIT, WITHDRAW or TRANSFER.");
        }
    }

    private int clampSize(int size) {
        if (size <= 0) {
            return 10;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }
}
