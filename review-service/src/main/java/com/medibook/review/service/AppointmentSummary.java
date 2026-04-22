package com.medibook.review.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.medibook.review.enums.AppointmentStatus;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AppointmentSummary(
        String appointmentId,
        String patientId,
        String providerId,
        AppointmentStatus status) {
}
