package com.medibook.record.service;

import com.medibook.record.dto.request.AttachDocumentRequest;
import com.medibook.record.dto.request.CreateMedicalRecordRequest;
import com.medibook.record.dto.request.UpdateMedicalRecordRequest;
import com.medibook.record.dto.response.MedicalRecordResponse;
import com.medibook.record.dto.response.MessageResponse;
import com.medibook.record.dto.response.RecordCountResponse;
import com.medibook.record.security.AuthenticatedUser;
import java.time.LocalDate;
import java.util.List;

public interface RecordService {

    MedicalRecordResponse createRecord(AuthenticatedUser authenticatedUser, CreateMedicalRecordRequest request);

    MedicalRecordResponse getRecordById(String recordId, AuthenticatedUser authenticatedUser);

    MedicalRecordResponse getRecordByAppointment(String appointmentId, AuthenticatedUser authenticatedUser);

    List<MedicalRecordResponse> getMyRecords(AuthenticatedUser authenticatedUser);

    List<MedicalRecordResponse> getRecordsByPatient(String patientId, AuthenticatedUser authenticatedUser);

    List<MedicalRecordResponse> getMyProviderRecords(AuthenticatedUser authenticatedUser);

    List<MedicalRecordResponse> getRecordsByProvider(String providerId, AuthenticatedUser authenticatedUser);

    List<MedicalRecordResponse> getFollowUpRecords(LocalDate date, AuthenticatedUser authenticatedUser);

    RecordCountResponse getRecordCount(String patientId, AuthenticatedUser authenticatedUser);

    MedicalRecordResponse updateRecord(
            String recordId,
            AuthenticatedUser authenticatedUser,
            UpdateMedicalRecordRequest request);

    MedicalRecordResponse attachDocument(
            String recordId,
            AuthenticatedUser authenticatedUser,
            AttachDocumentRequest request);

    MessageResponse deleteRecord(String recordId, AuthenticatedUser authenticatedUser);

    MedicalRecordResponse getRecordByAppointmentInternally(String appointmentId);

    List<MedicalRecordResponse> getFollowUpRecordsInternally(LocalDate date);

    RecordCountResponse getRecordCountInternally(String patientId);

    void dispatchDueFollowUpReminders();
}
