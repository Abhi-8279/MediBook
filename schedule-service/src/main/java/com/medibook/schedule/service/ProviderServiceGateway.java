package com.medibook.schedule.service;

public interface ProviderServiceGateway {

    ProviderSummary getProviderByUserId(String userId);

    void assertProviderPubliclyVisible(String providerId);

    void updateProviderAvailability(String providerId, boolean available);
}
