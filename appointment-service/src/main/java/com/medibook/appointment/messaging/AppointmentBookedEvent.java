package com.medibook.appointment.messaging;

import com.medibook.appointment.enums.ConsultationMode;
import java.time.LocalDate;
import java.time.LocalTime;

public record AppointmentBookedEvent(
        String appointmentId,
        String patientId,
        String providerId,
        String serviceType,
        ConsultationMode consultationMode,
        LocalDate appointmentDate,
        LocalTime startTime,
        LocalTime endTime) {
}
