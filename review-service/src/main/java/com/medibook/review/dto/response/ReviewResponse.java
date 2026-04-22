package com.medibook.review.dto.response;

import java.time.Instant;

public record ReviewResponse(
        String reviewId,
        String appointmentId,
        String patientId,
        String providerId,
        Integer rating,
        String comment,
        Instant reviewDate,
        Instant updatedAt,
        boolean verified,
        boolean anonymous,
        boolean flagged,
        String flagReason,
        Instant flaggedAt) {
}
