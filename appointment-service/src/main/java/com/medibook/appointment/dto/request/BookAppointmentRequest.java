package com.medibook.appointment.dto.request;

import com.medibook.appointment.enums.ConsultationMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BookAppointmentRequest(
        @NotBlank(message = "Provider ID is required")
        String providerId,
        @NotBlank(message = "Slot ID is required")
        String slotId,
        @NotBlank(message = "Service type is required")
        @Size(max = 120, message = "Service type must not exceed 120 characters")
        String serviceType,
        @NotNull(message = "Consultation mode is required")
        ConsultationMode modeOfConsultation,
        @Size(max = 2000, message = "Notes must not exceed 2000 characters")
        String notes) {
}
