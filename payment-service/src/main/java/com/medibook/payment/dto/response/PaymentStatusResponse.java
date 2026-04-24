package com.medibook.payment.dto.response;

import com.medibook.payment.enums.PaymentStatus;
import java.time.Instant;

public record PaymentStatusResponse(
        String paymentId,
        String appointmentId,
        PaymentStatus status,
        String transactionId,
        Instant paidAt,
        Instant refundedAt) {
}
