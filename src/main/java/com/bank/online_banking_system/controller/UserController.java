package com.bank.online_banking_system.controller;

import com.bank.online_banking_system.dto.request.ChangePasswordRequest;
import com.bank.online_banking_system.dto.request.UpdateProfileRequest;
import com.bank.online_banking_system.dto.response.ApiResponse;
import com.bank.online_banking_system.dto.response.DashboardResponse;
import com.bank.online_banking_system.dto.response.UserProfileResponse;
import com.bank.online_banking_system.security.SecurityUtils;
import com.bank.online_banking_system.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "User", description = "Dashboard and profile management")
public class UserController {

    private final UserService userService;

    @GetMapping("/dashboard")
    @Operation(summary = "Banking dashboard for the authenticated user")
    public ResponseEntity<ApiResponse<DashboardResponse>> dashboard() {
        DashboardResponse dashboard = userService.getDashboard(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.of("Dashboard loaded", dashboard));
    }

    @GetMapping("/profile")
    @Operation(summary = "Profile and account details")
    public ResponseEntity<ApiResponse<UserProfileResponse>> profile() {
        UserProfileResponse profile = userService.getProfile(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.of("Profile loaded", profile));
    }

    @PutMapping("/profile")
    @Operation(summary = "Update username and/or email. The UPI ID never changes.")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request) {
        UserProfileResponse updated =
                userService.updateProfile(SecurityUtils.getCurrentUserId(), request);
        return ResponseEntity.ok(ApiResponse.of("Profile updated", updated));
    }

    @PutMapping("/change-password")
    @Operation(summary = "Change the account password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(SecurityUtils.getCurrentUserId(), request);
        return ResponseEntity.ok(ApiResponse.message("Password changed successfully"));
    }
}
