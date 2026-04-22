package com.medibook.schedule.dto.request;

import com.medibook.schedule.enums.RecurrenceType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

public record GenerateRecurringSlotsRequest(
        @NotNull(message = "Start date is required")
        LocalDate startDate,
        @NotNull(message = "End date is required")
        LocalDate endDate,
        @NotNull(message = "Slot start time is required")
        LocalTime startTime,
        @NotNull(message = "Slot end time is required")
        LocalTime endTime,
        @NotNull(message = "Duration is required")
        @Positive(message = "Duration must be greater than zero")
        Integer durationMinutes,
        @NotNull(message = "Recurrence type is required")
        RecurrenceType recurrenceType,
        Set<DayOfWeek> daysOfWeek,
        @Positive(message = "Interval days must be greater than zero")
        Integer intervalDays) {
}
