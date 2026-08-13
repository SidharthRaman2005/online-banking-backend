package com.bank.online_banking_system.controller;

import com.bank.online_banking_system.dto.response.ApiResponse;
import com.bank.online_banking_system.dto.response.NotificationResponse;
import com.bank.online_banking_system.security.SecurityUtils;
import com.bank.online_banking_system.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "In-app notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "List your 50 most recent notifications")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.of("Notifications loaded",
                notificationService.list(SecurityUtils.getCurrentUserId())));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Number of unread notifications")
    public ResponseEntity<ApiResponse<Map<String, Long>>> unreadCount() {
        long count = notificationService.unreadCount(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.of("Unread count", Map.of("unread", count)));
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "Mark a notification as read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markRead(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.of("Notification marked as read",
                notificationService.markRead(SecurityUtils.getCurrentUserId(), id)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a notification")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        notificationService.delete(SecurityUtils.getCurrentUserId(), id);
        return ResponseEntity.ok(ApiResponse.message("Notification deleted"));
    }
}
