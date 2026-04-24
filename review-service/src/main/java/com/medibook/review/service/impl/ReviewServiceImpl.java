package com.medibook.review.service.impl;

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
import com.medibook.review.exception.ResourceNotFoundException;
import com.medibook.review.exception.ReviewConflictException;
import com.medibook.review.repository.ReviewRepository;
import com.medibook.review.security.AuthenticatedUser;
import com.medibook.review.service.AppointmentServiceGateway;
import com.medibook.review.service.AppointmentSummary;
import com.medibook.review.service.ProviderServiceGateway;
import com.medibook.review.service.ProviderSummary;
import com.medibook.review.service.ReviewService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final AppointmentServiceGateway appointmentServiceGateway;
    private final ProviderServiceGateway providerServiceGateway;
    private final Clock clock;

    public ReviewServiceImpl(
            ReviewRepository reviewRepository,
            AppointmentServiceGateway appointmentServiceGateway,
            ProviderServiceGateway providerServiceGateway,
            Clock clock) {
        this.reviewRepository = reviewRepository;
        this.appointmentServiceGateway = appointmentServiceGateway;
        this.providerServiceGateway = providerServiceGateway;
        this.clock = clock;
    }

    @Override
    public ReviewResponse addReview(AuthenticatedUser authenticatedUser, CreateReviewRequest request) {
        assertPatient(authenticatedUser);
        if (reviewRepository.existsByAppointmentId(request.appointmentId().trim())) {
            throw new ReviewConflictException("A review already exists for this appointment");
        }

        AppointmentSummary appointment = appointmentServiceGateway.getAppointmentById(request.appointmentId().trim());
        assertCompletedAppointmentOwnership(appointment, authenticatedUser);

        Review review = new Review();
        review.setReviewId(UUID.randomUUID().toString());
        review.setAppointmentId(appointment.appointmentId());
        review.setPatientId(authenticatedUser.userId());
        review.setProviderId(appointment.providerId());
        review.setRating(request.rating());
        review.setComment(blankToNull(request.comment()));
        review.setVerified(true);
        review.setAnonymous(Boolean.TRUE.equals(request.anonymous()));
        review.setFlagged(false);
        review.setFlagReason(null);
        review.setFlaggedAt(null);
        review.setReviewDate(Instant.now(clock));
        review.setUpdatedAt(Instant.now(clock));

        Review saved = reviewRepository.saveAndFlush(review);
        syncProviderRating(saved.getProviderId());
        return toResponse(saved, authenticatedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviewsByProviderId(String providerId, AuthenticatedUser authenticatedUser) {
        String normalizedProviderId = normalizeRequired(providerId, "Provider ID is required");
        List<Review> reviews = reviewRepository.findByProviderIdOrderByReviewDateDesc(normalizedProviderId);
        return reviews.stream().map(review -> toResponse(review, authenticatedUser)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AverageRatingResponse getAverageRating(String providerId) {
        String normalizedProviderId = normalizeRequired(providerId, "Provider ID is required");
        return new AverageRatingResponse(
                normalizedProviderId,
                computeAverageRating(normalizedProviderId));
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewCountResponse getReviewCount(String providerId) {
        String normalizedProviderId = normalizeRequired(providerId, "Provider ID is required");
        return new ReviewCountResponse(
                normalizedProviderId,
                reviewRepository.countByProviderId(normalizedProviderId));
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewResponse getReviewByAppointmentId(String appointmentId, AuthenticatedUser authenticatedUser) {
        Review review = reviewRepository.findByAppointmentId(normalizeRequired(appointmentId, "Appointment ID is required"))
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        assertCanAccessReview(review, authenticatedUser);
        return toResponse(review, authenticatedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> getMyReviews(AuthenticatedUser authenticatedUser) {
        assertPatient(authenticatedUser);
        return reviewRepository.findByPatientIdOrderByReviewDateDesc(authenticatedUser.userId())
                .stream()
                .map(review -> toResponse(review, authenticatedUser))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviewsByPatientId(String patientId, AuthenticatedUser authenticatedUser) {
        String normalizedPatientId = normalizeRequired(patientId, "Patient ID is required");
        if (!isAdmin(authenticatedUser) && !normalizedPatientId.equals(authenticatedUser.userId())) {
            throw new AccessDeniedException("You can only access your own reviews");
        }
        return reviewRepository.findByPatientIdOrderByReviewDateDesc(normalizedPatientId)
                .stream()
                .map(review -> toResponse(review, authenticatedUser))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> getAllReviews(
            String providerId,
            String patientId,
            String appointmentId,
            Integer rating,
            Boolean flagged,
            AuthenticatedUser authenticatedUser) {
        if (!isAdmin(authenticatedUser)) {
            throw new AccessDeniedException("Only admins can access all reviews");
        }

        String normalizedAppointmentId = blankToNull(appointmentId);
        if (normalizedAppointmentId != null) {
            Review review = reviewRepository.findByAppointmentId(normalizedAppointmentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
            if (matchesFilters(review, blankToNull(providerId), blankToNull(patientId), rating, flagged)) {
                return List.of(toResponse(review, authenticatedUser));
            }
            return List.of();
        }

        return reviewRepository.searchReviews(
                        blankToNull(providerId),
                        blankToNull(patientId),
                        rating,
                        flagged)
                .stream()
                .map(review -> toResponse(review, authenticatedUser))
                .toList();
    }

    @Override
    public ReviewResponse updateReview(
            String reviewId,
            AuthenticatedUser authenticatedUser,
            UpdateReviewRequest request) {
        Review review = findReviewOrThrow(reviewId);
        assertCanModifyReview(review, authenticatedUser);

        if (request.rating() != null) {
            review.setRating(request.rating());
        }
        if (request.comment() != null) {
            review.setComment(blankToNull(request.comment()));
        }
        if (request.anonymous() != null) {
            review.setAnonymous(request.anonymous());
        }

        Review saved = reviewRepository.saveAndFlush(review);
        syncProviderRating(saved.getProviderId());
        return toResponse(saved, authenticatedUser);
    }

    @Override
    public ReviewResponse flagReview(
            String reviewId,
            AuthenticatedUser authenticatedUser,
            FlagReviewRequest request) {
        Review review = findReviewOrThrow(reviewId);
        assertCanFlagReview(review, authenticatedUser);

        review.setFlagged(true);
        review.setFlagReason(normalizeRequired(request.reason(), "Flag reason is required"));
        review.setFlaggedAt(Instant.now(clock));

        return toResponse(reviewRepository.saveAndFlush(review), authenticatedUser);
    }

    @Override
    public MessageResponse deleteReview(String reviewId, AuthenticatedUser authenticatedUser) {
        Review review = findReviewOrThrow(reviewId);
        assertCanModifyReview(review, authenticatedUser);

        String providerId = review.getProviderId();
        reviewRepository.deleteByReviewId(review.getReviewId());
        reviewRepository.flush();
        syncProviderRating(providerId);
        return new MessageResponse("Review deleted successfully");
    }

    private Review findReviewOrThrow(String reviewId) {
        return reviewRepository.findByReviewId(normalizeRequired(reviewId, "Review ID is required"))
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
    }

    private void assertCompletedAppointmentOwnership(AppointmentSummary appointment, AuthenticatedUser authenticatedUser) {
        if (appointment.status() != AppointmentStatus.COMPLETED) {
            throw new IllegalStateException("Reviews can only be submitted for completed appointments");
        }
        if (!appointment.patientId().equals(authenticatedUser.userId())) {
            throw new AccessDeniedException("You can only review your own completed appointments");
        }
    }

    private void assertPatient(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null || authenticatedUser.role() != Role.PATIENT) {
            throw new AccessDeniedException("Only patients can submit reviews");
        }
    }

    private void assertCanAccessReview(Review review, AuthenticatedUser authenticatedUser) {
        if (isAdmin(authenticatedUser)) {
            return;
        }
        if (isPatientOwner(review, authenticatedUser)) {
            return;
        }
        if (isProviderOwner(review.getProviderId(), authenticatedUser)) {
            return;
        }
        throw new AccessDeniedException("You are not allowed to access this review");
    }

    private void assertCanModifyReview(Review review, AuthenticatedUser authenticatedUser) {
        if (isAdmin(authenticatedUser) || isPatientOwner(review, authenticatedUser)) {
            return;
        }
        throw new AccessDeniedException("You are not allowed to modify this review");
    }

    private void assertCanFlagReview(Review review, AuthenticatedUser authenticatedUser) {
        if (isAdmin(authenticatedUser) || isProviderOwner(review.getProviderId(), authenticatedUser)) {
            return;
        }
        throw new AccessDeniedException("Only the reviewed provider or an admin can flag this review");
    }

    private boolean isPatientOwner(Review review, AuthenticatedUser authenticatedUser) {
        return authenticatedUser != null
                && authenticatedUser.role() == Role.PATIENT
                && review.getPatientId().equals(authenticatedUser.userId());
    }

    private boolean isProviderOwner(String providerId, AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null || authenticatedUser.role() != Role.PROVIDER) {
            return false;
        }
        try {
            return providerId.equals(resolveCurrentProviderId(authenticatedUser));
        } catch (ResourceNotFoundException exception) {
            return false;
        }
    }

    private String resolveCurrentProviderId(AuthenticatedUser authenticatedUser) {
        ProviderSummary provider = providerServiceGateway.getProviderByUserId(authenticatedUser.userId());
        return provider.providerId();
    }

    private boolean isAdmin(AuthenticatedUser authenticatedUser) {
        return authenticatedUser != null && authenticatedUser.role() == Role.ADMIN;
    }

    private ReviewResponse toResponse(Review review, AuthenticatedUser authenticatedUser) {
        boolean revealPatientId = !review.isAnonymous()
                || isAdmin(authenticatedUser)
                || isPatientOwner(review, authenticatedUser);
        return new ReviewResponse(
                review.getReviewId(),
                review.getAppointmentId(),
                revealPatientId ? review.getPatientId() : null,
                review.getProviderId(),
                review.getRating(),
                review.getComment(),
                review.getReviewDate(),
                review.getUpdatedAt(),
                review.isVerified(),
                review.isAnonymous(),
                review.isFlagged(),
                review.getFlagReason(),
                review.getFlaggedAt());
    }

    private void syncProviderRating(String providerId) {
        providerServiceGateway.updateRating(
                providerId,
                computeAverageRating(providerId),
                reviewRepository.countByProviderId(providerId));
    }

    private BigDecimal computeAverageRating(String providerId) {
        Double average = reviewRepository.avgRatingByProviderId(providerId);
        if (average == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(average).setScale(2, RoundingMode.HALF_UP);
    }

    private boolean matchesFilters(Review review, String providerId, String patientId, Integer rating, Boolean flagged) {
        if (providerId != null && !providerId.equals(review.getProviderId())) {
            return false;
        }
        if (patientId != null && !patientId.equals(review.getPatientId())) {
            return false;
        }
        if (rating != null && !rating.equals(review.getRating())) {
            return false;
        }
        if (flagged != null && flagged.booleanValue() != review.isFlagged()) {
            return false;
        }
        return true;
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
