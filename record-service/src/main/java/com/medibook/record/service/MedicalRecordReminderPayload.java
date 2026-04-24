package com.medibook.record.service;

import java.time.LocalDate;

public record MedicalRecordReminderPayload(
        String recipientId,
        String recordId,
        String appointmentId,
        String providerId,
        LocalDate followUpDate,
        String diagnosis,
        String prescription) {
}
