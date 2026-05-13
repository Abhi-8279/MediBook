package com.medibook.provider.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record ProviderResponse(
        String providerId,
        String userId,
        String fullName,
        String email,
        String phone,
        String profilePicUrl,
        String specialization,
        String qualification,
        Integer experienceYears,
        String bio,
        String clinicName,
        String clinicAddress,
        BigDecimal avgRating,
        Integer reviewCount,
        boolean verified,
        boolean available,
        String verificationNote,
        Instant createdAt,
        Instant updatedAt,
        Instant verifiedAt) {
}
