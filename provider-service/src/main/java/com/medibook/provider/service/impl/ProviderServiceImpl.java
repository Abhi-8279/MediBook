package com.medibook.provider.service.impl;

import com.medibook.provider.dto.request.ProviderAvailabilityUpdateRequest;
import com.medibook.provider.dto.request.ProviderRatingUpdateRequest;
import com.medibook.provider.dto.request.ProviderVerificationRequest;
import com.medibook.provider.dto.request.RegisterProviderRequest;
import com.medibook.provider.dto.request.UpdateProviderRequest;
import com.medibook.provider.dto.response.ProviderResponse;
import com.medibook.provider.dto.response.ProviderSummaryResponse;
import com.medibook.provider.dto.response.SpecializationCountResponse;
import com.medibook.provider.entity.Provider;
import com.medibook.provider.enums.Role;
import com.medibook.provider.exception.DuplicateResourceException;
import com.medibook.provider.exception.ResourceNotFoundException;
import com.medibook.provider.repository.ProviderRepository;
import com.medibook.provider.security.AuthenticatedUser;
import com.medibook.provider.service.AuthServiceGateway;
import com.medibook.provider.service.AuthUserSummary;
import com.medibook.provider.service.ProviderService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class ProviderServiceImpl implements ProviderService {

    private final ProviderRepository providerRepository;
    private final AuthServiceGateway authServiceGateway;

    public ProviderServiceImpl(ProviderRepository providerRepository, AuthServiceGateway authServiceGateway) {
        this.providerRepository = providerRepository;
        this.authServiceGateway = authServiceGateway;
    }

    @Override
    public ProviderResponse registerProvider(AuthenticatedUser authenticatedUser, RegisterProviderRequest request) {
        if (providerRepository.existsByUserId(authenticatedUser.userId())) {
            throw new DuplicateResourceException("A provider profile already exists for this user");
        }

        AuthUserSummary authUser = authServiceGateway.getUserById(authenticatedUser.userId());
        validateProviderAccount(authUser);

        Provider provider = new Provider();
        provider.setProviderId(UUID.randomUUID().toString());
        provider.setUserId(authUser.userId());
        applyAuthSnapshot(provider, authUser);
        provider.setSpecialization(normalizeRequired(request.specialization(), "Specialization is required"));
        provider.setQualification(normalizeRequired(request.qualification(), "Qualification is required"));
        provider.setExperienceYears(request.experienceYears() == null ? 0 : request.experienceYears());
        provider.setBio(blankToNull(request.bio()));
        provider.setClinicName(normalizeRequired(request.clinicName(), "Clinic name is required"));
        provider.setClinicAddress(normalizeRequired(request.clinicAddress(), "Clinic address is required"));
        provider.setAvailable(request.available() == null || request.available());
        provider.setVerified(false);
        provider.setVerificationNote("Pending admin verification");

        return toResponse(providerRepository.save(provider));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProviderSummaryResponse> searchProviders(
            String search,
            String specialization,
            String location,
            Boolean available,
            Boolean verified,
            AuthenticatedUser authenticatedUser) {
        Boolean effectiveVerified = isAdmin(authenticatedUser) ? verified : Boolean.TRUE;
        return providerRepository.searchProviders(
                        blankToNull(search),
                        blankToNull(specialization),
                        blankToNull(location),
                        available,
                        effectiveVerified)
                .stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProviderResponse getProviderById(String providerId, AuthenticatedUser authenticatedUser) {
        Provider provider = findProviderOrThrow(providerId);
        if (!provider.isVerified() && !canAccessUnverifiedProfile(provider, authenticatedUser)) {
            throw new ResourceNotFoundException("Provider not found");
        }
        return toResponse(provider);
    }

    @Override
    @Transactional(readOnly = true)
    public ProviderResponse getMyProviderProfile(AuthenticatedUser authenticatedUser) {
        return toResponse(findProviderByUserIdOrThrow(authenticatedUser.userId()));
    }

    @Override
    public ProviderResponse syncAuthProfile(AuthenticatedUser authenticatedUser) {
        Provider provider = findProviderByUserIdOrThrow(authenticatedUser.userId());
        AuthUserSummary authUser = authServiceGateway.getUserById(authenticatedUser.userId());
        validateProviderAccount(authUser);
        applyAuthSnapshot(provider, authUser);
        return toResponse(providerRepository.save(provider));
    }

    @Override
    public ProviderResponse updateMyProviderProfile(AuthenticatedUser authenticatedUser, UpdateProviderRequest request) {
        Provider provider = findProviderByUserIdOrThrow(authenticatedUser.userId());

        if (request.specialization() != null) {
            provider.setSpecialization(normalizeRequired(request.specialization(), "Specialization is required"));
        }
        if (request.qualification() != null) {
            provider.setQualification(normalizeRequired(request.qualification(), "Qualification is required"));
        }
        if (request.experienceYears() != null) {
            provider.setExperienceYears(request.experienceYears());
        }
        if (request.bio() != null) {
            provider.setBio(blankToNull(request.bio()));
        }
        if (request.clinicName() != null) {
            provider.setClinicName(normalizeRequired(request.clinicName(), "Clinic name is required"));
        }
        if (request.clinicAddress() != null) {
            provider.setClinicAddress(normalizeRequired(request.clinicAddress(), "Clinic address is required"));
        }

        return toResponse(providerRepository.save(provider));
    }

    @Override
    public ProviderResponse verifyProvider(String providerId, ProviderVerificationRequest request) {
        Provider provider = findProviderOrThrow(providerId);
        provider.setVerified(Boolean.TRUE.equals(request.verified()));
        provider.setVerificationNote(blankToNull(request.verificationNote()));
        provider.setVerifiedAt(provider.isVerified() ? Instant.now() : null);
        if (provider.isVerified() && !StringUtils.hasText(provider.getVerificationNote())) {
            provider.setVerificationNote("Verified by admin");
        }
        if (!provider.isVerified() && !StringUtils.hasText(provider.getVerificationNote())) {
            provider.setVerificationNote("Verification rejected or revoked by admin");
        }
        return toResponse(providerRepository.save(provider));
    }

    @Override
    public ProviderResponse updateAvailability(
            String providerId,
            AuthenticatedUser authenticatedUser,
            ProviderAvailabilityUpdateRequest request) {
        Provider provider = findProviderOrThrow(providerId);
        if (!isAdmin(authenticatedUser) && !provider.getUserId().equals(authenticatedUser.userId())) {
            throw new AccessDeniedException("You can only update your own provider availability");
        }
        provider.setAvailable(Boolean.TRUE.equals(request.available()));
        return toResponse(providerRepository.save(provider));
    }

    @Override
    public ProviderResponse updateAvailabilityInternally(String providerId, ProviderAvailabilityUpdateRequest request) {
        Provider provider = findProviderOrThrow(providerId);
        provider.setAvailable(Boolean.TRUE.equals(request.available()));
        return toResponse(providerRepository.save(provider));
    }

    @Override
    public ProviderResponse updateRating(String providerId, ProviderRatingUpdateRequest request) {
        Provider provider = findProviderOrThrow(providerId);
        provider.setAvgRating(normalizeRating(request.avgRating()));
        provider.setReviewCount(request.reviewCount());
        return toResponse(providerRepository.save(provider));
    }

    @Override
    @Transactional(readOnly = true)
    public ProviderResponse getProviderByUserId(String userId) {
        return toResponse(findProviderByUserIdOrThrow(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SpecializationCountResponse> getSpecializationCounts() {
        return providerRepository.countVerifiedProvidersBySpecialization()
                .stream()
                .map(view -> new SpecializationCountResponse(view.getSpecialization(), view.getProviderCount()))
                .toList();
    }

    @Override
    public void deleteProvider(String providerId) {
        Provider provider = findProviderOrThrow(providerId);
        providerRepository.delete(provider);
    }

    private Provider findProviderOrThrow(String providerId) {
        return providerRepository.findByProviderId(providerId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found"));
    }

    private Provider findProviderByUserIdOrThrow(String userId) {
        return providerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider profile not found"));
    }

    private void validateProviderAccount(AuthUserSummary authUser) {
        if (authUser.role() != Role.PROVIDER) {
            throw new IllegalStateException("Only provider accounts can create a provider profile");
        }
        if (!authUser.active()) {
            throw new IllegalStateException("Inactive provider accounts cannot create or manage provider profiles");
        }
    }

    private void applyAuthSnapshot(Provider provider, AuthUserSummary authUser) {
        provider.setFullName(normalizeRequired(authUser.fullName(), "Provider full name is required"));
        provider.setEmail(normalizeRequired(authUser.email(), "Provider email is required").toLowerCase());
        provider.setPhone(blankToNull(authUser.phone()));
        provider.setProfilePicUrl(blankToNull(authUser.profilePicUrl()));
    }

    private boolean canAccessUnverifiedProfile(Provider provider, AuthenticatedUser authenticatedUser) {
        return isAdmin(authenticatedUser)
                || (authenticatedUser != null && provider.getUserId().equals(authenticatedUser.userId()));
    }

    private boolean isAdmin(AuthenticatedUser authenticatedUser) {
        return authenticatedUser != null && authenticatedUser.role() == Role.ADMIN;
    }

    private ProviderResponse toResponse(Provider provider) {
        return new ProviderResponse(
                provider.getProviderId(),
                provider.getUserId(),
                provider.getFullName(),
                provider.getEmail(),
                provider.getPhone(),
                provider.getProfilePicUrl(),
                provider.getSpecialization(),
                provider.getQualification(),
                provider.getExperienceYears(),
                provider.getBio(),
                provider.getClinicName(),
                provider.getClinicAddress(),
                normalizeRating(provider.getAvgRating()),
                provider.getReviewCount(),
                provider.isVerified(),
                provider.isAvailable(),
                provider.getVerificationNote(),
                provider.getCreatedAt(),
                provider.getUpdatedAt(),
                provider.getVerifiedAt());
    }

    private ProviderSummaryResponse toSummaryResponse(Provider provider) {
        return new ProviderSummaryResponse(
                provider.getProviderId(),
                provider.getUserId(),
                provider.getFullName(),
                provider.getProfilePicUrl(),
                provider.getSpecialization(),
                provider.getExperienceYears(),
                provider.getClinicName(),
                provider.getClinicAddress(),
                normalizeRating(provider.getAvgRating()),
                provider.getReviewCount(),
                provider.isVerified(),
                provider.isAvailable());
    }

    private BigDecimal normalizeRating(BigDecimal value) {
        BigDecimal safeValue = value == null ? BigDecimal.ZERO : value;
        return safeValue.setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizeRequired(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
