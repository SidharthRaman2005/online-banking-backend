package com.bank.online_banking_system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

/**
 * {@code UserDetailsServiceAutoConfiguration} is excluded because authentication is handled by
 * {@code AuthService} + the JWT filter. Left enabled, Boot registers an in-memory user and logs a
 * "Using generated security password" line that looks like a real credential but is never used.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class OnlineBankingSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(OnlineBankingSystemApplication.class, args);
    }
}
