package com.bank.online_banking_system.config;

import com.bank.online_banking_system.entity.BankAccount;
import com.bank.online_banking_system.entity.User;
import com.bank.online_banking_system.enums.AccountStatus;
import com.bank.online_banking_system.enums.Role;
import com.bank.online_banking_system.repository.BankAccountRepository;
import com.bank.online_banking_system.repository.UserRepository;
import com.bank.online_banking_system.util.AccountNumberGenerator;
import com.bank.online_banking_system.util.UpiIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/** Creates the first administrator on startup, once. Does nothing if an admin already exists. */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminSeeder implements CommandLineRunner {

    static final String DEFAULT_PASSWORD = "Admin@123";

    private final UserRepository userRepository;
    private final BankAccountRepository bankAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountNumberGenerator accountNumberGenerator;
    private final UpiIdGenerator upiIdGenerator;

    @Value("${app.admin.username}")
    private String username;

    @Value("${app.admin.email}")
    private String email;

    @Value("${app.admin.password}")
    private String password;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.existsByRole(Role.ADMIN)) {
            return;
        }

        User admin = userRepository.save(User.builder()
                .username(username)
                .email(email.toLowerCase())
                .password(passwordEncoder.encode(password))
                .role(Role.ADMIN)
                .status(AccountStatus.ACTIVE)
                .build());

        // An admin gets an account too, so the shared user endpoints work when signed in as one.
        bankAccountRepository.save(BankAccount.builder()
                .accountNumber(accountNumberGenerator.generate())
                .upiId(upiIdGenerator.generate(username))
                .balance(BigDecimal.ZERO.setScale(2))
                .status(AccountStatus.ACTIVE)
                .user(admin)
                .build());

        log.info("Seeded administrator '{}' <{}>", username, email);
        if (DEFAULT_PASSWORD.equals(password)) {
            log.warn("The administrator is using the default password. "
                    + "Set ADMIN_PASSWORD before showing this to anyone.");
        }
    }
}
