package com.medibook.review.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FlagReviewRequest(
        @NotBlank(message = "Flag reason is required")
        @Size(max = 500, message = "Flag reason must be at most 500 characters")
        String reason) {
}
