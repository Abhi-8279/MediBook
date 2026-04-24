package com.medibook.notification.dto.request;

import com.medibook.notification.enums.NotificationChannel;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public record ScheduleAppointmentRemindersRequest(
        @NotBlank(message = "Recipient ID is required")
        String recipientId,

        @NotBlank(message = "Appointment ID is required")
        String appointmentId,

        @NotBlank(message = "Provider ID is required")
        String providerId,

        @NotBlank(message = "Title is required")
        @Size(max = 120, message = "Title must be at most 120 characters")
        String title,

        @NotBlank(message = "Message is required")
        @Size(max = 2000, message = "Message must be at most 2000 characters")
        String message,

        @NotEmpty(message = "At least one channel is required")
        List<NotificationChannel> channels,

        @NotNull(message = "24-hour reminder time is required")
        Instant remindAt24Hours,

        @NotNull(message = "1-hour reminder time is required")
        Instant remindAt1Hour) {
}
