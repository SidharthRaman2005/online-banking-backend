package com.bank.online_banking_system.service;

import com.bank.online_banking_system.dto.request.CreateActivationRequest;
import com.bank.online_banking_system.dto.response.ActivationRequestResponse;
import com.bank.online_banking_system.entity.ActivationRequest;
import com.bank.online_banking_system.entity.User;
import com.bank.online_banking_system.enums.AccountStatus;
import com.bank.online_banking_system.enums.ActivationRequestStatus;
import com.bank.online_banking_system.enums.Role;
import com.bank.online_banking_system.exception.AccountDeactivatedException;
import com.bank.online_banking_system.exception.InvalidCredentialsException;
import com.bank.online_banking_system.repository.ActivationRequestRepository;
import com.bank.online_banking_system.repository.UserRepository;
import com.bank.online_banking_system.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ActivationRequestService {

    private final ActivationRequestRepository activationRequestRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;

    @Transactional
    public ActivationRequestResponse create(CreateActivationRequest request) {
        User user = resolveUserForRequest(request);

        ActivationRequest r = ActivationRequest.builder()
                .user(user)
                .message(request.getMessage())
                .build();
        ActivationRequest saved = activationRequestRepository.save(r);

        userRepository.findAllByRole(Role.ADMIN).forEach(admin ->
                notificationService.notify(admin,
                        "New activation request from " + user.getUsername() + ". Review and approve or decline it."));

        return ActivationRequestResponse.from(saved);
    }

    private User resolveUserForRequest(CreateActivationRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() != null
                && !"anonymousUser".equals(String.valueOf(authentication.getPrincipal()))) {
            Long userId = SecurityUtils.getCurrentUserId();
            return userRepository.findById(userId).orElseThrow();
        }

        String email = request.getEmail() == null ? "" : request.getEmail().trim().toLowerCase();
        String password = request.getPassword();
        if (email.isBlank() || password == null || password.isBlank()) {
            throw new InvalidCredentialsException("Authentication required. Please log in.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        if (user.getStatus() != AccountStatus.DEACTIVATED) {
            throw new AccountDeactivatedException("This account has been deactivated. Contact support.");
        }

        return user;
    }

    @Transactional(readOnly = true)
    public List<ActivationRequestResponse> listForUser() {
        Long userId = SecurityUtils.getCurrentUserId();
        return activationRequestRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(ActivationRequestResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<ActivationRequestResponse> listAll() {
        return activationRequestRepository.findAll().stream()
                .map(ActivationRequestResponse::from).toList();
    }

    @Transactional
    public ActivationRequestResponse accept(Long id) {
        ActivationRequest r = activationRequestRepository.findById(id).orElseThrow();
        r.setStatus(ActivationRequestStatus.ACCEPTED);
        User user = r.getUser();
        user.setStatus(AccountStatus.ACTIVE);
        userRepository.save(user);
        activationRequestRepository.save(r);

        notificationService.notify(user,
                "Your activation request was accepted. You can now sign in again.");

        return ActivationRequestResponse.from(r);
    }

    @Transactional
    public ActivationRequestResponse decline(Long id) {
        ActivationRequest r = activationRequestRepository.findById(id).orElseThrow();
        r.setStatus(ActivationRequestStatus.DECLINED);
        activationRequestRepository.save(r);

        notificationService.notify(r.getUser(),
                "Your activation request was declined. Please contact support for more details.");

        return ActivationRequestResponse.from(r);
    }
}
