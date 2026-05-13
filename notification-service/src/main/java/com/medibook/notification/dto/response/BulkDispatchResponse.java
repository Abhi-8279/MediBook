package com.medibook.notification.dto.response;

public record BulkDispatchResponse(
        int recipientCount,
        int notificationCount) {
}
