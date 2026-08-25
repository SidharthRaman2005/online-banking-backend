package com.bank.online_banking_system.controller;

import com.bank.online_banking_system.dto.response.ApiResponse;
import com.bank.online_banking_system.dto.response.ActivationRequestResponse;
import com.bank.online_banking_system.service.ActivationRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/activation-requests")
@RequiredArgsConstructor
@Tag(name = "AdminActivationRequests", description = "Admin operations for activation requests")
public class AdminActivationRequestController {

    private final ActivationRequestService activationRequestService;

    @GetMapping
    @Operation(summary = "List all activation requests")
    public ResponseEntity<ApiResponse<List<ActivationRequestResponse>>> all() {
        return ResponseEntity.ok(ApiResponse.of("Requests loaded",
                activationRequestService.listAll()));
    }

    @PutMapping("/{id}/accept")
    @Operation(summary = "Accept an activation request")
    public ResponseEntity<ApiResponse<ActivationRequestResponse>> accept(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.of("Request accepted",
                activationRequestService.accept(id)));
    }

    @PutMapping("/{id}/decline")
    @Operation(summary = "Decline an activation request")
    public ResponseEntity<ApiResponse<ActivationRequestResponse>> decline(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.of("Request declined",
                activationRequestService.decline(id)));
    }
}
