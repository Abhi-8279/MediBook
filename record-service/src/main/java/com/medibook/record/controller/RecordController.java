package com.medibook.record.controller;

import com.medibook.record.dto.request.AttachDocumentRequest;
import com.medibook.record.dto.request.CreateMedicalRecordRequest;
import com.medibook.record.dto.request.UpdateMedicalRecordRequest;
import com.medibook.record.dto.response.MedicalRecordResponse;
import com.medibook.record.dto.response.MessageResponse;
import com.medibook.record.dto.response.RecordCountResponse;
import com.medibook.record.security.AuthenticatedUser;
import com.medibook.record.service.RecordService;
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
@RequestMapping("/api/v1/records")
public class RecordController {

    private final RecordService recordService;

    public RecordController(RecordService recordService) {
        this.recordService = recordService;
    }

    @PostMapping
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<MedicalRecordResponse> createRecord(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody CreateMedicalRecordRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(recordService.createRecord(authenticatedUser, request));
    }

    @GetMapping("/{recordId}")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<MedicalRecordResponse> getRecordById(
            @PathVariable String recordId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return ResponseEntity.ok(recordService.getRecordById(recordId, authenticatedUser));
    }

    @GetMapping("/appointments/{appointmentId}")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<MedicalRecordResponse> getRecordByAppointment(
            @PathVariable String appointmentId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return ResponseEntity.ok(recordService.getRecordByAppointment(appointmentId, authenticatedUser));
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<List<MedicalRecordResponse>> getMyRecords(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return ResponseEntity.ok(recordService.getMyRecords(authenticatedUser));
    }

    @GetMapping("/patients/{patientId}")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('PATIENT','ADMIN')")
    public ResponseEntity<List<MedicalRecordResponse>> getRecordsByPatient(
            @PathVariable String patientId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return ResponseEntity.ok(recordService.getRecordsByPatient(patientId, authenticatedUser));
    }

    @GetMapping("/patients/{patientId}/count")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('PATIENT','ADMIN')")
    public ResponseEntity<RecordCountResponse> getRecordCount(
            @PathVariable String patientId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return ResponseEntity.ok(recordService.getRecordCount(patientId, authenticatedUser));
    }

    @GetMapping("/providers/me")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<List<MedicalRecordResponse>> getMyProviderRecords(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return ResponseEntity.ok(recordService.getMyProviderRecords(authenticatedUser));
    }

    @GetMapping("/providers/{providerId}")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('PROVIDER','ADMIN')")
    public ResponseEntity<List<MedicalRecordResponse>> getRecordsByProvider(
            @PathVariable String providerId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return ResponseEntity.ok(recordService.getRecordsByProvider(providerId, authenticatedUser));
    }

    @GetMapping("/follow-ups")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('PATIENT','PROVIDER','ADMIN')")
    public ResponseEntity<List<MedicalRecordResponse>> getFollowUpRecords(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return ResponseEntity.ok(recordService.getFollowUpRecords(date, authenticatedUser));
    }

    @PutMapping("/{recordId}")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<MedicalRecordResponse> updateRecord(
            @PathVariable String recordId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody UpdateMedicalRecordRequest request) {
        return ResponseEntity.ok(recordService.updateRecord(recordId, authenticatedUser, request));
    }

    @PutMapping("/{recordId}/attachment")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<MedicalRecordResponse> attachDocument(
            @PathVariable String recordId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody AttachDocumentRequest request) {
        return ResponseEntity.ok(recordService.attachDocument(recordId, authenticatedUser, request));
    }

    @DeleteMapping("/{recordId}")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<MessageResponse> deleteRecord(
            @PathVariable String recordId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return ResponseEntity.ok(recordService.deleteRecord(recordId, authenticatedUser));
    }
}
