package com.bank.online_banking_system.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Both fields are optional; a null field means "leave unchanged". The bean validation annotations
 * below all skip nulls, so only supplied values are checked.
 */
@Getter
@Setter
public class UpdateProfileRequest {

    @Size(min = 3, max = 20, message = "Username must be between 3 and 20 characters")
    @Pattern(regexp = "^[a-zA-Z0-9._]+$",
            message = "Username may only contain letters, digits, dots and underscores")
    private String username;

    @Email(message = "Enter a valid email address")
    @Size(max = 120, message = "Email is too long")
    private String email;

    @JsonIgnore
    @AssertTrue(message = "Provide a username or an email to update")
    public boolean isAtLeastOneFieldPresent() {
        return (username != null && !username.isBlank()) || (email != null && !email.isBlank());
    }
}
