package com.medibook.provider.dto.response;

import java.math.BigDecimal;

public record ProviderSummaryResponse(
        String providerId,
        String userId,
        String fullName,
        String profilePicUrl,
        String specialization,
        Integer experienceYears,
        String clinicName,
        String clinicAddress,
        BigDecimal avgRating,
        Integer reviewCount,
        boolean verified,
        boolean available) {
}
