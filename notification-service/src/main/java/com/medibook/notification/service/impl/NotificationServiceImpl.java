package com.medibook.notification.service.impl;

import com.medibook.notification.dto.request.FollowUpReminderRequest;
import com.medibook.notification.dto.request.ScheduleAppointmentRemindersRequest;
import com.medibook.notification.dto.request.SendBulkNotificationRequest;
import com.medibook.notification.dto.request.SendNotificationRequest;
import com.medibook.notification.dto.response.BulkDispatchResponse;
import com.medibook.notification.dto.response.MessageResponse;
import com.medibook.notification.dto.response.NotificationResponse;
import com.medibook.notification.dto.response.ScheduledReminderResponse;
import com.medibook.notification.dto.response.UnreadCountResponse;
import com.medibook.notification.entity.Notification;
import com.medibook.notification.entity.NotificationSchedule;
import com.medibook.notification.enums.BroadcastAudience;
import com.medibook.notification.enums.NotificationChannel;
import com.medibook.notification.enums.NotificationType;
import com.medibook.notification.enums.Role;
import com.medibook.notification.exception.ResourceNotFoundException;
import com.medibook.notification.repository.NotificationRepository;
import com.medibook.notification.repository.NotificationScheduleRepository;
import com.medibook.notification.security.AuthenticatedUser;
import com.medibook.notification.service.AuthServiceGateway;
import com.medibook.notification.service.AuthUserSummary;
import com.medibook.notification.service.EmailSender;
import com.medibook.notification.service.NotificationService;
import com.medibook.notification.service.SmsSender;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private static final DateTimeFormatter FOLLOW_UP_DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM uuuu");

    private final NotificationRepository notificationRepository;
    private final NotificationScheduleRepository notificationScheduleRepository;
    private final AuthServiceGateway authServiceGateway;
    private final EmailSender emailSender;
    private final SmsSender smsSender;
    private final Clock clock;

    public NotificationServiceImpl(
            NotificationRepository notificationRepository,
            NotificationScheduleRepository notificationScheduleRepository,
            AuthServiceGateway authServiceGateway,
            EmailSender emailSender,
            SmsSender smsSender,
            Clock clock) {
        this.notificationRepository = notificationRepository;
        this.notificationScheduleRepository = notificationScheduleRepository;
        this.authServiceGateway = authServiceGateway;
        this.emailSender = emailSender;
        this.smsSender = smsSender;
        this.clock = clock;
    }

    @Override
    public List<NotificationResponse> send(SendNotificationRequest request) {
        AuthUserSummary recipient = authServiceGateway.getUserById(normalizeRequired(
                request.recipientId(),
                "Recipient ID is required"));
        if (!recipient.active()) {
            throw new IllegalStateException("Notifications cannot be sent to inactive users");
        }

        List<Notification> notifications = distinctChannels(request.channels()).stream()
                .map(channel -> buildNotification(
                        recipient.userId(),
                        request.type(),
                        request.title(),
                        request.message(),
                        channel,
                        request.relatedId(),
                        request.relatedType()))
                .toList();

        List<Notification> savedNotifications = notificationRepository.saveAll(notifications);
        savedNotifications.forEach(notification -> dispatchByChannel(notification, recipient));
        return savedNotifications.stream().map(this::toResponse).toList();
    }

    @Override
    public void sendFollowUpReminder(FollowUpReminderRequest request) {
        String title = "Follow-up reminder";
        String message = buildFollowUpMessage(request.followUpDate(), request.diagnosis(), request.prescription());
        SendNotificationRequest sendRequest = new SendNotificationRequest(
                request.recipientId(),
                NotificationType.FOLLOWUP,
                title,
                message,
                List.of(NotificationChannel.APP, NotificationChannel.EMAIL, NotificationChannel.SMS),
                request.recordId(),
                "MEDICAL_RECORD");
        send(sendRequest);
    }

    @Override
    public ScheduledReminderResponse scheduleAppointmentReminders(ScheduleAppointmentRemindersRequest request) {
        String appointmentId = normalizeRequired(request.appointmentId(), "Appointment ID is required");
        cancelPendingSchedules(appointmentId);
        AuthUserSummary recipient = authServiceGateway.getUserById(normalizeRequired(
                request.recipientId(),
                "Recipient ID is required"));
        if (!recipient.active()) {
            throw new IllegalStateException("Reminders cannot be scheduled for inactive users");
        }

        List<NotificationSchedule> schedules = distinctChannels(request.channels()).stream()
                .flatMap(channel -> List.of(
                                buildSchedule(
                                        recipient.userId(),
                                        NotificationType.REMINDER,
                                        request.title(),
                                        request.message(),
                                        channel,
                                        appointmentId,
                                        "APPOINTMENT",
                                        request.remindAt24Hours()),
                                buildSchedule(
                                        recipient.userId(),
                                        NotificationType.REMINDER,
                                        request.title(),
                                        request.message(),
                                        channel,
                                        appointmentId,
                                        "APPOINTMENT",
                                        request.remindAt1Hour()))
                        .stream())
                .filter(schedule -> schedule.getTriggerAt().isAfter(Instant.now(clock)))
                .toList();

        notificationScheduleRepository.saveAll(schedules);
        return new ScheduledReminderResponse(appointmentId, schedules.size());
    }

    @Override
    public MessageResponse cancelScheduledAppointmentReminders(String appointmentId) {
        long deleted = cancelPendingSchedules(normalizeRequired(appointmentId, "Appointment ID is required"));
        return new MessageResponse("Cancelled " + deleted + " scheduled appointment reminder(s)");
    }

    @Override
    @Scheduled(fixedDelayString = "${app.reminders.dispatch-interval-ms:30000}")
    public void dispatchDueScheduledReminders() {
        Instant now = Instant.now(clock);
        List<NotificationSchedule> dueSchedules = notificationScheduleRepository
                .findByProcessedFalseAndTriggerAtLessThanEqualOrderByTriggerAtAsc(now);
        for (NotificationSchedule schedule : dueSchedules) {
            Notification notification = buildNotification(
                    schedule.getRecipientId(),
                    schedule.getType(),
                    schedule.getTitle(),
                    schedule.getMessage(),
                    schedule.getChannel(),
                    schedule.getRelatedId(),
                    schedule.getRelatedType());
            Notification savedNotification = notificationRepository.save(notification);
            dispatchByChannel(savedNotification);
            schedule.setProcessed(true);
            schedule.setProcessedAt(now);
            notificationScheduleRepository.save(schedule);
        }
    }

    @Override
    public BulkDispatchResponse sendBulk(SendBulkNotificationRequest request, AuthenticatedUser authenticatedUser) {
        assertAdmin(authenticatedUser);
        List<AuthUserSummary> recipients = resolveRecipients(request.audience());
        List<NotificationDispatch> notifications = recipients.stream()
                .flatMap(recipient -> distinctChannels(request.channels()).stream()
                        .map(channel -> new NotificationDispatch(
                                buildNotification(
                                        recipient.userId(),
                                        request.type(),
                                        request.title(),
                                        request.message(),
                                        channel,
                                        request.relatedId(),
                                        request.relatedType()),
                                recipient)))
                .toList();

        List<Notification> savedNotifications = notificationRepository.saveAll(
                notifications.stream().map(NotificationDispatch::notification).toList());
        for (int i = 0; i < savedNotifications.size(); i++) {
            dispatchByChannel(savedNotifications.get(i), notifications.get(i).recipient());
        }
        return new BulkDispatchResponse(recipients.size(), savedNotifications.size());
    }

    @Override
    public NotificationResponse markAsRead(String notificationId, AuthenticatedUser authenticatedUser) {
        Notification notification = findNotificationOrThrow(notificationId);
        assertCanAccessRecipient(notification.getRecipientId(), authenticatedUser);
        notification.setRead(true);
        return toResponse(notificationRepository.save(notification));
    }

    @Override
    public MessageResponse markAllRead(String recipientId, AuthenticatedUser authenticatedUser) {
        String normalizedRecipientId = normalizeRequired(recipientId, "Recipient ID is required");
        assertCanAccessRecipient(normalizedRecipientId, authenticatedUser);
        notificationRepository.markAllReadByRecipientId(normalizedRecipientId);
        return new MessageResponse("All notifications marked as read");
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getByRecipient(
            String recipientId,
            Boolean unreadOnly,
            AuthenticatedUser authenticatedUser) {
        String normalizedRecipientId = normalizeRequired(recipientId, "Recipient ID is required");
        assertCanAccessRecipient(normalizedRecipientId, authenticatedUser);
        List<Notification> notifications = Boolean.TRUE.equals(unreadOnly)
                ? notificationRepository.findByRecipientIdAndReadOrderBySentAtDesc(normalizedRecipientId, false)
                : notificationRepository.findByRecipientIdOrderBySentAtDesc(normalizedRecipientId);
        return notifications.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount(String recipientId, AuthenticatedUser authenticatedUser) {
        String normalizedRecipientId = normalizeRequired(recipientId, "Recipient ID is required");
        assertCanAccessRecipient(normalizedRecipientId, authenticatedUser);
        return new UnreadCountResponse(
                normalizedRecipientId,
                notificationRepository.countByRecipientIdAndRead(normalizedRecipientId, false));
    }

    @Override
    public MessageResponse deleteNotification(String notificationId, AuthenticatedUser authenticatedUser) {
        Notification notification = findNotificationOrThrow(notificationId);
        assertCanAccessRecipient(notification.getRecipientId(), authenticatedUser);
        notificationRepository.deleteByNotificationId(notification.getNotificationId());
        return new MessageResponse("Notification deleted successfully");
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getAll(
            String recipientId,
            NotificationType type,
            NotificationChannel channel,
            Boolean read,
            String relatedId,
            AuthenticatedUser authenticatedUser) {
        assertAdmin(authenticatedUser);
        return notificationRepository.searchNotifications(
                        blankToNull(recipientId),
                        type,
                        channel,
                        read,
                        blankToNull(relatedId))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private Notification findNotificationOrThrow(String notificationId) {
        return notificationRepository.findByNotificationId(normalizeRequired(notificationId, "Notification ID is required"))
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
    }

    private Notification buildNotification(
            String recipientId,
            NotificationType type,
            String title,
            String message,
            NotificationChannel channel,
            String relatedId,
            String relatedType) {
        Notification notification = new Notification();
        notification.setNotificationId(UUID.randomUUID().toString());
        notification.setRecipientId(recipientId);
        notification.setType(type);
        notification.setTitle(normalizeRequired(title, "Title is required"));
        notification.setMessage(normalizeRequired(message, "Message is required"));
        notification.setChannel(channel);
        notification.setRelatedId(blankToNull(relatedId));
        notification.setRelatedType(blankToNull(relatedType));
        notification.setRead(false);
        notification.setSentAt(Instant.now(clock));
        return notification;
    }

    private NotificationSchedule buildSchedule(
            String recipientId,
            NotificationType type,
            String title,
            String message,
            NotificationChannel channel,
            String relatedId,
            String relatedType,
            Instant triggerAt) {
        NotificationSchedule schedule = new NotificationSchedule();
        schedule.setScheduleId(UUID.randomUUID().toString());
        schedule.setRecipientId(recipientId);
        schedule.setType(type);
        schedule.setTitle(normalizeRequired(title, "Title is required"));
        schedule.setMessage(normalizeRequired(message, "Message is required"));
        schedule.setChannel(channel);
        schedule.setRelatedId(blankToNull(relatedId));
        schedule.setRelatedType(blankToNull(relatedType));
        schedule.setTriggerAt(triggerAt);
        schedule.setProcessed(false);
        return schedule;
    }

    private void dispatchByChannel(Notification notification) {
        AuthUserSummary recipient = authServiceGateway.getUserById(notification.getRecipientId());
        dispatchByChannel(notification, recipient);
    }

    private void dispatchByChannel(Notification notification, AuthUserSummary recipient) {
        switch (notification.getChannel()) {
            case APP -> {
                // In-app notifications are persisted only.
            }
            case EMAIL -> sendEmail(notification, recipient);
            case SMS -> sendSMS(notification, recipient);
        }
    }

    private void sendEmail(Notification notification, AuthUserSummary recipient) {
        emailSender.send(recipient.email(), notification.getTitle(), notification.getMessage());
    }

    private void sendSMS(Notification notification, AuthUserSummary recipient) {
        smsSender.send(recipient.phone(), notification.getMessage());
    }

    private List<AuthUserSummary> resolveRecipients(BroadcastAudience audience) {
        Role role = switch (audience) {
            case ALL -> null;
            case PATIENT -> Role.PATIENT;
            case PROVIDER -> Role.PROVIDER;
        };
        return authServiceGateway.getUsers(role).stream()
                .filter(AuthUserSummary::active)
                .toList();
    }

    private String buildFollowUpMessage(LocalDate followUpDate, String diagnosis, String prescription) {
        StringBuilder builder = new StringBuilder("Your follow-up is scheduled for ")
                .append(followUpDate.format(FOLLOW_UP_DATE_FORMAT))
                .append('.');
        if (StringUtils.hasText(diagnosis)) {
            builder.append(" Diagnosis: ").append(diagnosis.trim()).append('.');
        }
        if (StringUtils.hasText(prescription)) {
            builder.append(" Prescription: ").append(prescription.trim()).append('.');
        }
        return builder.toString();
    }

    private List<NotificationChannel> distinctChannels(List<NotificationChannel> channels) {
        return new LinkedHashSet<>(channels).stream().toList();
    }

    private long cancelPendingSchedules(String appointmentId) {
        return notificationScheduleRepository.deleteByRelatedIdAndRelatedTypeAndProcessedFalse(appointmentId, "APPOINTMENT");
    }

    private record NotificationDispatch(Notification notification, AuthUserSummary recipient) {
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getNotificationId(),
                notification.getRecipientId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getChannel(),
                notification.getRelatedId(),
                notification.getRelatedType(),
                notification.isRead(),
                notification.getSentAt());
    }

    private void assertCanAccessRecipient(String recipientId, AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null) {
            throw new AccessDeniedException("Authentication is required");
        }
        if (authenticatedUser.role() == Role.ADMIN) {
            return;
        }
        if (recipientId.equals(authenticatedUser.userId())) {
            return;
        }
        throw new AccessDeniedException("You can only access your own notifications");
    }

    private void assertAdmin(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null || authenticatedUser.role() != Role.ADMIN) {
            throw new AccessDeniedException("Only admins can perform this action");
        }
    }

    private String normalizeRequired(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
