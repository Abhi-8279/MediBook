package com.medibook.review.controller;

import com.medibook.review.dto.response.AverageRatingResponse;
import com.medibook.review.dto.response.ReviewCountResponse;
import com.medibook.review.service.ReviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reviews/internal")
public class InternalReviewController {

    private final ReviewService reviewService;

    public InternalReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/providers/{providerId}/avg-rating")
    public ResponseEntity<AverageRatingResponse> getProviderAverageRating(@PathVariable String providerId) {
        return ResponseEntity.ok(reviewService.getAverageRating(providerId));
    }

    @GetMapping("/providers/{providerId}/count")
    public ResponseEntity<ReviewCountResponse> getProviderReviewCount(@PathVariable String providerId) {
        return ResponseEntity.ok(reviewService.getReviewCount(providerId));
    }
}
