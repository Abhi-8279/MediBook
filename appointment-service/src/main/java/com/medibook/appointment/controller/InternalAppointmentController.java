package com.medibook.appointment.controller;

import com.medibook.appointment.dto.response.AppointmentCountResponse;
import com.medibook.appointment.dto.response.AppointmentResponse;
import com.medibook.appointment.dto.response.CompletedAppointmentCheckResponse;
import com.medibook.appointment.service.AppointmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/appointments/internal")
public class InternalAppointmentController {

    private final AppointmentService appointmentService;

    public InternalAppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping("/{appointmentId}")
    public ResponseEntity<AppointmentResponse> getAppointmentById(@PathVariable String appointmentId) {
        return ResponseEntity.ok(appointmentService.getAppointmentByIdInternally(appointmentId));
    }

    @GetMapping("/providers/{providerId}/count")
    public ResponseEntity<AppointmentCountResponse> getAppointmentCountByProviderId(@PathVariable String providerId) {
        return ResponseEntity.ok(appointmentService.getAppointmentCountByProviderIdInternally(providerId));
    }

    @GetMapping("/patients/{patientId}/providers/{providerId}/completed/exists")
    public ResponseEntity<CompletedAppointmentCheckResponse> hasCompletedAppointment(
            @PathVariable String patientId,
            @PathVariable String providerId) {
        return ResponseEntity.ok(appointmentService.hasCompletedAppointment(patientId, providerId));
    }
}
