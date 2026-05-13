package com.medibook.payment.messaging;

import com.medibook.payment.enums.PaymentMode;
import com.medibook.payment.enums.PaymentStatus;
import java.math.BigDecimal;

public record PaymentProcessedEvent(
        String paymentId,
        String appointmentId,
        String patientId,
        String providerId,
        BigDecimal amount,
        String currency,
        PaymentMode mode,
        PaymentStatus status) {
}
