package com.medibook.schedule.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record AddBulkSlotsRequest(
        @NotEmpty(message = "At least one slot is required")
        List<@Valid AddSlotRequest> slots) {
}
