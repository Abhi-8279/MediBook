package com.medibook.provider.dto.response;

public record SpecializationCountResponse(
        String specialization,
        long providerCount) {
}
