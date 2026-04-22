package com.medibook.schedule.dto.response;

import com.medibook.schedule.enums.RecurrenceType;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public record AvailabilitySlotResponse(
        String slotId,
        String providerId,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        Integer durationMinutes,
        boolean booked,
        boolean blocked,
        RecurrenceType recurrenceType,
        String blockedReason,
        String bookingReference,
        Instant bookedAt,
        Instant createdAt,
        Instant updatedAt) {
}
