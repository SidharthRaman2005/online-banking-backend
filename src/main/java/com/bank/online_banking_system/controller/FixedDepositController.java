package com.bank.online_banking_system.controller;

import com.bank.online_banking_system.dto.request.CreateFdRequest;
import com.bank.online_banking_system.dto.response.ApiResponse;
import com.bank.online_banking_system.dto.response.FdResponse;
import com.bank.online_banking_system.dto.response.FdTermsResponse;
import com.bank.online_banking_system.service.FixedDepositService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fd")
@RequiredArgsConstructor
@Tag(name = "FixedDeposit", description = "Fixed deposit terms and creation")
public class FixedDepositController {

    private final FixedDepositService fixedDepositService;

    @GetMapping("/terms")
    @Operation(summary = "Get available FD terms and interest rates")
    public ResponseEntity<ApiResponse<FdTermsResponse>> terms() {
        return ResponseEntity.ok(ApiResponse.of("FD terms", fixedDepositService.getTerms()));
    }

    @PostMapping
    @Operation(summary = "Create a fixed deposit by debiting the user's account")
    public ResponseEntity<ApiResponse<FdResponse>> create(@Valid @RequestBody CreateFdRequest request) {
        return ResponseEntity.ok(ApiResponse.of("FD created", fixedDepositService.createFd(request)));
    }

    @GetMapping
    @Operation(summary = "List current user's fixed deposits")
    public ResponseEntity<ApiResponse<java.util.List<FdResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.of("FDs loaded", fixedDepositService.listFds()));
    }
}
