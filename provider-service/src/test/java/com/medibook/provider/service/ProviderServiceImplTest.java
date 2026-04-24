package com.medibook.provider.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.medibook.provider.dto.request.ProviderVerificationRequest;
import com.medibook.provider.dto.request.RegisterProviderRequest;
import com.medibook.provider.dto.response.ProviderResponse;
import com.medibook.provider.entity.Provider;
import com.medibook.provider.enums.Role;
import com.medibook.provider.exception.DuplicateResourceException;
import com.medibook.provider.exception.ResourceNotFoundException;
import com.medibook.provider.repository.ProviderRepository;
import com.medibook.provider.security.AuthenticatedUser;
import com.medibook.provider.service.impl.ProviderServiceImpl;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProviderServiceImplTest {

    @Mock
    private ProviderRepository providerRepository;

    @Mock
    private AuthServiceGateway authServiceGateway;

    private ProviderServiceImpl providerService;

    @BeforeEach
    void setUp() {
        providerService = new ProviderServiceImpl(providerRepository, authServiceGateway);
    }

    @Test
    void shouldRegisterProviderProfileFromAuthSnapshot() {
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                "provider-user-1",
                "meera@medibook.com",
                Role.PROVIDER);
        RegisterProviderRequest request = new RegisterProviderRequest(
                "Cardiologist",
                "MBBS, MD Cardiology",
                12,
                "Experienced cardiac specialist",
                "HeartCare Clinic",
                "MG Road, Bengaluru",
                true);

        when(providerRepository.existsByUserId("provider-user-1")).thenReturn(false);
        when(authServiceGateway.getUserById("provider-user-1")).thenReturn(new AuthUserSummary(
                "provider-user-1",
                "Dr. Meera Sharma",
                "meera@medibook.com",
                "+919999999999",
                Role.PROVIDER,
                true,
                "https://example.com/meera.png"));
        when(providerRepository.save(any(Provider.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProviderResponse response = providerService.registerProvider(authenticatedUser, request);

        ArgumentCaptor<Provider> providerCaptor = ArgumentCaptor.forClass(Provider.class);
        verify(providerRepository).save(providerCaptor.capture());
        Provider savedProvider = providerCaptor.getValue();

        assertThat(savedProvider.getUserId()).isEqualTo("provider-user-1");
        assertThat(savedProvider.getFullName()).isEqualTo("Dr. Meera Sharma");
        assertThat(savedProvider.getSpecialization()).isEqualTo("Cardiologist");
        assertThat(savedProvider.isVerified()).isFalse();
        assertThat(savedProvider.isAvailable()).isTrue();
        assertThat(response.email()).isEqualTo("meera@medibook.com");
    }

    @Test
    void shouldRejectDuplicateProviderProfileRegistration() {
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                "provider-user-1",
                "meera@medibook.com",
                Role.PROVIDER);
        RegisterProviderRequest request = new RegisterProviderRequest(
                "Cardiologist",
                "MBBS",
                5,
                null,
                "HeartCare Clinic",
                "MG Road, Bengaluru",
                true);

        when(providerRepository.existsByUserId("provider-user-1")).thenReturn(true);

        assertThatThrownBy(() -> providerService.registerProvider(authenticatedUser, request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void shouldHideUnverifiedProviderFromPublicReads() {
        Provider provider = buildProvider();
        provider.setVerified(false);

        when(providerRepository.findByProviderId("provider-1")).thenReturn(Optional.of(provider));

        assertThatThrownBy(() -> providerService.getProviderById("provider-1", null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Provider not found");
    }

    @Test
    void shouldVerifyProviderAndStampTimestamp() {
        Provider provider = buildProvider();
        provider.setVerified(false);
        provider.setVerificationNote("Pending admin verification");

        when(providerRepository.findByProviderId("provider-1")).thenReturn(Optional.of(provider));
        when(providerRepository.save(any(Provider.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProviderResponse response = providerService.verifyProvider(
                "provider-1",
                new ProviderVerificationRequest(true, "Credentials validated"));

        assertThat(response.verified()).isTrue();
        assertThat(response.verificationNote()).isEqualTo("Credentials validated");
        assertThat(response.verifiedAt()).isNotNull();
    }

    @Test
    void shouldUpdateRatingWithNormalizedScale() {
        Provider provider = buildProvider();

        when(providerRepository.findByProviderId("provider-1")).thenReturn(Optional.of(provider));
        when(providerRepository.save(any(Provider.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProviderResponse response = providerService.updateRating(
                "provider-1",
                new com.medibook.provider.dto.request.ProviderRatingUpdateRequest(new BigDecimal("4.236"), 17));

        assertThat(response.avgRating()).isEqualByComparingTo("4.24");
        assertThat(response.reviewCount()).isEqualTo(17);
    }

    private Provider buildProvider() {
        Provider provider = new Provider();
        provider.setProviderId("provider-1");
        provider.setUserId("provider-user-1");
        provider.setFullName("Dr. Meera Sharma");
        provider.setEmail("meera@medibook.com");
        provider.setSpecialization("Cardiologist");
        provider.setQualification("MBBS, MD Cardiology");
        provider.setExperienceYears(12);
        provider.setClinicName("HeartCare Clinic");
        provider.setClinicAddress("MG Road, Bengaluru");
        provider.setAvgRating(new BigDecimal("4.10"));
        provider.setReviewCount(11);
        provider.setAvailable(true);
        return provider;
    }
}
