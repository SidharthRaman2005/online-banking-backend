package com.bank.online_banking_system.security;

import com.bank.online_banking_system.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * The caller's identity always comes from the token, never from the request body — otherwise any
 * user could deposit into, or withdraw from, someone else's account.
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("Authentication required. Please log in.");
        }
        try {
            return Long.valueOf(authentication.getName());
        } catch (NumberFormatException ex) {
            throw new UnauthorizedException("Invalid authentication token");
        }
    }
}
