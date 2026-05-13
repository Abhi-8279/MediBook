package com.medibook.record.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record UpdateMedicalRecordRequest(
        @NotBlank(message = "Diagnosis is required")
        String diagnosis,

        @NotBlank(message = "Prescription is required")
        String prescription,

        String notes,
        LocalDate followUpDate) {
}
