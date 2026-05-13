package com.medibook.record.dto.response;

import java.time.Instant;
import java.time.LocalDate;

public record MedicalRecordResponse(
        String recordId,
        String appointmentId,
        String patientId,
        String providerId,
        String diagnosis,
        String prescription,
        String notes,
        String attachmentUrl,
        LocalDate followUpDate,
        Instant followUpReminderSentAt,
        Instant createdAt,
        Instant updatedAt) {
}
