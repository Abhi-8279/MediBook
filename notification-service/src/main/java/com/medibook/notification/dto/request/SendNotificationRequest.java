package com.medibook.notification.dto.request;

import com.medibook.notification.enums.NotificationChannel;
import com.medibook.notification.enums.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record SendNotificationRequest(
        @NotBlank(message = "Recipient ID is required")
        String recipientId,

        @NotNull(message = "Notification type is required")
        NotificationType type,

        @NotBlank(message = "Title is required")
        @Size(max = 120, message = "Title must be at most 120 characters")
        String title,

        @NotBlank(message = "Message is required")
        @Size(max = 2000, message = "Message must be at most 2000 characters")
        String message,

        @NotEmpty(message = "At least one channel is required")
        List<NotificationChannel> channels,

        String relatedId,

        @Size(max = 80, message = "Related type must be at most 80 characters")
        String relatedType) {
}
