package com.bank.online_banking_system.controller;

import com.bank.online_banking_system.dto.request.CreateActivationRequest;
import com.bank.online_banking_system.dto.response.ApiResponse;
import com.bank.online_banking_system.dto.response.ActivationRequestResponse;
import com.bank.online_banking_system.service.ActivationRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/activation-requests")
@RequiredArgsConstructor
@Tag(name = "ActivationRequests", description = "User requests for account re-activation")
public class ActivationRequestController {

    private final ActivationRequestService activationRequestService;

    @PostMapping
    @Operation(summary = "Create an activation request for a deactivated account")
    public ResponseEntity<ApiResponse<ActivationRequestResponse>> create(
            @Valid @RequestBody CreateActivationRequest request) {
        return ResponseEntity.ok(ApiResponse.of("Request submitted",
                activationRequestService.create(request)));
    }

    @GetMapping("/my")
    @Operation(summary = "List my activation requests")
    public ResponseEntity<ApiResponse<List<ActivationRequestResponse>>> my() {
        return ResponseEntity.ok(ApiResponse.of("Requests loaded",
                activationRequestService.listForUser()));
    }
}
