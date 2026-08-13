package com.bank.online_banking_system.service;

import com.bank.online_banking_system.dto.request.LoginRequest;
import com.bank.online_banking_system.dto.request.RegisterRequest;
import com.bank.online_banking_system.dto.response.AuthResponse;
import com.bank.online_banking_system.dto.response.RegisterResponse;
import com.bank.online_banking_system.dto.response.UserSummaryResponse;
import com.bank.online_banking_system.entity.BankAccount;
import com.bank.online_banking_system.entity.User;
import com.bank.online_banking_system.enums.AccountStatus;
import com.bank.online_banking_system.enums.Role;
import com.bank.online_banking_system.exception.AccountDeactivatedException;
import com.bank.online_banking_system.exception.DuplicateResourceException;
import com.bank.online_banking_system.exception.InvalidCredentialsException;
import com.bank.online_banking_system.repository.BankAccountRepository;
import com.bank.online_banking_system.repository.UserRepository;
import com.bank.online_banking_system.security.JwtService;
import com.bank.online_banking_system.util.AccountNumberGenerator;
import com.bank.online_banking_system.util.InitialBalanceGenerator;
import com.bank.online_banking_system.util.UpiIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final BankAccountRepository bankAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AccountNumberGenerator accountNumberGenerator;
    private final UpiIdGenerator upiIdGenerator;
    private final InitialBalanceGenerator initialBalanceGenerator;

    /**
     * Creates the user and their bank account in one database transaction, so a failure part-way
     * through can never leave a user without an account.
     */
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String username = request.getUsername().trim();
        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByUsername(username)) {
            throw new DuplicateResourceException("That username is already taken");
        }
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("An account with that email already exists");
        }

        User user = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .status(AccountStatus.ACTIVE)
                .build();
        user = userRepository.save(user);

        BankAccount account = BankAccount.builder()
                .accountNumber(accountNumberGenerator.generate())
                .upiId(upiIdGenerator.generate(username))
                .balance(initialBalanceGenerator.generate())
                .status(AccountStatus.ACTIVE)
                .user(user)
                .build();
        account = bankAccountRepository.save(account);

        log.info("Registered user id={} with account {}", user.getId(), account.getAccountNumber());
        return RegisterResponse.from(account);
    }

    /**
     * Read-only by design. Login must never regenerate the account number, UPI ID or balance —
     * it only proves identity and hands back a token.
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        // Same message for an unknown email and a wrong password, so the endpoint cannot be used
        // to discover which emails are registered.
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountDeactivatedException("This account has been deactivated. Contact support.");
        }

        return AuthResponse.builder()
                .token(jwtService.generateToken(user))
                .expiresIn(jwtService.getExpirationSeconds())
                .user(UserSummaryResponse.from(user))
                .build();
    }
}
