package com.medibook.record.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.medibook.record.enums.AppointmentStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AppointmentSummary(
        String appointmentId,
        String patientId,
        String providerId,
        String serviceType,
        LocalDate appointmentDate,
        LocalTime startTime,
        LocalTime endTime,
        AppointmentStatus status,
        Instant completedAt) {
}
