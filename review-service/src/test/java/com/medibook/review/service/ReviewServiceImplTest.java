package com.medibook.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.medibook.review.dto.request.CreateReviewRequest;
import com.medibook.review.dto.request.FlagReviewRequest;
import com.medibook.review.dto.request.UpdateReviewRequest;
import com.medibook.review.dto.response.AverageRatingResponse;
import com.medibook.review.dto.response.MessageResponse;
import com.medibook.review.dto.response.ReviewCountResponse;
import com.medibook.review.dto.response.ReviewResponse;
import com.medibook.review.entity.Review;
import com.medibook.review.enums.AppointmentStatus;
import com.medibook.review.enums.Role;
import com.medibook.review.exception.ReviewConflictException;
import com.medibook.review.repository.ReviewRepository;
import com.medibook.review.security.AuthenticatedUser;
import com.medibook.review.service.impl.ReviewServiceImpl;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-04-23T08:00:00Z"), ZoneOffset.UTC);

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private AppointmentServiceGateway appointmentServiceGateway;

    @Mock
    private ProviderServiceGateway providerServiceGateway;

    private ReviewServiceImpl reviewService;

    @BeforeEach
    void setUp() {
        reviewService = new ReviewServiceImpl(
                reviewRepository,
                appointmentServiceGateway,
                providerServiceGateway,
                FIXED_CLOCK);
    }

    @Test
    void shouldAddReviewForCompletedAppointmentAndSyncProviderRating() {
        AuthenticatedUser authenticatedUser = new AuthenticatedUser("patient-1", "patient@medibook.com", Role.PATIENT);
        when(reviewRepository.existsByAppointmentId("appointment-1")).thenReturn(false);
        when(appointmentServiceGateway.getAppointmentById("appointment-1"))
                .thenReturn(new AppointmentSummary("appointment-1", "patient-1", "provider-1", AppointmentStatus.COMPLETED));
        when(reviewRepository.saveAndFlush(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reviewRepository.avgRatingByProviderId("provider-1")).thenReturn(4.5d);
        when(reviewRepository.countByProviderId("provider-1")).thenReturn(3L);

        ReviewResponse response = reviewService.addReview(
                authenticatedUser,
                new CreateReviewRequest("appointment-1", 5, "Very helpful consultation", true));

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).saveAndFlush(captor.capture());
        Review savedReview = captor.getValue();

        assertThat(savedReview.getAppointmentId()).isEqualTo("appointment-1");
        assertThat(savedReview.getPatientId()).isEqualTo("patient-1");
        assertThat(savedReview.getProviderId()).isEqualTo("provider-1");
        assertThat(savedReview.getRating()).isEqualTo(5);
        assertThat(savedReview.isAnonymous()).isTrue();
        assertThat(response.patientId()).isEqualTo("patient-1");
        verify(providerServiceGateway).updateRating("provider-1", new BigDecimal("4.50"), 3L);
    }

    @Test
    void shouldRejectDuplicateReviewPerAppointment() {
        AuthenticatedUser authenticatedUser = new AuthenticatedUser("patient-1", "patient@medibook.com", Role.PATIENT);
        when(reviewRepository.existsByAppointmentId("appointment-1")).thenReturn(true);

        assertThatThrownBy(() -> reviewService.addReview(
                        authenticatedUser,
                        new CreateReviewRequest("appointment-1", 4, "Duplicate", false)))
                .isInstanceOf(ReviewConflictException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void shouldRejectReviewForIncompleteAppointment() {
        AuthenticatedUser authenticatedUser = new AuthenticatedUser("patient-1", "patient@medibook.com", Role.PATIENT);
        when(reviewRepository.existsByAppointmentId("appointment-1")).thenReturn(false);
        when(appointmentServiceGateway.getAppointmentById("appointment-1"))
                .thenReturn(new AppointmentSummary("appointment-1", "patient-1", "provider-1", AppointmentStatus.SCHEDULED));

        assertThatThrownBy(() -> reviewService.addReview(
                        authenticatedUser,
                        new CreateReviewRequest("appointment-1", 4, "Too early", false)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("completed appointments");
    }

    @Test
    void shouldHideAnonymousPatientIdentityFromPublicProviderReads() {
        Review review = buildReview();
        review.setAnonymous(true);
        review.setFlagged(true);

        when(reviewRepository.findByProviderIdOrderByReviewDateDesc("provider-1"))
                .thenReturn(List.of(review));

        List<ReviewResponse> responses = reviewService.getReviewsByProviderId("provider-1", null);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().patientId()).isNull();
        assertThat(responses.getFirst().anonymous()).isTrue();
        assertThat(responses.getFirst().flagged()).isTrue();
    }

    @Test
    void shouldAllowReviewedProviderToFlagReview() {
        Review review = buildReview();
        AuthenticatedUser authenticatedUser = new AuthenticatedUser("provider-user-1", "doctor@medibook.com", Role.PROVIDER);

        when(reviewRepository.findByReviewId("review-1")).thenReturn(Optional.of(review));
        when(providerServiceGateway.getProviderByUserId("provider-user-1"))
                .thenReturn(new ProviderSummary("provider-1", "provider-user-1", true, true));
        when(reviewRepository.saveAndFlush(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReviewResponse response = reviewService.flagReview(
                "review-1",
                authenticatedUser,
                new FlagReviewRequest("Abusive language"));

        assertThat(response.flagged()).isTrue();
        assertThat(response.flagReason()).isEqualTo("Abusive language");
        assertThat(response.flaggedAt()).isEqualTo(Instant.parse("2026-04-23T08:00:00Z"));
    }

    @Test
    void shouldDeleteReviewAndRecomputeProviderRating() {
        Review review = buildReview();
        AuthenticatedUser authenticatedUser = new AuthenticatedUser("patient-1", "patient@medibook.com", Role.PATIENT);

        when(reviewRepository.findByReviewId("review-1")).thenReturn(Optional.of(review));
        when(reviewRepository.avgRatingByProviderId("provider-1")).thenReturn(null);
        when(reviewRepository.countByProviderId("provider-1")).thenReturn(0L);

        MessageResponse response = reviewService.deleteReview("review-1", authenticatedUser);

        assertThat(response.message()).isEqualTo("Review deleted successfully");
        verify(reviewRepository).deleteByReviewId("review-1");
        verify(providerServiceGateway).updateRating("provider-1", new BigDecimal("0.00"), 0L);
    }

    @Test
    void shouldUpdateReviewFieldsAndRecomputeProviderRating() {
        Review review = buildReview();
        AuthenticatedUser authenticatedUser = new AuthenticatedUser("patient-1", "patient@medibook.com", Role.PATIENT);

        when(reviewRepository.findByReviewId("review-1")).thenReturn(Optional.of(review));
        when(reviewRepository.saveAndFlush(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reviewRepository.avgRatingByProviderId("provider-1")).thenReturn(4.25d);
        when(reviewRepository.countByProviderId("provider-1")).thenReturn(2L);

        ReviewResponse response = reviewService.updateReview(
                "review-1",
                authenticatedUser,
                new UpdateReviewRequest(4, "Updated review text", false));

        assertThat(response.rating()).isEqualTo(4);
        assertThat(response.comment()).isEqualTo("Updated review text");
        assertThat(response.anonymous()).isFalse();
        verify(providerServiceGateway).updateRating("provider-1", new BigDecimal("4.25"), 2L);
    }

    @Test
    void shouldReturnSeparateAverageRatingAndReviewCount() {
        when(reviewRepository.avgRatingByProviderId("provider-1")).thenReturn(4.0d);
        when(reviewRepository.countByProviderId("provider-1")).thenReturn(6L);

        AverageRatingResponse averageRating = reviewService.getAverageRating("provider-1");
        ReviewCountResponse reviewCount = reviewService.getReviewCount("provider-1");

        assertThat(averageRating.avgRating()).isEqualByComparingTo("4.00");
        assertThat(reviewCount.reviewCount()).isEqualTo(6L);
    }

    @Test
    void shouldFilterAdminAllReviewsByAppointmentId() {
        Review review = buildReview();
        AuthenticatedUser adminUser = new AuthenticatedUser("admin-1", "admin@medibook.com", Role.ADMIN);

        when(reviewRepository.findByAppointmentId("appointment-1")).thenReturn(Optional.of(review));

        List<ReviewResponse> responses = reviewService.getAllReviews(
                null,
                null,
                "appointment-1",
                null,
                null,
                adminUser);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().appointmentId()).isEqualTo("appointment-1");
        assertThat(responses.getFirst().patientId()).isEqualTo("patient-1");
        verify(reviewRepository, never()).searchReviews(any(), any(), any(), any());
    }

    private Review buildReview() {
        Review review = new Review();
        review.setReviewId("review-1");
        review.setAppointmentId("appointment-1");
        review.setPatientId("patient-1");
        review.setProviderId("provider-1");
        review.setRating(5);
        review.setComment("Helpful and clear");
        review.setReviewDate(Instant.parse("2026-04-22T10:00:00Z"));
        review.setUpdatedAt(Instant.parse("2026-04-22T10:00:00Z"));
        review.setVerified(true);
        review.setAnonymous(false);
        review.setFlagged(false);
        return review;
    }
}
