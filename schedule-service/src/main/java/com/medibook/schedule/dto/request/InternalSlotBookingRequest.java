package com.medibook.schedule.dto.request;

import jakarta.validation.constraints.NotBlank;

public record InternalSlotBookingRequest(
        @NotBlank(message = "Booking reference is required")
        String bookingReference) {
}
