package com.medibook.notification.service;

import com.medibook.notification.dto.request.FollowUpReminderRequest;
import com.medibook.notification.dto.request.ScheduleAppointmentRemindersRequest;
import com.medibook.notification.dto.request.SendBulkNotificationRequest;
import com.medibook.notification.dto.request.SendNotificationRequest;
import com.medibook.notification.dto.response.BulkDispatchResponse;
import com.medibook.notification.dto.response.MessageResponse;
import com.medibook.notification.dto.response.NotificationResponse;
import com.medibook.notification.dto.response.ScheduledReminderResponse;
import com.medibook.notification.dto.response.UnreadCountResponse;
import com.medibook.notification.enums.NotificationChannel;
import com.medibook.notification.enums.NotificationType;
import com.medibook.notification.security.AuthenticatedUser;
import java.util.List;

public interface NotificationService {

    List<NotificationResponse> send(SendNotificationRequest request);

    void sendFollowUpReminder(FollowUpReminderRequest request);

    ScheduledReminderResponse scheduleAppointmentReminders(ScheduleAppointmentRemindersRequest request);

    MessageResponse cancelScheduledAppointmentReminders(String appointmentId);

    void dispatchDueScheduledReminders();

    BulkDispatchResponse sendBulk(SendBulkNotificationRequest request, AuthenticatedUser authenticatedUser);

    NotificationResponse markAsRead(String notificationId, AuthenticatedUser authenticatedUser);

    MessageResponse markAllRead(String recipientId, AuthenticatedUser authenticatedUser);

    List<NotificationResponse> getByRecipient(String recipientId, Boolean unreadOnly, AuthenticatedUser authenticatedUser);

    UnreadCountResponse getUnreadCount(String recipientId, AuthenticatedUser authenticatedUser);

    MessageResponse deleteNotification(String notificationId, AuthenticatedUser authenticatedUser);

    List<NotificationResponse> getAll(
            String recipientId,
            NotificationType type,
            NotificationChannel channel,
            Boolean read,
            String relatedId,
            AuthenticatedUser authenticatedUser);
}
