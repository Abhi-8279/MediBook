package com.medibook.provider.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ProviderRatingUpdateRequest(
        @NotNull(message = "Average rating is required")
        @DecimalMin(value = "0.0", message = "Average rating must be at least 0")
        @DecimalMax(value = "5.0", message = "Average rating must be at most 5")
        BigDecimal avgRating,

        @NotNull(message = "Review count is required")
        @Min(value = 0, message = "Review count must not be negative")
        Integer reviewCount) {
}
