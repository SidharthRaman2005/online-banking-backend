package com.bank.online_banking_system.controller;

import com.bank.online_banking_system.dto.request.AmountRequest;
import com.bank.online_banking_system.dto.request.TransferRequest;
import com.bank.online_banking_system.dto.response.ApiResponse;
import com.bank.online_banking_system.dto.response.PageResponse;
import com.bank.online_banking_system.dto.response.TransactionResponse;
import com.bank.online_banking_system.security.SecurityUtils;
import com.bank.online_banking_system.service.TransactionHistoryService;
import com.bank.online_banking_system.service.TransactionService;
import com.bank.online_banking_system.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Deposit, withdraw, transfer and history")
public class TransactionController {

    private final TransactionService transactionService;
    private final TransferService transferService;
    private final TransactionHistoryService historyService;

    @PostMapping("/deposit")
    @Operation(summary = "Deposit money into your own account (simulated)")
    public ResponseEntity<ApiResponse<TransactionResponse>> deposit(
            @Valid @RequestBody AmountRequest request) {
        TransactionResponse txn = transactionService.deposit(SecurityUtils.getCurrentUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of("Deposit successful", txn));
    }

    @PostMapping("/withdraw")
    @Operation(summary = "Withdraw money from your own account")
    public ResponseEntity<ApiResponse<TransactionResponse>> withdraw(
            @Valid @RequestBody AmountRequest request) {
        TransactionResponse txn = transactionService.withdraw(SecurityUtils.getCurrentUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of("Withdrawal successful", txn));
    }

    @PostMapping("/transfer")
    @Operation(summary = "Transfer money by account number or UPI ID")
    public ResponseEntity<ApiResponse<TransactionResponse>> transfer(
            @Valid @RequestBody TransferRequest request) {
        TransactionResponse txn = transferService.transfer(SecurityUtils.getCurrentUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of("Transfer successful", txn));
    }

    @GetMapping
    @Operation(summary = "Your transaction history, filtered and paginated")
    public ResponseEntity<ApiResponse<PageResponse<TransactionResponse>>> history(
            @RequestParam(defaultValue = "ALL") String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PageResponse<TransactionResponse> result = historyService.history(
                SecurityUtils.getCurrentUserId(), type, from, to, search, page, size);
        return ResponseEntity.ok(ApiResponse.of("Transactions loaded", result));
    }

    @GetMapping("/{reference}")
    @Operation(summary = "A single transaction of yours, by reference")
    public ResponseEntity<ApiResponse<TransactionResponse>> byReference(@PathVariable String reference) {
        TransactionResponse txn =
                historyService.byReference(SecurityUtils.getCurrentUserId(), reference);
        return ResponseEntity.ok(ApiResponse.of("Transaction loaded", txn));
    }
}
