package com.bank.online_banking_system.dto.response;

import com.bank.online_banking_system.entity.User;
import com.bank.online_banking_system.enums.Role;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserSummaryResponse {

    private final Long id;
    private final String username;
    private final String email;
    private final Role role;

    public static UserSummaryResponse from(User user) {
        return UserSummaryResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}
