package com.bank.online_banking_system.service;

import com.bank.online_banking_system.dto.request.UpdateStatusRequest;
import com.bank.online_banking_system.dto.response.*;
import com.bank.online_banking_system.entity.BankAccount;
import com.bank.online_banking_system.entity.Transaction;
import com.bank.online_banking_system.entity.User;
import com.bank.online_banking_system.enums.AccountStatus;
import com.bank.online_banking_system.enums.Role;
import com.bank.online_banking_system.enums.TransactionStatus;
import com.bank.online_banking_system.enums.TransactionType;
import com.bank.online_banking_system.exception.InvalidTransactionException;
import com.bank.online_banking_system.exception.UserNotFoundException;
import com.bank.online_banking_system.repository.BankAccountRepository;
import com.bank.online_banking_system.repository.TransactionRepository;
import com.bank.online_banking_system.repository.UserRepository;
import com.bank.online_banking_system.repository.spec.AccountSpecs;
import com.bank.online_banking_system.repository.spec.TransactionSpecs;
import com.bank.online_banking_system.repository.spec.UserSpecs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Module 7 — administrative monitoring. Every method here is reachable only with ROLE_ADMIN. */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private static final int MAX_PAGE_SIZE = 100;

    private final UserRepository userRepository;
    private final BankAccountRepository bankAccountRepository;
    private final TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public AdminStatsResponse stats() {
        long total = userRepository.count();
        long active = userRepository.countByStatus(AccountStatus.ACTIVE);
        return AdminStatsResponse.builder()
                .totalUsers(total)
                .activeUsers(active)
                .deactivatedUsers(total - active)
                .totalAccounts(bankAccountRepository.count())
                .totalTransactions(transactionRepository.countDistinctOperations())
                .totalMoneyInSystem(bankAccountRepository.sumAllBalances())
                .build();
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> users(String search, AccountStatus status, int page, int size) {
        Specification<User> spec = Specification.where(UserSpecs.matching(search))
                .and(UserSpecs.withStatus(status));

        Page<User> found = userRepository.findAll(spec,
                PageRequest.of(Math.max(page, 0), clamp(size), Sort.by(Sort.Direction.DESC, "id")));

        List<AdminUserResponse> content = found.getContent().stream()
                .map(user -> AdminUserResponse.from(user,
                        bankAccountRepository.findByUserId(user.getId()).orElse(null)))
                .toList();
        return PageResponse.of(found, content);
    }

    @Transactional(readOnly = true)
    public AdminUserResponse user(Long userId) {
        User user = requireUser(userId);
        return AdminUserResponse.from(user, bankAccountRepository.findByUserId(userId).orElse(null));
    }

    /**
     * A deactivated user cannot log in, and their account is frozen for deposits, withdrawals and
     * both sides of a transfer. The action is reversible from the same screen.
     */
    @Transactional
    public AdminUserResponse updateStatus(Long adminId, Long userId, UpdateStatusRequest request) {
        if (adminId.equals(userId)) {
            throw new InvalidTransactionException("You cannot change the status of your own account");
        }

        User user = requireUser(userId);
        if (user.getRole() == Role.ADMIN) {
            throw new InvalidTransactionException("Administrator accounts cannot be deactivated");
        }

        user.setStatus(request.getStatus());
        userRepository.save(user);

        // The bank account is frozen alongside the login, otherwise money could still move.
        BankAccount account = bankAccountRepository.findByUserId(userId).orElse(null);
        if (account != null) {
            account.setStatus(request.getStatus());
            bankAccountRepository.save(account);
        }

        log.info("Admin {} set user {} to {}", adminId, userId, request.getStatus());
        return AdminUserResponse.from(user, account);
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminAccountResponse> accounts(String search, int page, int size) {
        Page<BankAccount> found = bankAccountRepository.findAll(AccountSpecs.matching(search),
                PageRequest.of(Math.max(page, 0), clamp(size), Sort.by(Sort.Direction.DESC, "id")));
        return PageResponse.of(found, found.getContent().stream()
                .map(AdminAccountResponse::from)
                .toList());
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminTransactionResponse> transactions(String type, String status, String search,
                                                               int page, int size) {
        Specification<Transaction> spec = Specification
                .where(TransactionSpecs.ofType(parseType(type)))
                .and(TransactionSpecs.withStatus(parseStatus(status)))
                .and(TransactionSpecs.matching(search));

        Page<Transaction> found = transactionRepository.findAll(spec,
                PageRequest.of(Math.max(page, 0), clamp(size),
                        Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"))));

        return PageResponse.of(found, found.getContent().stream()
                .map(AdminTransactionResponse::from)
                .toList());
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

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

    private TransactionStatus parseStatus(String status) {
        if (status == null || status.isBlank() || status.equalsIgnoreCase("ALL")) {
            return null;
        }
        try {
            return TransactionStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new InvalidTransactionException("Unknown status. Use ALL, SUCCESS or FAILED.");
        }
    }

    private int clamp(int size) {
        return size <= 0 ? 10 : Math.min(size, MAX_PAGE_SIZE);
    }
}
