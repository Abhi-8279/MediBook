package com.medibook.appointment.dto.request;

import com.medibook.appointment.enums.AppointmentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateAppointmentStatusRequest(
        @NotNull(message = "Status is required")
        AppointmentStatus status,
        @Size(max = 2000, message = "Notes must not exceed 2000 characters")
        String notes,
        @Size(max = 500, message = "Cancellation reason must not exceed 500 characters")
        String cancellationReason) {
}
