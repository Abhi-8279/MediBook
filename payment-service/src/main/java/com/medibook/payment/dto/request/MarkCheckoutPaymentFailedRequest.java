package com.medibook.payment.dto.request;

import jakarta.validation.constraints.NotBlank;

public record MarkCheckoutPaymentFailedRequest(
        @NotBlank(message = "Payment ID is required")
        String paymentId,
        String razorpayOrderId,
        String razorpayPaymentId,
        String reason) {
}
