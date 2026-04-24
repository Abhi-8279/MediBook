package com.medibook.notification.messaging.event;

import java.math.BigDecimal;

public record PaymentProcessedEvent(
        String paymentId,
        String appointmentId,
        String patientId,
        String providerId,
        BigDecimal amount,
        String currency,
        String mode,
        String status) {
}
