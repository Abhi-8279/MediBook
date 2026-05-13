package com.medibook.appointment.messaging;

import java.time.LocalDate;
import java.time.LocalTime;

public record AppointmentCancelledEvent(
        String appointmentId,
        String patientId,
        String providerId,
        LocalDate appointmentDate,
        LocalTime startTime,
        LocalTime endTime,
        String cancellationReason) {
}
