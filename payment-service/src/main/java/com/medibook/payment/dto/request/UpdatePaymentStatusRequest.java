package com.medibook.payment.dto.request;

import com.medibook.payment.enums.PaymentStatus;
import jakarta.validation.constraints.NotNull;

public record UpdatePaymentStatusRequest(
        @NotNull(message = "Payment status is required")
        PaymentStatus status,
        String notes) {
}
