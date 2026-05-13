package com.medibook.notification.controller;

import com.medibook.notification.dto.request.FollowUpReminderRequest;
import com.medibook.notification.dto.request.SendAppointmentBookedNotificationRequest;
import com.medibook.notification.dto.request.SendAppointmentCancelledNotificationRequest;
import com.medibook.notification.dto.request.SendAppointmentRescheduledNotificationRequest;
import com.medibook.notification.dto.request.SendDirectEmailRequest;
import com.medibook.notification.dto.request.ScheduleAppointmentRemindersRequest;
import com.medibook.notification.dto.request.SendNotificationRequest;
import com.medibook.notification.dto.response.MessageResponse;
import com.medibook.notification.dto.response.NotificationResponse;
import com.medibook.notification.dto.response.ScheduledReminderResponse;
import com.medibook.notification.service.NotificationService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications/internal")
public class InternalNotificationController {

    private final NotificationService notificationService;

    public InternalNotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/send")
    public ResponseEntity<List<NotificationResponse>> send(@Valid @RequestBody SendNotificationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(notificationService.send(request));
    }

    @PostMapping("/email")
    public ResponseEntity<MessageResponse> sendDirectEmail(@Valid @RequestBody SendDirectEmailRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(notificationService.sendDirectEmail(request));
    }

    @PostMapping("/appointments/booked")
    public ResponseEntity<Void> sendAppointmentBookedNotifications(
            @Valid @RequestBody SendAppointmentBookedNotificationRequest request) {
        notificationService.sendAppointmentBookedNotifications(request);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/appointments/cancelled")
    public ResponseEntity<Void> sendAppointmentCancelledNotifications(
            @Valid @RequestBody SendAppointmentCancelledNotificationRequest request) {
        notificationService.sendAppointmentCancelledNotifications(request);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/appointments/rescheduled")
    public ResponseEntity<Void> sendAppointmentRescheduledNotifications(
            @Valid @RequestBody SendAppointmentRescheduledNotificationRequest request) {
        notificationService.sendAppointmentRescheduledNotifications(request);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/follow-up-reminders")
    public ResponseEntity<Void> sendFollowUpReminder(@Valid @RequestBody FollowUpReminderRequest request) {
        notificationService.sendFollowUpReminder(request);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/appointment-reminders")
    public ResponseEntity<ScheduledReminderResponse> scheduleAppointmentReminders(
            @Valid @RequestBody ScheduleAppointmentRemindersRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(notificationService.scheduleAppointmentReminders(request));
    }

    @DeleteMapping("/appointment-reminders/{appointmentId}")
    public ResponseEntity<MessageResponse> cancelAppointmentReminders(@PathVariable String appointmentId) {
        return ResponseEntity.ok(notificationService.cancelScheduledAppointmentReminders(appointmentId));
    }
}
