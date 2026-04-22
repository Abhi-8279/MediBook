package com.medibook.review.dto.response;

import java.math.BigDecimal;

public record AverageRatingResponse(
        String providerId,
        BigDecimal avgRating) {
}
