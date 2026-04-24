package com.medibook.appointment.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RescheduleAppointmentRequest(
        @NotBlank(message = "New slot ID is required")
        String slotId) {
}
