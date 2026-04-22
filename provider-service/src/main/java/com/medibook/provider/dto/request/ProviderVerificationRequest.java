package com.medibook.provider.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProviderVerificationRequest(
        @NotNull(message = "Verification status is required")
        Boolean verified,

        @Size(max = 500, message = "Verification note must not exceed 500 characters")
        String verificationNote) {
}
