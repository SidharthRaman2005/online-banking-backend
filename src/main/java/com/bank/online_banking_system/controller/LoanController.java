package com.bank.online_banking_system.controller;

import com.bank.online_banking_system.dto.response.ApiResponse;
import com.bank.online_banking_system.dto.response.LoanInfoResponse;
import com.bank.online_banking_system.service.LoanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
@Tag(name = "Loans", description = "Public loan information and rates")
public class LoanController {

    private final LoanService loanService;

    @GetMapping
    @Operation(summary = "Get loan overview, types, eligibility, documents, and rates")
    public ResponseEntity<ApiResponse<LoanInfoResponse>> info() {
        LoanInfoResponse response = loanService.getLoanInfo();
        return ResponseEntity.ok(ApiResponse.of("Loan information", response));
    }
}
