package com.medibook.notification.dto.response;

import com.medibook.notification.enums.NotificationChannel;
import com.medibook.notification.enums.NotificationType;
import java.time.Instant;

public record NotificationResponse(
        String notificationId,
        String recipientId,
        NotificationType type,
        String title,
        String message,
        NotificationChannel channel,
        String relatedId,
        String relatedType,
        boolean read,
        Instant sentAt) {
}
