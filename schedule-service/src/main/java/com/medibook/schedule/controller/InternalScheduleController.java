package com.medibook.schedule.controller;

import com.medibook.schedule.dto.request.InternalSlotBookingRequest;
import com.medibook.schedule.dto.response.AvailabilitySlotResponse;
import com.medibook.schedule.service.ScheduleService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/v1/schedules/internal")
public class InternalScheduleController {

    private final ScheduleService scheduleService;

    public InternalScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @PostMapping("/slots/{slotId}/book")
    public ResponseEntity<AvailabilitySlotResponse> bookSlot(
            @PathVariable String slotId,
            @Valid @RequestBody InternalSlotBookingRequest request) {
        return ResponseEntity.ok(scheduleService.bookSlotInternally(slotId, request));
    }

    @PostMapping("/slots/{slotId}/release")
    public ResponseEntity<AvailabilitySlotResponse> releaseSlot(@PathVariable String slotId) {
        return ResponseEntity.ok(scheduleService.releaseSlotInternally(slotId));
    }

    @GetMapping("/slots/{slotId}")
    public ResponseEntity<AvailabilitySlotResponse> getSlotById(@PathVariable String slotId) {
        return ResponseEntity.ok(scheduleService.getSlotByIdInternally(slotId));
    }

    @GetMapping("/providers/{providerId}/slots/available")
    public ResponseEntity<List<AvailabilitySlotResponse>> getAvailableSlots(
            @PathVariable String providerId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dateFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dateTo) {
        return ResponseEntity.ok(scheduleService.getAvailableSlotsByProviderInternally(providerId, date, dateFrom, dateTo));
    }
}
