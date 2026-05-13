package com.medibook.payment.dto.request;

import jakarta.validation.constraints.NotBlank;

public record VerifyCheckoutPaymentRequest(
        @NotBlank(message = "Payment ID is required")
        String paymentId,

        @NotBlank(message = "Razorpay order ID is required")
        String razorpayOrderId,

        @NotBlank(message = "Razorpay payment ID is required")
        String razorpayPaymentId,

        @NotBlank(message = "Razorpay signature is required")
        String razorpaySignature) {
}
