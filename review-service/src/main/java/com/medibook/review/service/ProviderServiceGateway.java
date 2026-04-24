package com.medibook.review.service;

import java.math.BigDecimal;

public interface ProviderServiceGateway {

    ProviderSummary getProviderByUserId(String userId);

    void updateRating(String providerId, BigDecimal avgRating, long reviewCount);
}
