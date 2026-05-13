package com.medibook.notification.messaging.event;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentRefundedEvent(
        String paymentId,
        String appointmentId,
        String patientId,
        String providerId,
        BigDecimal amount,
        String currency,
        Instant refundedAt,
        String notes) {
}
