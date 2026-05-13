package com.medibook.notification.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record FollowUpReminderRequest(
        @NotBlank(message = "Recipient ID is required")
        String recipientId,

        @NotBlank(message = "Record ID is required")
        String recordId,

        @NotBlank(message = "Appointment ID is required")
        String appointmentId,

        @NotBlank(message = "Provider ID is required")
        String providerId,

        @NotNull(message = "Follow-up date is required")
        LocalDate followUpDate,

        @Size(max = 500, message = "Diagnosis must be at most 500 characters")
        String diagnosis,

        @Size(max = 1000, message = "Prescription must be at most 1000 characters")
        String prescription) {
}
