package com.medibook.appointment.dto.response;

import com.medibook.appointment.enums.AppointmentStatus;
import com.medibook.appointment.enums.ConsultationMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public record AppointmentResponse(
        String appointmentId,
        String patientId,
        String providerId,
        String slotId,
        String serviceType,
        LocalDate appointmentDate,
        LocalTime startTime,
        LocalTime endTime,
        AppointmentStatus status,
        String notes,
        ConsultationMode modeOfConsultation,
        String cancellationReason,
        Instant createdAt,
        Instant updatedAt,
        Instant cancelledAt,
        Instant completedAt) {
}
