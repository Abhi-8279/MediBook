package com.medibook.payment.dto.request;

import com.medibook.payment.enums.PaymentMode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreateCheckoutOrderRequest(
        @NotBlank(message = "Appointment ID is required")
        String appointmentId,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        BigDecimal amount,

        @NotNull(message = "Payment mode is required")
        PaymentMode mode,

        String currency,
        String notes) {
}
