package com.medibook.appointment.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDate;
import java.time.LocalTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ScheduleSlotSummary(
        String slotId,
        String providerId,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        Integer durationMinutes,
        boolean booked,
        boolean blocked,
        String bookingReference) {
}
