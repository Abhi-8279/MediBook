package com.medibook.notification.messaging.event;

import java.time.LocalDate;

public record FollowUpReminderEvent(
        String recipientId,
        String recordId,
        String appointmentId,
        String providerId,
        LocalDate followUpDate,
        String diagnosis,
        String prescription) {
}
