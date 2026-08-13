package com.bank.online_banking_system.service;

import com.bank.online_banking_system.dto.request.ChangePasswordRequest;
import com.bank.online_banking_system.dto.request.UpdateProfileRequest;
import com.bank.online_banking_system.dto.response.DashboardResponse;
import com.bank.online_banking_system.dto.response.UserProfileResponse;
import com.bank.online_banking_system.entity.BankAccount;
import com.bank.online_banking_system.entity.Transaction;
import com.bank.online_banking_system.entity.User;
import com.bank.online_banking_system.exception.DuplicateResourceException;
import com.bank.online_banking_system.exception.InvalidCredentialsException;
import com.bank.online_banking_system.exception.InvalidTransactionException;
import com.bank.online_banking_system.exception.UserNotFoundException;
import com.bank.online_banking_system.repository.TransactionRepository;
import com.bank.online_banking_system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final AccountService accountService;
    private final TransactionMapper transactionMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.timezone}")
    private String timezone;

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(Long userId) {
        User user = requireUser(userId);
        BankAccount account = accountService.requireAccountOf(userId);

        ZoneId zone = ZoneId.of(timezone);
        Instant monthStart = LocalDate.now(zone).withDayOfMonth(1).atStartOfDay(zone).toInstant();
        Instant nextMonthStart = LocalDate.now(zone).withDayOfMonth(1).plusMonths(1)
                .atStartOfDay(zone).toInstant();

        List<Transaction> recent =
                transactionRepository.findTop5ByAccountIdOrderByCreatedAtDescIdDesc(account.getId());

        return DashboardResponse.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .accountNumber(account.getAccountNumber())
                .upiId(account.getUpiId())
                .balance(account.getBalance())
                .accountCreatedAt(account.getCreatedAt())
                .totalIncome(transactionRepository.sumIncome(account.getId(),
                        account.getAccountNumber(), monthStart, nextMonthStart))
                .totalExpense(transactionRepository.sumExpense(account.getId(),
                        account.getAccountNumber(), monthStart, nextMonthStart))
                .recentTransactions(transactionMapper.toResponses(recent, account))
                .build();
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(Long userId) {
        return UserProfileResponse.from(requireUser(userId), accountService.requireAccountOf(userId));
    }

    /**
     * Updates the username and/or email. The UPI ID is deliberately left alone: it is a payment
     * address other people have saved, so renaming the user must not re-point it.
     * <p>
     * Changing the email is safe for live sessions because the JWT subject is the user id.
     */
    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = requireUser(userId);

        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            String username = request.getUsername().trim();
            if (userRepository.existsByUsernameAndIdNot(username, userId)) {
                throw new DuplicateResourceException("That username is already taken");
            }
            user.setUsername(username);
        }

        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            String email = request.getEmail().trim().toLowerCase();
            if (userRepository.existsByEmailAndIdNot(email, userId)) {
                throw new DuplicateResourceException("An account with that email already exists");
            }
            user.setEmail(email);
        }

        userRepository.save(user);
        log.info("Profile updated for user id={}", userId);
        return UserProfileResponse.from(user, accountService.requireAccountOf(userId));
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = requireUser(userId);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Current password is incorrect");
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new InvalidTransactionException("New password must be different from the current one");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Password changed for user id={}", userId);
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }
}
