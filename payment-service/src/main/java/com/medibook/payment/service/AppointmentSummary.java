package com.medibook.payment.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.medibook.payment.enums.AppointmentStatus;
import java.time.LocalDate;
import java.time.LocalTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AppointmentSummary(
        String appointmentId,
        String patientId,
        String providerId,
        String slotId,
        String serviceType,
        LocalDate appointmentDate,
        LocalTime startTime,
        LocalTime endTime,
        AppointmentStatus status) {
}
