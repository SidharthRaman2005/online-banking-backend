package com.bank.online_banking_system.service;

import com.bank.online_banking_system.exception.GoogleVerificationException;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

@Service
public class GoogleVerificationService {

    private final boolean enabled;
    private final GoogleIdTokenVerifier verifier;

    public GoogleVerificationService(
            @Value("${app.google.signup-verification.enabled:true}") boolean enabled,
            @Value("${app.google.client-id:}") String clientId) {
        this.enabled = enabled;
        this.verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(List.of(clientId))
                .build();
    }

    public String verifyEmail(String credential) {
        if (!enabled) {
            return null;
        }
        if (credential == null || credential.isBlank() || verifier.getAudience().isEmpty()
                || verifier.getAudience().iterator().next().isBlank()) {
            throw new GoogleVerificationException("Google signup verification is not configured");
        }

        try {
            GoogleIdToken token = verifier.verify(credential);
            if (token == null || !Boolean.TRUE.equals(token.getPayload().getEmailVerified())
                    || token.getPayload().getEmail() == null) {
                throw new GoogleVerificationException("Google email verification failed");
            }
            return token.getPayload().getEmail().trim().toLowerCase();
        } catch (GeneralSecurityException | IOException ex) {
            throw new GoogleVerificationException("Google email verification failed");
        }
    }
}
