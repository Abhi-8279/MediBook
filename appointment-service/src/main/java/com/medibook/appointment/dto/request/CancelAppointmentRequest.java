package com.medibook.appointment.dto.request;

import jakarta.validation.constraints.Size;

public record CancelAppointmentRequest(
        @Size(max = 500, message = "Cancellation reason must not exceed 500 characters")
        String reason) {
}
