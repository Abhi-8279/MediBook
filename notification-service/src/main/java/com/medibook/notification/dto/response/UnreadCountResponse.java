package com.medibook.notification.dto.response;

public record UnreadCountResponse(
        String recipientId,
        long unreadCount) {
}
