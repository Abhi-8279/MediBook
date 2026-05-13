package com.medibook.review.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProviderSummary(
        String providerId,
        String userId,
        boolean verified,
        boolean available) {
}
