package com.bank.online_banking_system.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * Deliberately minimal. Confirming a payee must never leak the receiver's account number or
 * balance to whoever is about to pay them.
 */
@Getter
@Builder
public class UpiResolveResponse {

    private final String upiId;
    private final String accountHolderName;
    private final boolean valid;
}
