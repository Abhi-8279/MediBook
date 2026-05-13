package com.medibook.appointment.controller;

import com.medibook.appointment.dto.request.BookAppointmentRequest;
import com.medibook.appointment.dto.request.CancelAppointmentRequest;
import com.medibook.appointment.dto.request.CompleteAppointmentRequest;
import com.medibook.appointment.dto.request.RescheduleAppointmentRequest;
import com.medibook.appointment.dto.request.UpdateAppointmentStatusRequest;
import com.medibook.appointment.dto.response.AppointmentCountResponse;
import com.medibook.appointment.dto.response.AppointmentResponse;
import com.medibook.appointment.security.AuthenticatedUser;
import com.medibook.appointment.service.AppointmentService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<AppointmentResponse> bookAppointment(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody BookAppointmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(appointmentService.bookAppointment(authenticatedUser, request));
    }

    @GetMapping("/{appointmentId}")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<AppointmentResponse> getAppointmentById(
            @PathVariable String appointmentId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return ResponseEntity.ok(appointmentService.getAppointmentById(appointmentId, authenticatedUser));
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<List<AppointmentResponse>> getMyAppointments(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return ResponseEntity.ok(appointmentService.getMyAppointments(authenticatedUser));
    }

    @GetMapping("/me/upcoming")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<List<AppointmentResponse>> getMyUpcomingAppointments(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return ResponseEntity.ok(appointmentService.getMyUpcomingAppointments(authenticatedUser));
    }

    @GetMapping("/provider/me")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<List<AppointmentResponse>> getMyProviderAppointments(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return ResponseEntity.ok(appointmentService.getMyProviderAppointments(authenticatedUser));
    }

    @GetMapping("/provider/me/upcoming")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<List<AppointmentResponse>> getMyUpcomingProviderAppointments(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return ResponseEntity.ok(appointmentService.getMyUpcomingProviderAppointments(authenticatedUser));
    }

    @GetMapping("/provider/me/today")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<List<AppointmentResponse>> getMyProviderAppointmentsToday(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return ResponseEntity.ok(appointmentService.getMyProviderAppointmentsForDate(
                authenticatedUser,
                LocalDate.now()));
    }

    @GetMapping("/provider/me/date")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<List<AppointmentResponse>> getMyProviderAppointmentsForDate(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date) {
        return ResponseEntity.ok(appointmentService.getMyProviderAppointmentsForDate(authenticatedUser, date));
    }

    @GetMapping("/admin")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AppointmentResponse>> getAdminAppointments(
            @RequestParam(required = false) com.medibook.appointment.enums.AppointmentStatus status,
            @RequestParam(required = false) String patientId,
            @RequestParam(required = false) String providerId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date) {
        return ResponseEntity.ok(appointmentService.getAdminAppointments(status, patientId, providerId, date));
    }

    @GetMapping("/providers/{providerId}")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('PROVIDER','ADMIN')")
    public ResponseEntity<List<AppointmentResponse>> getAppointmentsByProviderId(
            @PathVariable String providerId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return ResponseEntity.ok(appointmentService.getAppointmentsByProviderId(providerId, null, authenticatedUser));
    }

    @GetMapping("/providers/{providerId}/date/{date}")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('PROVIDER','ADMIN')")
    public ResponseEntity<List<AppointmentResponse>> getAppointmentsByProviderIdAndDate(
            @PathVariable String providerId,
            @PathVariable
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return ResponseEntity.ok(appointmentService.getAppointmentsByProviderId(providerId, date, authenticatedUser));
    }

    @GetMapping("/providers/{providerId}/count")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('PROVIDER','ADMIN')")
    public ResponseEntity<AppointmentCountResponse> getAppointmentCountByProviderId(
            @PathVariable String providerId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return ResponseEntity.ok(appointmentService.getAppointmentCountByProviderId(providerId, authenticatedUser));
    }

    @PutMapping("/{appointmentId}/cancel")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('PATIENT','PROVIDER','ADMIN')")
    public ResponseEntity<AppointmentResponse> cancelAppointment(
            @PathVariable String appointmentId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestBody(required = false) CancelAppointmentRequest request) {
        CancelAppointmentRequest effectiveRequest = request == null ? new CancelAppointmentRequest(null) : request;
        return ResponseEntity.ok(appointmentService.cancelAppointment(appointmentId, authenticatedUser, effectiveRequest));
    }

    @PutMapping("/{appointmentId}/reschedule")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<AppointmentResponse> rescheduleAppointment(
            @PathVariable String appointmentId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody RescheduleAppointmentRequest request) {
        return ResponseEntity.ok(appointmentService.rescheduleAppointment(appointmentId, authenticatedUser, request));
    }

    @PutMapping("/{appointmentId}/complete")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('PROVIDER','ADMIN')")
    public ResponseEntity<AppointmentResponse> completeAppointment(
            @PathVariable String appointmentId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestBody(required = false) CompleteAppointmentRequest request) {
        CompleteAppointmentRequest effectiveRequest = request == null ? new CompleteAppointmentRequest(null) : request;
        return ResponseEntity.ok(appointmentService.completeAppointment(appointmentId, authenticatedUser, effectiveRequest));
    }

    @PutMapping("/{appointmentId}/status")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('PROVIDER','ADMIN')")
    public ResponseEntity<AppointmentResponse> updateStatus(
            @PathVariable String appointmentId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody UpdateAppointmentStatusRequest request) {
        return ResponseEntity.ok(appointmentService.updateStatus(appointmentId, authenticatedUser, request));
    }
}
