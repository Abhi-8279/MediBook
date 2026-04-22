package com.medibook.review.service;

import com.medibook.review.dto.request.CreateReviewRequest;
import com.medibook.review.dto.request.FlagReviewRequest;
import com.medibook.review.dto.request.UpdateReviewRequest;
import com.medibook.review.dto.response.AverageRatingResponse;
import com.medibook.review.dto.response.MessageResponse;
import com.medibook.review.dto.response.ReviewCountResponse;
import com.medibook.review.dto.response.ReviewResponse;
import com.medibook.review.security.AuthenticatedUser;
import java.util.List;

public interface ReviewService {

    ReviewResponse addReview(AuthenticatedUser authenticatedUser, CreateReviewRequest request);

    List<ReviewResponse> getReviewsByProviderId(String providerId, AuthenticatedUser authenticatedUser);

    AverageRatingResponse getAverageRating(String providerId);

    ReviewCountResponse getReviewCount(String providerId);

    ReviewResponse getReviewByAppointmentId(String appointmentId, AuthenticatedUser authenticatedUser);

    List<ReviewResponse> getMyReviews(AuthenticatedUser authenticatedUser);

    List<ReviewResponse> getReviewsByPatientId(String patientId, AuthenticatedUser authenticatedUser);

    List<ReviewResponse> getAllReviews(
            String providerId,
            String patientId,
            String appointmentId,
            Integer rating,
            Boolean flagged,
            AuthenticatedUser authenticatedUser);

    ReviewResponse updateReview(
            String reviewId,
            AuthenticatedUser authenticatedUser,
            UpdateReviewRequest request);

    ReviewResponse flagReview(
            String reviewId,
            AuthenticatedUser authenticatedUser,
            FlagReviewRequest request);

    MessageResponse deleteReview(String reviewId, AuthenticatedUser authenticatedUser);
}
