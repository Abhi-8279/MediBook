package com.medibook.schedule.controller;

import com.medibook.schedule.dto.request.AddBulkSlotsRequest;
import com.medibook.schedule.dto.request.AddSlotRequest;
import com.medibook.schedule.dto.request.BlockSlotRequest;
import com.medibook.schedule.dto.request.GenerateRecurringSlotsRequest;
import com.medibook.schedule.dto.request.UpdateSlotRequest;
import com.medibook.schedule.dto.response.AvailabilitySlotResponse;
import com.medibook.schedule.dto.response.MessageResponse;
import com.medibook.schedule.security.AuthenticatedUser;
import com.medibook.schedule.service.ScheduleService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/schedules")
public class ScheduleController {

    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @PostMapping("/slots")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<AvailabilitySlotResponse> addSlot(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody AddSlotRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(scheduleService.addSlot(authenticatedUser, request));
    }

    @PostMapping("/slots/bulk")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<List<AvailabilitySlotResponse>> addBulkSlots(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody AddBulkSlotsRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(scheduleService.addBulkSlots(authenticatedUser, request));
    }

    @PostMapping("/slots/recurring")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<List<AvailabilitySlotResponse>> generateRecurringSlots(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody GenerateRecurringSlotsRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(scheduleService.generateRecurringSlots(authenticatedUser, request));
    }

    @GetMapping("/slots/{slotId}")
    public ResponseEntity<AvailabilitySlotResponse> getSlotById(
            @PathVariable String slotId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return ResponseEntity.ok(scheduleService.getSlotById(slotId, authenticatedUser));
    }

    @GetMapping("/providers/{providerId}/slots")
    public ResponseEntity<List<AvailabilitySlotResponse>> getProviderSlots(
            @PathVariable String providerId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dateFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dateTo,
            @RequestParam(required = false) Boolean includeBooked,
            @RequestParam(required = false) Boolean includeBlocked,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return ResponseEntity.ok(scheduleService.getProviderSlots(
                providerId,
                date,
                dateFrom,
                dateTo,
                includeBooked,
                includeBlocked,
                authenticatedUser));
    }

    @GetMapping("/me/slots")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<List<AvailabilitySlotResponse>> getMySlots(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dateFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dateTo) {
        return ResponseEntity.ok(scheduleService.getMySlots(authenticatedUser, date, dateFrom, dateTo));
    }

    @PutMapping("/slots/{slotId}")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('PROVIDER','ADMIN')")
    public ResponseEntity<AvailabilitySlotResponse> updateSlot(
            @PathVariable String slotId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody UpdateSlotRequest request) {
        return ResponseEntity.ok(scheduleService.updateSlot(slotId, authenticatedUser, request));
    }

    @PutMapping("/slots/{slotId}/block")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('PROVIDER','ADMIN')")
    public ResponseEntity<AvailabilitySlotResponse> blockSlot(
            @PathVariable String slotId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestBody(required = false) BlockSlotRequest request) {
        BlockSlotRequest effectiveRequest = request == null ? new BlockSlotRequest(null) : request;
        return ResponseEntity.ok(scheduleService.blockSlot(slotId, authenticatedUser, effectiveRequest));
    }

    @PutMapping("/slots/{slotId}/unblock")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('PROVIDER','ADMIN')")
    public ResponseEntity<AvailabilitySlotResponse> unblockSlot(
            @PathVariable String slotId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return ResponseEntity.ok(scheduleService.unblockSlot(slotId, authenticatedUser));
    }

    @DeleteMapping("/slots/{slotId}")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('PROVIDER','ADMIN')")
    public ResponseEntity<MessageResponse> deleteSlot(
            @PathVariable String slotId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        scheduleService.deleteSlot(slotId, authenticatedUser);
        return ResponseEntity.ok(new MessageResponse("Slot deleted successfully"));
    }
}
