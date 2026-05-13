package com.medibook.auth.dto.request;

import com.medibook.auth.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Full name is required")
        @Size(max = 120, message = "Full name must not exceed 120 characters")
        String fullName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
        String password,

        @Pattern(
                regexp = "^$|^[0-9+()\\-\\s]{10,20}$",
                message = "Phone must contain 10-20 valid characters")
        String phone,

        @NotNull(message = "Role is required")
        Role role,

        String profilePicUrl) {
}

