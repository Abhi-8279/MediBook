package com.medibook.record.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record CreateMedicalRecordRequest(
        @NotBlank(message = "Appointment ID is required")
        String appointmentId,

        @NotBlank(message = "Diagnosis is required")
        String diagnosis,

        @NotBlank(message = "Prescription is required")
        String prescription,

        String notes,
        String attachmentUrl,
        LocalDate followUpDate) {
}
