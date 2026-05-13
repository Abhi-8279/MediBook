package com.medibook.payment.dto.response;

import java.math.BigDecimal;

public record CheckoutOrderResponse(
        String paymentId,
        String appointmentId,
        String razorpayOrderId,
        String keyId,
        BigDecimal amount,
        long amountInSubunits,
        String currency,
        String businessName,
        String description,
        String imageUrl) {
}
