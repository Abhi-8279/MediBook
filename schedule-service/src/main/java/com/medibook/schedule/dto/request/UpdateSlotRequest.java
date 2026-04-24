package com.medibook.schedule.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.time.LocalTime;

public record UpdateSlotRequest(
        @NotNull(message = "Slot date is required")
        LocalDate date,
        @NotNull(message = "Slot start time is required")
        LocalTime startTime,
        @NotNull(message = "Slot end time is required")
        LocalTime endTime,
        @NotNull(message = "Duration is required")
        @Positive(message = "Duration must be greater than zero")
        Integer durationMinutes) {
}
