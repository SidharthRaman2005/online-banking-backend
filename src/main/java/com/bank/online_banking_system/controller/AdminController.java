package com.bank.online_banking_system.controller;

import com.bank.online_banking_system.dto.request.UpdateStatusRequest;
import com.bank.online_banking_system.dto.response.*;
import com.bank.online_banking_system.enums.AccountStatus;
import com.bank.online_banking_system.security.SecurityUtils;
import com.bank.online_banking_system.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Every route here sits behind {@code hasRole("ADMIN")} in SecurityConfig; a USER token gets 403.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "System monitoring and user management")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/stats")
    @Operation(summary = "System-wide totals")
    public ResponseEntity<ApiResponse<AdminStatsResponse>> stats() {
        return ResponseEntity.ok(ApiResponse.of("Stats loaded", adminService.stats()));
    }

    @GetMapping("/users")
    @Operation(summary = "All users, searchable and paginated")
    public ResponseEntity<ApiResponse<PageResponse<AdminUserResponse>>> users(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) AccountStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.of("Users loaded",
                adminService.users(search, status, page, size)));
    }

    @GetMapping("/users/{id}")
    @Operation(summary = "One user with their account details")
    public ResponseEntity<ApiResponse<AdminUserResponse>> user(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.of("User loaded", adminService.user(id)));
    }

    @PutMapping("/users/{id}/status")
    @Operation(summary = "Activate or deactivate a user")
    public ResponseEntity<ApiResponse<AdminUserResponse>> updateStatus(
            @PathVariable Long id, @Valid @RequestBody UpdateStatusRequest request) {
        AdminUserResponse updated =
                adminService.updateStatus(SecurityUtils.getCurrentUserId(), id, request);
        return ResponseEntity.ok(ApiResponse.of("User status updated", updated));
    }

    @GetMapping("/accounts")
    @Operation(summary = "All bank accounts, searchable and paginated")
    public ResponseEntity<ApiResponse<PageResponse<AdminAccountResponse>>> accounts(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.of("Accounts loaded",
                adminService.accounts(search, page, size)));
    }

    @GetMapping("/transactions")
    @Operation(summary = "All ledger rows, filterable by type and status")
    public ResponseEntity<ApiResponse<PageResponse<AdminTransactionResponse>>> transactions(
            @RequestParam(defaultValue = "ALL") String type,
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.of("Transactions loaded",
                adminService.transactions(type, status, search, page, size)));
    }
}
