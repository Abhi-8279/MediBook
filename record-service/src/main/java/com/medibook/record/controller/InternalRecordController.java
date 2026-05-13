package com.medibook.record.controller;

import com.medibook.record.dto.response.MedicalRecordResponse;
import com.medibook.record.dto.response.RecordCountResponse;
import com.medibook.record.service.RecordService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/records/internal")
public class InternalRecordController {

    private final RecordService recordService;

    public InternalRecordController(RecordService recordService) {
        this.recordService = recordService;
    }

    @GetMapping("/appointments/{appointmentId}")
    public ResponseEntity<MedicalRecordResponse> getRecordByAppointment(@PathVariable String appointmentId) {
        return ResponseEntity.ok(recordService.getRecordByAppointmentInternally(appointmentId));
    }

    @GetMapping("/follow-ups")
    public ResponseEntity<List<MedicalRecordResponse>> getFollowUpRecords(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date) {
        return ResponseEntity.ok(recordService.getFollowUpRecordsInternally(date));
    }

    @GetMapping("/patients/{patientId}/count")
    public ResponseEntity<RecordCountResponse> getRecordCount(@PathVariable String patientId) {
        return ResponseEntity.ok(recordService.getRecordCountInternally(patientId));
    }
}
