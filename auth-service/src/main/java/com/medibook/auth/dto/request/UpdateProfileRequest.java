package com.medibook.auth.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 120, message = "Full name must not exceed 120 characters")
        String fullName,

        @Pattern(
                regexp = "^$|^[0-9+()\\-\\s]{10,20}$",
                message = "Phone must contain 10-20 valid characters")
        String phone,

        String profilePicUrl) {
}

