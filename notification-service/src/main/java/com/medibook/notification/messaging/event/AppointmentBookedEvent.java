package com.medibook.notification.messaging.event;

import com.medibook.notification.enums.NotificationChannel;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record AppointmentBookedEvent(
        String appointmentId,
        String patientId,
        String providerId,
        String serviceType,
        String consultationMode,
        LocalDate appointmentDate,
        LocalTime startTime,
        LocalTime endTime) {
}
