package com.medibook.review.controller;

import com.medibook.review.dto.request.CreateReviewRequest;
import com.medibook.review.dto.request.FlagReviewRequest;
import com.medibook.review.dto.request.UpdateReviewRequest;
import com.medibook.review.dto.response.AverageRatingResponse;
import com.medibook.review.dto.response.MessageResponse;
import com.medibook.review.dto.response.ReviewCountResponse;
import com.medibook.review.dto.response.ReviewResponse;
import com.medibook.review.security.AuthenticatedUser;
import com.medibook.review.service.ReviewService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ReviewResponse> addReview(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody CreateReviewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reviewService.addReview(authenticatedUser, request));
    }

    @GetMapping
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ReviewResponse>> getAllReviews(
            @RequestParam(required = false) String providerId,
            @RequestParam(required = false) String patientId,
            @RequestParam(required = false) String appointmentId,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) Boolean flagged,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return ResponseEntity.ok(reviewService.getAllReviews(
                providerId,
                patientId,
                appointmentId,
                rating,
                flagged,
                authenticatedUser));
    }

    @GetMapping("/providers/{providerId}")
    public ResponseEntity<List<ReviewResponse>> getReviewsByProvider(
            @PathVariable String providerId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return ResponseEntity.ok(reviewService.getReviewsByProviderId(providerId, authenticatedUser));
    }

    @GetMapping("/providers/{providerId}/avg-rating")
    public ResponseEntity<AverageRatingResponse> getAverageRating(@PathVariable String providerId) {
        return ResponseEntity.ok(reviewService.getAverageRating(providerId));
    }

    @GetMapping("/providers/{providerId}/count")
    public ResponseEntity<ReviewCountResponse> getReviewCount(@PathVariable String providerId) {
        return ResponseEntity.ok(reviewService.getReviewCount(providerId));
    }

    @GetMapping("/appointments/{appointmentId}")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ReviewResponse> getReviewByAppointment(
            @PathVariable String appointmentId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return ResponseEntity.ok(reviewService.getReviewByAppointmentId(appointmentId, authenticatedUser));
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<List<ReviewResponse>> getMyReviews(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return ResponseEntity.ok(reviewService.getMyReviews(authenticatedUser));
    }

    @GetMapping("/patients/{patientId}")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('PATIENT','ADMIN')")
    public ResponseEntity<List<ReviewResponse>> getReviewsByPatient(
            @PathVariable String patientId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return ResponseEntity.ok(reviewService.getReviewsByPatientId(patientId, authenticatedUser));
    }

    @PutMapping("/{reviewId}")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('PATIENT','ADMIN')")
    public ResponseEntity<ReviewResponse> updateReview(
            @PathVariable String reviewId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody UpdateReviewRequest request) {
        return ResponseEntity.ok(reviewService.updateReview(reviewId, authenticatedUser, request));
    }

    @PutMapping("/{reviewId}/flag")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('PROVIDER','ADMIN')")
    public ResponseEntity<ReviewResponse> flagReview(
            @PathVariable String reviewId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody FlagReviewRequest request) {
        return ResponseEntity.ok(reviewService.flagReview(reviewId, authenticatedUser, request));
    }

    @DeleteMapping("/{reviewId}")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('PATIENT','ADMIN')")
    public ResponseEntity<MessageResponse> deleteReview(
            @PathVariable String reviewId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return ResponseEntity.ok(reviewService.deleteReview(reviewId, authenticatedUser));
    }
}
