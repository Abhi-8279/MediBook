package com.medibook.provider.service;

import com.medibook.provider.dto.request.ProviderAvailabilityUpdateRequest;
import com.medibook.provider.dto.request.ProviderRatingUpdateRequest;
import com.medibook.provider.dto.request.ProviderVerificationRequest;
import com.medibook.provider.dto.request.RegisterProviderRequest;
import com.medibook.provider.dto.request.UpdateProviderRequest;
import com.medibook.provider.dto.response.ProviderResponse;
import com.medibook.provider.dto.response.ProviderSummaryResponse;
import com.medibook.provider.dto.response.SpecializationCountResponse;
import com.medibook.provider.security.AuthenticatedUser;
import java.util.List;

public interface ProviderService {

    ProviderResponse registerProvider(AuthenticatedUser authenticatedUser, RegisterProviderRequest request);

    List<ProviderSummaryResponse> searchProviders(
            String search,
            String specialization,
            String location,
            Boolean available,
            Boolean verified,
            AuthenticatedUser authenticatedUser);

    ProviderResponse getProviderById(String providerId, AuthenticatedUser authenticatedUser);

    ProviderResponse getMyProviderProfile(AuthenticatedUser authenticatedUser);

    ProviderResponse syncAuthProfile(AuthenticatedUser authenticatedUser);

    ProviderResponse updateMyProviderProfile(AuthenticatedUser authenticatedUser, UpdateProviderRequest request);

    ProviderResponse verifyProvider(String providerId, ProviderVerificationRequest request);

    ProviderResponse updateAvailability(
            String providerId,
            AuthenticatedUser authenticatedUser,
            ProviderAvailabilityUpdateRequest request);

    ProviderResponse updateAvailabilityInternally(String providerId, ProviderAvailabilityUpdateRequest request);

    ProviderResponse updateRating(String providerId, ProviderRatingUpdateRequest request);

    ProviderResponse getProviderByUserId(String userId);

    List<SpecializationCountResponse> getSpecializationCounts();

    void deleteProvider(String providerId);
}
