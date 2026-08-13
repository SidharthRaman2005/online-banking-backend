package com.bank.online_banking_system.controller;

import com.bank.online_banking_system.dto.response.ApiResponse;
import com.bank.online_banking_system.dto.response.UpiResolveResponse;
import com.bank.online_banking_system.security.SecurityUtils;
import com.bank.online_banking_system.service.UpiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Simulated UPI. Transfers themselves go through {@code POST /api/transactions/transfer} with
 * {@code receiverUpiId} — there is one transfer code path, not two.
 */
@RestController
@RequestMapping("/api/upi")
@RequiredArgsConstructor
@Tag(name = "UPI", description = "Simulated UPI directory")
public class UpiController {

    private final UpiService upiService;

    @GetMapping("/me")
    @Operation(summary = "Your own UPI ID")
    public ResponseEntity<ApiResponse<Map<String, String>>> myUpiId() {
        String upiId = upiService.myUpiId(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.of("UPI ID loaded", Map.of("upiId", upiId)));
    }

    @GetMapping("/resolve")
    @Operation(summary = "Confirm a payee before sending money")
    public ResponseEntity<ApiResponse<UpiResolveResponse>> resolve(@RequestParam String upiId) {
        return ResponseEntity.ok(ApiResponse.of("Payee found", upiService.resolve(upiId)));
    }
}
