package com.medibook.appointment.service;

public interface ProviderServiceGateway {

    ProviderSummary getProviderByUserId(String userId);

    void assertProviderPubliclyVisible(String providerId);
}
