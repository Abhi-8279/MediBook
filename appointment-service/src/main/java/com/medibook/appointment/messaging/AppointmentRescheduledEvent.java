package com.medibook.appointment.messaging;

import java.time.LocalDate;
import java.time.LocalTime;

public record AppointmentRescheduledEvent(
        String appointmentId,
        String patientId,
        String providerId,
        LocalDate previousAppointmentDate,
        LocalTime previousStartTime,
        LocalTime previousEndTime,
        LocalDate appointmentDate,
        LocalTime startTime,
        LocalTime endTime,
        String serviceType) {
}
