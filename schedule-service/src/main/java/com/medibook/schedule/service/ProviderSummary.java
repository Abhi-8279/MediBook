package com.medibook.schedule.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProviderSummary(
        String providerId,
        String userId,
        boolean verified,
        boolean available) {
}
