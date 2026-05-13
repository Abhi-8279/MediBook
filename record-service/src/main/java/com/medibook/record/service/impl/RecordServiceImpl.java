package com.medibook.record.service.impl;

import com.medibook.record.config.AppProperties;
import com.medibook.record.dto.request.AttachDocumentRequest;
import com.medibook.record.dto.request.CreateMedicalRecordRequest;
import com.medibook.record.dto.request.UpdateMedicalRecordRequest;
import com.medibook.record.dto.response.MedicalRecordResponse;
import com.medibook.record.dto.response.MessageResponse;
import com.medibook.record.dto.response.RecordCountResponse;
import com.medibook.record.entity.MedicalRecord;
import com.medibook.record.enums.AppointmentStatus;
import com.medibook.record.enums.Role;
import com.medibook.record.exception.ExternalServiceException;
import com.medibook.record.messaging.NotificationEventPublisher;
import com.medibook.record.exception.RecordConflictException;
import com.medibook.record.exception.ResourceNotFoundException;
import com.medibook.record.repository.RecordRepository;
import com.medibook.record.security.AuthenticatedUser;
import com.medibook.record.service.AppointmentServiceGateway;
import com.medibook.record.service.AppointmentSummary;
import com.medibook.record.service.MedicalRecordReminderPayload;
import com.medibook.record.service.NotificationServiceGateway;
import com.medibook.record.service.ProviderServiceGateway;
import com.medibook.record.service.ProviderSummary;
import com.medibook.record.service.RecordService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class RecordServiceImpl implements RecordService {

    private final RecordRepository recordRepository;
    private final AppointmentServiceGateway appointmentServiceGateway;
    private final ProviderServiceGateway providerServiceGateway;
    private final NotificationServiceGateway notificationServiceGateway;
    private final NotificationEventPublisher notificationEventPublisher;
    private final AppProperties appProperties;
    private final Clock clock;

    public RecordServiceImpl(
            RecordRepository recordRepository,
            AppointmentServiceGateway appointmentServiceGateway,
            ProviderServiceGateway providerServiceGateway,
            NotificationServiceGateway notificationServiceGateway,
            NotificationEventPublisher notificationEventPublisher,
            AppProperties appProperties,
            Clock clock) {
        this.recordRepository = recordRepository;
        this.appointmentServiceGateway = appointmentServiceGateway;
        this.providerServiceGateway = providerServiceGateway;
        this.notificationServiceGateway = notificationServiceGateway;
        this.notificationEventPublisher = notificationEventPublisher;
        this.appProperties = appProperties;
        this.clock = clock;
    }

    @Override
    public MedicalRecordResponse createRecord(AuthenticatedUser authenticatedUser, CreateMedicalRecordRequest request) {
        String providerId = resolveCurrentProviderId(authenticatedUser);
        AppointmentSummary appointment = appointmentServiceGateway.getAppointmentById(request.appointmentId().trim());
        assertCompletedAppointment(appointment);
        if (!providerId.equals(appointment.providerId())) {
            throw new AccessDeniedException("You can only create records for your own completed appointments");
        }
        if (recordRepository.findByAppointmentId(appointment.appointmentId()).isPresent()) {
            throw new RecordConflictException("A medical record already exists for this appointment");
        }

        MedicalRecord record = new MedicalRecord();
        record.setRecordId(UUID.randomUUID().toString());
        record.setAppointmentId(appointment.appointmentId());
        record.setPatientId(appointment.patientId());
        record.setProviderId(appointment.providerId());
        record.setDiagnosis(request.diagnosis().trim());
        record.setPrescription(request.prescription().trim());
        record.setNotes(blankToNull(request.notes()));
        record.setAttachmentUrl(blankToNull(request.attachmentUrl()));
        record.setFollowUpDate(request.followUpDate());

        return toResponse(recordRepository.saveAndFlush(record));
    }

    @Override
    @Transactional(readOnly = true)
    public MedicalRecordResponse getRecordById(String recordId, AuthenticatedUser authenticatedUser) {
        MedicalRecord record = findRecordOrThrow(recordId);
        assertCanView(record, authenticatedUser);
        return toResponse(record);
    }

    @Override
    @Transactional(readOnly = true)
    public MedicalRecordResponse getRecordByAppointment(String appointmentId, AuthenticatedUser authenticatedUser) {
        MedicalRecord record = findRecordByAppointmentOrThrow(appointmentId);
        assertCanView(record, authenticatedUser);
        return toResponse(record);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MedicalRecordResponse> getMyRecords(AuthenticatedUser authenticatedUser) {
        assertPatient(authenticatedUser);
        return recordRepository.findByPatientIdOrderByCreatedAtDesc(authenticatedUser.userId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MedicalRecordResponse> getRecordsByPatient(String patientId, AuthenticatedUser authenticatedUser) {
        if (isAdmin(authenticatedUser)) {
            return mapResponses(recordRepository.findByPatientIdOrderByCreatedAtDesc(patientId));
        }
        if (isPatient(authenticatedUser) && authenticatedUser.userId().equals(patientId)) {
            return mapResponses(recordRepository.findByPatientIdOrderByCreatedAtDesc(patientId));
        }
        throw new AccessDeniedException("You can only access your own medical records");
    }

    @Override
    @Transactional(readOnly = true)
    public List<MedicalRecordResponse> getMyProviderRecords(AuthenticatedUser authenticatedUser) {
        String providerId = resolveCurrentProviderId(authenticatedUser);
        return mapResponses(recordRepository.findByProviderIdOrderByCreatedAtDesc(providerId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MedicalRecordResponse> getRecordsByProvider(String providerId, AuthenticatedUser authenticatedUser) {
        if (isAdmin(authenticatedUser)) {
            return mapResponses(recordRepository.findByProviderIdOrderByCreatedAtDesc(providerId));
        }
        String currentProviderId = resolveCurrentProviderId(authenticatedUser);
        if (!currentProviderId.equals(providerId)) {
            throw new AccessDeniedException("You can only access records you created");
        }
        return mapResponses(recordRepository.findByProviderIdOrderByCreatedAtDesc(providerId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MedicalRecordResponse> getFollowUpRecords(LocalDate date, AuthenticatedUser authenticatedUser) {
        LocalDate effectiveDate = date == null ? LocalDate.now(clock) : date;
        List<MedicalRecord> records = recordRepository.findByFollowUpDateOrderByCreatedAtAsc(effectiveDate);

        if (isAdmin(authenticatedUser)) {
            return mapResponses(records);
        }
        if (isProvider(authenticatedUser)) {
            String providerId = resolveCurrentProviderId(authenticatedUser);
            return mapResponses(records.stream()
                    .filter(record -> providerId.equals(record.getProviderId()))
                    .toList());
        }
        if (isPatient(authenticatedUser)) {
            return mapResponses(records.stream()
                    .filter(record -> authenticatedUser.userId().equals(record.getPatientId()))
                    .toList());
        }
        throw new AccessDeniedException("You are not allowed to access follow-up records");
    }

    @Override
    @Transactional(readOnly = true)
    public RecordCountResponse getRecordCount(String patientId, AuthenticatedUser authenticatedUser) {
        if (!isAdmin(authenticatedUser) && !(isPatient(authenticatedUser) && authenticatedUser.userId().equals(patientId))) {
            throw new AccessDeniedException("You can only access your own medical record count");
        }
        return new RecordCountResponse(patientId, recordRepository.countByPatientId(patientId));
    }

    @Override
    public MedicalRecordResponse updateRecord(
            String recordId,
            AuthenticatedUser authenticatedUser,
            UpdateMedicalRecordRequest request) {
        MedicalRecord record = findRecordOrThrow(recordId);
        assertCanEdit(record, authenticatedUser);
        LocalDate previousFollowUpDate = record.getFollowUpDate();

        record.setDiagnosis(request.diagnosis().trim());
        record.setPrescription(request.prescription().trim());
        record.setNotes(blankToNull(request.notes()));
        record.setFollowUpDate(request.followUpDate());
        if (request.followUpDate() == null) {
            record.setFollowUpReminderSentAt(null);
        } else if (!request.followUpDate().equals(previousFollowUpDate)) {
            record.setFollowUpReminderSentAt(null);
        }

        return toResponse(recordRepository.saveAndFlush(record));
    }

    @Override
    public MedicalRecordResponse attachDocument(
            String recordId,
            AuthenticatedUser authenticatedUser,
            AttachDocumentRequest request) {
        MedicalRecord record = findRecordOrThrow(recordId);
        assertCanEdit(record, authenticatedUser);
        record.setAttachmentUrl(request.attachmentUrl().trim());
        return toResponse(recordRepository.saveAndFlush(record));
    }

    @Override
    public MessageResponse deleteRecord(String recordId, AuthenticatedUser authenticatedUser) {
        MedicalRecord record = findRecordOrThrow(recordId);
        assertCanEdit(record, authenticatedUser);
        recordRepository.delete(record);
        return new MessageResponse("Medical record deleted successfully");
    }

    @Override
    @Transactional(readOnly = true)
    public MedicalRecordResponse getRecordByAppointmentInternally(String appointmentId) {
        return toResponse(findRecordByAppointmentOrThrow(appointmentId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MedicalRecordResponse> getFollowUpRecordsInternally(LocalDate date) {
        LocalDate effectiveDate = date == null ? LocalDate.now(clock) : date;
        return mapResponses(recordRepository.findByFollowUpDateOrderByCreatedAtAsc(effectiveDate));
    }

    @Override
    @Transactional(readOnly = true)
    public RecordCountResponse getRecordCountInternally(String patientId) {
        return new RecordCountResponse(patientId, recordRepository.countByPatientId(patientId));
    }

    @Override
    @Scheduled(fixedDelayString = "${app.record.follow-up-check-interval-ms:3600000}")
    public void dispatchDueFollowUpReminders() {
        LocalDate today = LocalDate.now(clock);
        List<MedicalRecord> dueRecords = recordRepository.findByFollowUpDateAndFollowUpReminderSentAtIsNullOrderByCreatedAtAsc(today);
        if (dueRecords.isEmpty()) {
            return;
        }

        for (MedicalRecord record : dueRecords) {
            MedicalRecordReminderPayload payload = new MedicalRecordReminderPayload(
                    record.getPatientId(),
                    record.getRecordId(),
                    record.getAppointmentId(),
                    record.getProviderId(),
                    record.getFollowUpDate(),
                    record.getDiagnosis(),
                    record.getPrescription());
            try {
                notificationEventPublisher.publishFollowUpReminder(payload);
                record.setFollowUpReminderSentAt(Instant.now(clock));
                recordRepository.save(record);
            } catch (RuntimeException publishException) {
                try {
                    notificationServiceGateway.sendFollowUpReminder(payload);
                    record.setFollowUpReminderSentAt(Instant.now(clock));
                    recordRepository.save(record);
                } catch (ExternalServiceException exception) {
                    // Keep the reminder eligible for a future retry if notification-service is unavailable.
                }
            }
        }
    }

    private MedicalRecord findRecordOrThrow(String recordId) {
        return recordRepository.findByRecordId(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("Medical record not found"));
    }

    private MedicalRecord findRecordByAppointmentOrThrow(String appointmentId) {
        return recordRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Medical record not found"));
    }

    private void assertCompletedAppointment(AppointmentSummary appointment) {
        if (appointment.status() != AppointmentStatus.COMPLETED) {
            throw new IllegalStateException("Medical records can only be created for completed appointments");
        }
    }

    private void assertCanView(MedicalRecord record, AuthenticatedUser authenticatedUser) {
        if (isAdmin(authenticatedUser)) {
            return;
        }
        if (isPatient(authenticatedUser) && record.getPatientId().equals(authenticatedUser.userId())) {
            return;
        }
        if (isProvider(authenticatedUser) && record.getProviderId().equals(resolveCurrentProviderId(authenticatedUser))) {
            return;
        }
        throw new AccessDeniedException("You are not allowed to access this medical record");
    }

    private void assertCanEdit(MedicalRecord record, AuthenticatedUser authenticatedUser) {
        String currentProviderId = resolveCurrentProviderId(authenticatedUser);
        if (!record.getProviderId().equals(currentProviderId)) {
            throw new AccessDeniedException("You can only modify records you created");
        }
        AppointmentSummary appointment = appointmentServiceGateway.getAppointmentById(record.getAppointmentId());
        assertCompletedAppointment(appointment);
        if (!isWithinEditWindow(appointment)) {
            throw new IllegalStateException("The allowed record editing window has expired");
        }
    }

    private boolean isWithinEditWindow(AppointmentSummary appointment) {
        LocalDateTime completionTime = appointment.completedAt() != null
                ? LocalDateTime.ofInstant(appointment.completedAt(), ZoneOffset.UTC)
                : LocalDateTime.of(appointment.appointmentDate(), appointment.endTime());
        LocalDateTime expiryTime = completionTime.plusHours(appProperties.getRecord().getEditWindowHours());
        return !LocalDateTime.now(clock).isAfter(expiryTime);
    }

    private String resolveCurrentProviderId(AuthenticatedUser authenticatedUser) {
        if (!isProvider(authenticatedUser)) {
            throw new AccessDeniedException("Only providers can access this resource");
        }
        ProviderSummary provider = providerServiceGateway.getProviderByUserId(authenticatedUser.userId());
        return provider.providerId();
    }

    private List<MedicalRecordResponse> mapResponses(List<MedicalRecord> records) {
        return records.stream()
                .map(this::toResponse)
                .toList();
    }

    private MedicalRecordResponse toResponse(MedicalRecord record) {
        return new MedicalRecordResponse(
                record.getRecordId(),
                record.getAppointmentId(),
                record.getPatientId(),
                record.getProviderId(),
                record.getDiagnosis(),
                record.getPrescription(),
                record.getNotes(),
                record.getAttachmentUrl(),
                record.getFollowUpDate(),
                record.getFollowUpReminderSentAt(),
                record.getCreatedAt(),
                record.getUpdatedAt());
    }

    private void assertPatient(AuthenticatedUser authenticatedUser) {
        if (!isPatient(authenticatedUser)) {
            throw new AccessDeniedException("Only patients can access this resource");
        }
    }

    private boolean isAdmin(AuthenticatedUser authenticatedUser) {
        return authenticatedUser != null && authenticatedUser.role() == Role.ADMIN;
    }

    private boolean isPatient(AuthenticatedUser authenticatedUser) {
        return authenticatedUser != null && authenticatedUser.role() == Role.PATIENT;
    }

    private boolean isProvider(AuthenticatedUser authenticatedUser) {
        return authenticatedUser != null && authenticatedUser.role() == Role.PROVIDER;
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
