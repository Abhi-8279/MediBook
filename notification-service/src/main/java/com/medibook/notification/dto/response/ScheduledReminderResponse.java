package com.medibook.notification.dto.response;

public record ScheduledReminderResponse(
        String appointmentId,
        int scheduledCount) {
}
