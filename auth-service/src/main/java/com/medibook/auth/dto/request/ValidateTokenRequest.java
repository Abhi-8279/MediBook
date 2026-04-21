package com.medibook.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ValidateTokenRequest(
        @NotBlank(message = "Access token is required")
        String accessToken) {
}

