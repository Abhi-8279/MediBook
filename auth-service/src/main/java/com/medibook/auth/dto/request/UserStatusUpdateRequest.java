package com.medibook.auth.dto.request;

import jakarta.validation.constraints.NotNull;

public record UserStatusUpdateRequest(
        @NotNull(message = "Active flag is required")
        Boolean active) {
}

