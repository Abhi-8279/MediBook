package com.medibook.payment.dto.response;

import com.medibook.payment.enums.PaymentMode;
import com.medibook.payment.enums.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public record InvoiceResponse(
        String invoiceNumber,
        String paymentId,
        String appointmentId,
        String patientId,
        String providerId,
        String serviceType,
        LocalDate appointmentDate,
        LocalTime appointmentStartTime,
        LocalTime appointmentEndTime,
        BigDecimal amount,
        String currency,
        PaymentMode mode,
        PaymentStatus status,
        String transactionId,
        Instant paidAt,
        Instant generatedAt,
        String notes) {
}
