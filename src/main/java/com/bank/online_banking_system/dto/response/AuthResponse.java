package com.bank.online_banking_system.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponse {

    private final String token;

    @Builder.Default
    private final String tokenType = "Bearer";

    /** Seconds until the token expires. */
    private final long expiresIn;

    private final UserSummaryResponse user;
}
