package com.bank.online_banking_system.service;

import com.bank.online_banking_system.dto.response.NotificationResponse;
import com.bank.online_banking_system.entity.Notification;
import com.bank.online_banking_system.entity.User;
import com.bank.online_banking_system.exception.AccountNotFoundException;
import com.bank.online_banking_system.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Module 8 — simple in-application notifications. */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    /**
     * Called from inside the banking transaction, so a notification is only ever persisted for an
     * operation that actually committed.
     */
    public void notify(User user, String message) {
        notificationRepository.save(Notification.builder()
                .user(user)
                .message(message)
                .read(false)
                .build());
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> list(Long userId) {
        return notificationRepository.findTop50ByUserIdOrderByCreatedAtDescIdDesc(userId).stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public NotificationResponse markRead(Long userId, Long notificationId) {
        Notification notification = require(userId, notificationId);
        notification.setRead(true);
        return NotificationResponse.from(notificationRepository.save(notification));
    }

    @Transactional
    public void delete(Long userId, Long notificationId) {
        notificationRepository.delete(require(userId, notificationId));
    }

    /** Scoped by user id, so one user can never read or delete another user's notification. */
    private Notification require(Long userId, Long notificationId) {
        return notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new AccountNotFoundException("Notification not found"));
    }
}
