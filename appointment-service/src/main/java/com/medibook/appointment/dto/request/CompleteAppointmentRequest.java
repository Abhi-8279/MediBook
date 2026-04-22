package com.medibook.appointment.dto.request;

import jakarta.validation.constraints.Size;

public record CompleteAppointmentRequest(
        @Size(max = 2000, message = "Notes must not exceed 2000 characters")
        String notes) {
}
