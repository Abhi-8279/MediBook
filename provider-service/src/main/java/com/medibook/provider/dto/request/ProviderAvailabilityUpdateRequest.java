package com.medibook.provider.dto.request;

import jakarta.validation.constraints.NotNull;

public record ProviderAvailabilityUpdateRequest(
        @NotNull(message = "Availability status is required")
        Boolean available) {
}
