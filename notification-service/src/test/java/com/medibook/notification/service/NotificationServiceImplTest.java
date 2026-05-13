package com.medibook.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.medibook.notification.config.AppProperties;
import com.medibook.notification.dto.request.FollowUpReminderRequest;
import com.medibook.notification.dto.request.ScheduleAppointmentRemindersRequest;
import com.medibook.notification.dto.request.SendBulkNotificationRequest;
import com.medibook.notification.dto.request.SendNotificationRequest;
import com.medibook.notification.dto.response.BulkDispatchResponse;
import com.medibook.notification.dto.response.NotificationResponse;
import com.medibook.notification.dto.response.ScheduledReminderResponse;
import com.medibook.notification.dto.response.UnreadCountResponse;
import com.medibook.notification.entity.Notification;
import com.medibook.notification.entity.NotificationSchedule;
import com.medibook.notification.enums.BroadcastAudience;
import com.medibook.notification.enums.NotificationChannel;
import com.medibook.notification.enums.NotificationType;
import com.medibook.notification.enums.Role;
import com.medibook.notification.repository.NotificationRepository;
import com.medibook.notification.repository.NotificationScheduleRepository;
import com.medibook.notification.security.AuthenticatedUser;
import com.medibook.notification.service.impl.NotificationServiceImpl;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-04-23T12:00:00Z"), ZoneOffset.UTC);

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationScheduleRepository notificationScheduleRepository;

    @Mock
    private AuthServiceGateway authServiceGateway;

    @Mock
    private EmailSender emailSender;

    @Mock
    private SmsSender smsSender;

    private AppProperties appProperties;

    private NotificationServiceImpl notificationService;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        notificationService = new NotificationServiceImpl(
                notificationRepository,
                notificationScheduleRepository,
                authServiceGateway,
                appProperties,
                emailSender,
                smsSender,
                FIXED_CLOCK);
    }

    @Test
    void shouldSendOneNotificationPerChannel() {
        when(authServiceGateway.getUserById("patient-1"))
                .thenReturn(new AuthUserSummary("patient-1", "Asha", "asha@medibook.com", "+911111111111", Role.PATIENT, true, null));
        when(notificationRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<NotificationResponse> responses = notificationService.send(new SendNotificationRequest(
                "patient-1",
                NotificationType.BOOKING,
                "Booking confirmed",
                "Your appointment is confirmed",
                List.of(NotificationChannel.APP, NotificationChannel.EMAIL, NotificationChannel.SMS),
                "appointment-1",
                "APPOINTMENT"));

        assertThat(responses).hasSize(3);
        assertThat(responses).extracting(NotificationResponse::channel)
                .containsExactly(NotificationChannel.APP, NotificationChannel.EMAIL, NotificationChannel.SMS);
        verify(emailSender).send("asha@medibook.com", "Booking confirmed", "Your appointment is confirmed");
        verify(smsSender).send("+911111111111", "Your appointment is confirmed");
    }

    @Test
    void shouldCreateFollowUpReminderAcrossAllChannels() {
        when(authServiceGateway.getUserById("patient-1"))
                .thenReturn(new AuthUserSummary("patient-1", "Asha", "asha@medibook.com", "+911111111111", Role.PATIENT, true, null));
        when(notificationRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        notificationService.sendFollowUpReminder(new FollowUpReminderRequest(
                "patient-1",
                "record-1",
                "appointment-1",
                "provider-1",
                LocalDate.of(2026, 4, 25),
                "Migraine",
                "Rest and medication"));

        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(3);
        assertThat(captor.getValue()).allMatch(notification -> notification.getType() == NotificationType.FOLLOWUP);
    }

    @Test
    void shouldScheduleAppointmentReminderNotifications() {
        when(authServiceGateway.getUserById("patient-1"))
                .thenReturn(new AuthUserSummary("patient-1", "Asha", "asha@medibook.com", "+911111111111", Role.PATIENT, true, null));
        when(notificationScheduleRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ScheduledReminderResponse response = notificationService.scheduleAppointmentReminders(
                new ScheduleAppointmentRemindersRequest(
                        "patient-1",
                        "appointment-1",
                        "provider-1",
                        "Appointment reminder",
                        "You have an appointment tomorrow",
                        List.of(NotificationChannel.APP, NotificationChannel.EMAIL),
                        Instant.parse("2026-04-24T12:00:00Z"),
                        Instant.parse("2026-04-25T11:00:00Z")));

        assertThat(response.appointmentId()).isEqualTo("appointment-1");
        assertThat(response.scheduledCount()).isEqualTo(4);
    }

    @Test
    void shouldDispatchDueScheduledReminders() {
        NotificationSchedule appSchedule = buildSchedule("schedule-1", NotificationChannel.APP);
        NotificationSchedule smsSchedule = buildSchedule("schedule-2", NotificationChannel.SMS);
        when(notificationScheduleRepository.findByProcessedFalseAndTriggerAtLessThanEqualOrderByTriggerAtAsc(
                Instant.parse("2026-04-23T12:00:00Z")))
                .thenReturn(List.of(appSchedule, smsSchedule));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(authServiceGateway.getUserById("patient-1"))
                .thenReturn(new AuthUserSummary("patient-1", "Asha", "asha@medibook.com", "+911111111111", Role.PATIENT, true, null));

        notificationService.dispatchDueScheduledReminders();

        assertThat(appSchedule.isProcessed()).isTrue();
        assertThat(smsSchedule.isProcessed()).isTrue();
        verify(notificationRepository, org.mockito.Mockito.times(2)).save(any(Notification.class));
        verify(smsSender).send("+911111111111", "Reminder message");
    }

    @Test
    void shouldSendBulkToActivePatientsOnly() {
        when(authServiceGateway.getUsers(Role.PATIENT)).thenReturn(List.of(
                new AuthUserSummary("patient-1", "Asha", "asha@medibook.com", null, Role.PATIENT, true, null),
                new AuthUserSummary("patient-2", "Riya", "riya@medibook.com", null, Role.PATIENT, false, null)));
        when(notificationRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        BulkDispatchResponse response = notificationService.sendBulk(
                new SendBulkNotificationRequest(
                        BroadcastAudience.PATIENT,
                        NotificationType.BROADCAST,
                        "System message",
                        "Clinic timing updated",
                        List.of(NotificationChannel.APP, NotificationChannel.EMAIL),
                        null,
                        "SYSTEM"),
                new AuthenticatedUser("admin-1", "admin@medibook.com", Role.ADMIN));

        assertThat(response.recipientCount()).isEqualTo(1);
        assertThat(response.notificationCount()).isEqualTo(2);
    }

    @Test
    void shouldOnlyAllowOwnerToReadNotification() {
        Notification notification = buildNotification();
        when(notificationRepository.findByNotificationId("notification-1")).thenReturn(Optional.of(notification));

        assertThatThrownBy(() -> notificationService.markAsRead(
                        "notification-1",
                        new AuthenticatedUser("patient-2", "other@medibook.com", Role.PATIENT)))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    @Test
    void shouldMarkNotificationAsRead() {
        Notification notification = buildNotification();
        when(notificationRepository.findByNotificationId("notification-1")).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationResponse response = notificationService.markAsRead(
                "notification-1",
                new AuthenticatedUser("patient-1", "patient@medibook.com", Role.PATIENT));

        assertThat(response.read()).isTrue();
    }

    @Test
    void shouldReturnUnreadCountForRecipient() {
        when(notificationRepository.countByRecipientIdAndRead("patient-1", false)).thenReturn(4L);

        UnreadCountResponse response = notificationService.getUnreadCount(
                "patient-1",
                new AuthenticatedUser("patient-1", "patient@medibook.com", Role.PATIENT));

        assertThat(response.unreadCount()).isEqualTo(4L);
    }

    private Notification buildNotification() {
        Notification notification = new Notification();
        notification.setNotificationId("notification-1");
        notification.setRecipientId("patient-1");
        notification.setType(NotificationType.REMINDER);
        notification.setTitle("Reminder");
        notification.setMessage("Upcoming appointment");
        notification.setChannel(NotificationChannel.APP);
        notification.setRelatedId("appointment-1");
        notification.setRelatedType("APPOINTMENT");
        notification.setRead(false);
        notification.setSentAt(Instant.parse("2026-04-23T12:00:00Z"));
        return notification;
    }

    private NotificationSchedule buildSchedule(String scheduleId, NotificationChannel channel) {
        NotificationSchedule schedule = new NotificationSchedule();
        schedule.setScheduleId(scheduleId);
        schedule.setRecipientId("patient-1");
        schedule.setType(NotificationType.REMINDER);
        schedule.setTitle("Reminder");
        schedule.setMessage("Reminder message");
        schedule.setChannel(channel);
        schedule.setRelatedId("appointment-1");
        schedule.setRelatedType("APPOINTMENT");
        schedule.setTriggerAt(Instant.parse("2026-04-23T11:59:00Z"));
        schedule.setProcessed(false);
        return schedule;
    }
}
