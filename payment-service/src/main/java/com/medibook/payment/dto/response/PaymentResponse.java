package com.medibook.payment.dto.response;

import com.medibook.payment.enums.PaymentMode;
import com.medibook.payment.enums.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(
        String paymentId,
        String appointmentId,
        String patientId,
        String providerId,
        BigDecimal amount,
        PaymentStatus status,
        PaymentMode mode,
        String transactionId,
        String currency,
        Instant paidAt,
        Instant refundedAt,
        String notes,
        Instant createdAt,
        Instant updatedAt) {
}
