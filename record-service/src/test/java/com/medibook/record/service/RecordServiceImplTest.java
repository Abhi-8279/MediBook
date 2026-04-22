package com.medibook.record.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.medibook.record.config.AppProperties;
import com.medibook.record.dto.request.CreateMedicalRecordRequest;
import com.medibook.record.dto.request.UpdateMedicalRecordRequest;
import com.medibook.record.dto.response.MedicalRecordResponse;
import com.medibook.record.entity.MedicalRecord;
import com.medibook.record.enums.AppointmentStatus;
import com.medibook.record.enums.Role;
import com.medibook.record.repository.RecordRepository;
import com.medibook.record.security.AuthenticatedUser;
import com.medibook.record.service.impl.RecordServiceImpl;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecordServiceImplTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-04-22T10:00:00Z"), ZoneOffset.UTC);

    @Mock
    private RecordRepository recordRepository;

    @Mock
    private AppointmentServiceGateway appointmentServiceGateway;

    @Mock
    private ProviderServiceGateway providerServiceGateway;

    @Mock
    private NotificationServiceGateway notificationServiceGateway;

    private RecordServiceImpl recordService;

    @BeforeEach
    void setUp() {
        AppProperties appProperties = new AppProperties();
        recordService = new RecordServiceImpl(
                recordRepository,
                appointmentServiceGateway,
                providerServiceGateway,
                notificationServiceGateway,
                appProperties,
                FIXED_CLOCK);
    }

    @Test
    void shouldCreateRecordForCompletedAppointmentOwnedByProvider() {
        AuthenticatedUser provider = new AuthenticatedUser("provider-user-1", "doctor@medibook.com", Role.PROVIDER);
        AppointmentSummary appointment = completedAppointment();

        when(providerServiceGateway.getProviderByUserId("provider-user-1"))
                .thenReturn(new ProviderSummary("provider-1", "provider-user-1", true, true));
        when(appointmentServiceGateway.getAppointmentById("appointment-1")).thenReturn(appointment);
        when(recordRepository.findByAppointmentId("appointment-1")).thenReturn(Optional.empty());
        when(recordRepository.saveAndFlush(any(MedicalRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MedicalRecordResponse response = recordService.createRecord(
                provider,
                new CreateMedicalRecordRequest(
                        "appointment-1",
                        "Seasonal viral fever",
                        "Paracetamol and rest",
                        "Hydration advised",
                        "https://example.com/report.pdf",
                        LocalDate.of(2026, 5, 2)));

        ArgumentCaptor<MedicalRecord> captor = ArgumentCaptor.forClass(MedicalRecord.class);
        verify(recordRepository).saveAndFlush(captor.capture());
        MedicalRecord saved = captor.getValue();

        assertThat(saved.getAppointmentId()).isEqualTo("appointment-1");
        assertThat(saved.getPatientId()).isEqualTo("patient-1");
        assertThat(saved.getProviderId()).isEqualTo("provider-1");
        assertThat(response.diagnosis()).isEqualTo("Seasonal viral fever");
    }

    @Test
    void shouldRejectCreateRecordForIncompleteAppointment() {
        AuthenticatedUser provider = new AuthenticatedUser("provider-user-1", "doctor@medibook.com", Role.PROVIDER);

        when(providerServiceGateway.getProviderByUserId("provider-user-1"))
                .thenReturn(new ProviderSummary("provider-1", "provider-user-1", true, true));
        when(appointmentServiceGateway.getAppointmentById("appointment-1"))
                .thenReturn(new AppointmentSummary(
                        "appointment-1",
                        "patient-1",
                        "provider-1",
                        "General Consultation",
                        LocalDate.of(2026, 4, 22),
                        LocalTime.of(9, 0),
                        LocalTime.of(9, 30),
                        AppointmentStatus.SCHEDULED,
                        null));

        assertThatThrownBy(() -> recordService.createRecord(
                        provider,
                        new CreateMedicalRecordRequest(
                                "appointment-1",
                                "Diagnosis",
                                "Prescription",
                                "Notes",
                                null,
                                null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("completed appointments");
    }

    @Test
    void shouldAllowPatientToReadOwnRecord() {
        MedicalRecord record = record();
        AuthenticatedUser patient = new AuthenticatedUser("patient-1", "patient@medibook.com", Role.PATIENT);

        when(recordRepository.findByRecordId("record-1")).thenReturn(Optional.of(record));

        MedicalRecordResponse response = recordService.getRecordById("record-1", patient);

        assertThat(response.recordId()).isEqualTo("record-1");
    }

    @Test
    void shouldRejectUpdateOutsideEditWindow() {
        MedicalRecord record = record();
        AuthenticatedUser provider = new AuthenticatedUser("provider-user-1", "doctor@medibook.com", Role.PROVIDER);

        when(recordRepository.findByRecordId("record-1")).thenReturn(Optional.of(record));
        when(providerServiceGateway.getProviderByUserId("provider-user-1"))
                .thenReturn(new ProviderSummary("provider-1", "provider-user-1", true, true));
        when(appointmentServiceGateway.getAppointmentById("appointment-1"))
                .thenReturn(new AppointmentSummary(
                        "appointment-1",
                        "patient-1",
                        "provider-1",
                        "General Consultation",
                        LocalDate.of(2026, 4, 18),
                        LocalTime.of(9, 0),
                        LocalTime.of(9, 30),
                        AppointmentStatus.COMPLETED,
                        Instant.parse("2026-04-18T09:30:00Z")));

        assertThatThrownBy(() -> recordService.updateRecord(
                        "record-1",
                        provider,
                        new UpdateMedicalRecordRequest("Updated diagnosis", "Updated prescription", "Updated notes", null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("editing window");
    }

    @Test
    void shouldDispatchDueFollowUpReminderAndMarkRecordAsSent() {
        MedicalRecord record = record();
        record.setFollowUpDate(LocalDate.of(2026, 4, 22));
        record.setFollowUpReminderSentAt(null);

        when(recordRepository.findByFollowUpDateAndFollowUpReminderSentAtIsNullOrderByCreatedAtAsc(LocalDate.of(2026, 4, 22)))
                .thenReturn(List.of(record));
        when(recordRepository.save(any(MedicalRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        recordService.dispatchDueFollowUpReminders();

        verify(notificationServiceGateway).sendFollowUpReminder(any(MedicalRecordReminderPayload.class));
        assertThat(record.getFollowUpReminderSentAt()).isEqualTo(Instant.parse("2026-04-22T10:00:00Z"));
    }

    @Test
    void shouldKeepReminderEligibleWhenNotificationDispatchFails() {
        MedicalRecord record = record();
        record.setFollowUpDate(LocalDate.of(2026, 4, 22));
        record.setFollowUpReminderSentAt(null);

        when(recordRepository.findByFollowUpDateAndFollowUpReminderSentAtIsNullOrderByCreatedAtAsc(LocalDate.of(2026, 4, 22)))
                .thenReturn(List.of(record));
        org.mockito.Mockito.doThrow(new com.medibook.record.exception.ExternalServiceException("down", new RuntimeException()))
                .when(notificationServiceGateway)
                .sendFollowUpReminder(any(MedicalRecordReminderPayload.class));

        recordService.dispatchDueFollowUpReminders();

        assertThat(record.getFollowUpReminderSentAt()).isNull();
        verify(recordRepository, never()).save(any(MedicalRecord.class));
    }

    private AppointmentSummary completedAppointment() {
        return new AppointmentSummary(
                "appointment-1",
                "patient-1",
                "provider-1",
                "General Consultation",
                LocalDate.of(2026, 4, 22),
                LocalTime.of(9, 0),
                LocalTime.of(9, 30),
                AppointmentStatus.COMPLETED,
                Instant.parse("2026-04-22T09:30:00Z"));
    }

    private MedicalRecord record() {
        MedicalRecord record = new MedicalRecord();
        record.setRecordId("record-1");
        record.setAppointmentId("appointment-1");
        record.setPatientId("patient-1");
        record.setProviderId("provider-1");
        record.setDiagnosis("Initial diagnosis");
        record.setPrescription("Initial prescription");
        record.setNotes("Clinical notes");
        record.setCreatedAt(Instant.parse("2026-04-22T09:40:00Z"));
        record.setUpdatedAt(Instant.parse("2026-04-22T09:45:00Z"));
        return record;
    }
}
