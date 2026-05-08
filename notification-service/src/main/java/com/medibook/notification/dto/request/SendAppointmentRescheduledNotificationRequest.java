package com.medibook.notification.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

public record SendAppointmentRescheduledNotificationRequest(
        @NotBlank(message = "Appointment ID is required")
        String appointmentId,

        @NotBlank(message = "Patient ID is required")
        String patientId,

        @NotBlank(message = "Provider ID is required")
        String providerId,

        @NotBlank(message = "Service type is required")
        @Size(max = 120, message = "Service type must be at most 120 characters")
        String serviceType,

        @NotNull(message = "Previous appointment date is required")
        LocalDate previousAppointmentDate,

        @NotNull(message = "Previous start time is required")
        LocalTime previousStartTime,

        @NotNull(message = "Previous end time is required")
        LocalTime previousEndTime,

        @NotNull(message = "New appointment date is required")
        LocalDate appointmentDate,

        @NotNull(message = "New start time is required")
        LocalTime startTime,

        @NotNull(message = "New end time is required")
        LocalTime endTime) {
}
