package com.medibook.appointment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.medibook.appointment.dto.request.BookAppointmentRequest;
import com.medibook.appointment.dto.request.CancelAppointmentRequest;
import com.medibook.appointment.dto.request.CompleteAppointmentRequest;
import com.medibook.appointment.dto.request.RescheduleAppointmentRequest;
import com.medibook.appointment.dto.response.AppointmentResponse;
import com.medibook.appointment.config.AppProperties;
import com.medibook.appointment.entity.Appointment;
import com.medibook.appointment.enums.AppointmentStatus;
import com.medibook.appointment.enums.ConsultationMode;
import com.medibook.appointment.enums.Role;
import com.medibook.appointment.repository.AppointmentRepository;
import com.medibook.appointment.security.AuthenticatedUser;
import com.medibook.appointment.service.impl.AppointmentServiceImpl;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceImplTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-04-22T10:00:00Z"), ZoneOffset.UTC);

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private ProviderServiceGateway providerServiceGateway;

    @Mock
    private ScheduleServiceGateway scheduleServiceGateway;

    @Mock
    private PaymentServiceGateway paymentServiceGateway;

    private RecordingNotificationServiceGateway notificationServiceGateway;
    private AppointmentServiceImpl appointmentService;
    private AppProperties appProperties;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        notificationServiceGateway = new RecordingNotificationServiceGateway();
        appointmentService = new AppointmentServiceImpl(
                appointmentRepository,
                providerServiceGateway,
                scheduleServiceGateway,
                paymentServiceGateway,
                notificationServiceGateway,
                appProperties,
                FIXED_CLOCK);
    }

    @Test
    void shouldBookAppointmentAndReserveSlot() {
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                "patient-1",
                "patient@medibook.com",
                Role.PATIENT);
        ScheduleSlotSummary slot = new ScheduleSlotSummary(
                "slot-1",
                "provider-1",
                LocalDate.of(2026, 4, 25),
                LocalTime.of(11, 0),
                LocalTime.of(11, 30),
                30,
                false,
                false,
                null);

        when(scheduleServiceGateway.getSlotById("slot-1")).thenReturn(slot);
        when(appointmentRepository.saveAndFlush(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppointmentResponse response = appointmentService.bookAppointment(
                authenticatedUser,
                new BookAppointmentRequest(
                        "provider-1",
                        "slot-1",
                        "General Consultation",
                        ConsultationMode.IN_PERSON,
                        "Bring previous reports"));

        ArgumentCaptor<Appointment> captor = ArgumentCaptor.forClass(Appointment.class);
        verify(appointmentRepository).saveAndFlush(captor.capture());
        Appointment savedAppointment = captor.getValue();

        assertThat(savedAppointment.getPatientId()).isEqualTo("patient-1");
        assertThat(savedAppointment.getProviderId()).isEqualTo("provider-1");
        assertThat(savedAppointment.getSlotId()).isEqualTo("slot-1");
        assertThat(savedAppointment.getStatus()).isEqualTo(AppointmentStatus.SCHEDULED);
        assertThat(response.slotId()).isEqualTo("slot-1");
        verify(providerServiceGateway).assertProviderPubliclyVisible("provider-1");
        verify(scheduleServiceGateway).bookSlot(eq("slot-1"), any(String.class));
        assertThat(notificationServiceGateway.bookedAppointment).isSameAs(savedAppointment);
    }

    @Test
    void shouldCancelAppointmentAndReleaseSlot() {
        Appointment appointment = buildAppointment();
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                "patient-1",
                "patient@medibook.com",
                Role.PATIENT);

        when(appointmentRepository.findByAppointmentId("appointment-1")).thenReturn(Optional.of(appointment));
        when(appointmentRepository.saveAndFlush(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(scheduleServiceGateway).releaseSlot("slot-1");
        doNothing().when(paymentServiceGateway).requestRefund("appointment-1", "Need to reschedule later");

        AppointmentResponse response = appointmentService.cancelAppointment(
                "appointment-1",
                authenticatedUser,
                new CancelAppointmentRequest("Need to reschedule later"));

        assertThat(response.status()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(response.cancellationReason()).isEqualTo("Need to reschedule later");
        verify(scheduleServiceGateway).releaseSlot("slot-1");
        verify(paymentServiceGateway).requestRefund("appointment-1", "Need to reschedule later");
        assertThat(notificationServiceGateway.cancelledAppointment).isSameAs(appointment);
    }

    @Test
    void shouldSkipRefundWhenCancellationFallsInsideRefundWindow() {
        Appointment appointment = buildAppointment();
        appointment.setAppointmentDate(LocalDate.of(2026, 4, 22));
        appointment.setStartTime(LocalTime.of(11, 0));
        appointment.setEndTime(LocalTime.of(11, 30));
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                "patient-1",
                "patient@medibook.com",
                Role.PATIENT);

        when(appointmentRepository.findByAppointmentId("appointment-1")).thenReturn(Optional.of(appointment));
        when(appointmentRepository.saveAndFlush(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        appointmentService.cancelAppointment(
                "appointment-1",
                authenticatedUser,
                new CancelAppointmentRequest("Need to cancel"));

        verify(scheduleServiceGateway).releaseSlot("slot-1");
        verify(paymentServiceGateway, org.mockito.Mockito.never()).requestRefund(any(), any());
    }

    @Test
    void shouldRescheduleAppointmentToNewSlotOfSameProvider() {
        Appointment appointment = buildAppointment();
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                "patient-1",
                "patient@medibook.com",
                Role.PATIENT);
        ScheduleSlotSummary newSlot = new ScheduleSlotSummary(
                "slot-2",
                "provider-1",
                LocalDate.of(2026, 4, 26),
                LocalTime.of(9, 0),
                LocalTime.of(9, 30),
                30,
                false,
                false,
                null);

        when(appointmentRepository.findByAppointmentId("appointment-1")).thenReturn(Optional.of(appointment));
        when(scheduleServiceGateway.getSlotById("slot-2")).thenReturn(newSlot);
        when(appointmentRepository.saveAndFlush(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppointmentResponse response = appointmentService.rescheduleAppointment(
                "appointment-1",
                authenticatedUser,
                new RescheduleAppointmentRequest("slot-2"));

        assertThat(response.slotId()).isEqualTo("slot-2");
        assertThat(response.appointmentDate()).isEqualTo(LocalDate.of(2026, 4, 26));
        verify(scheduleServiceGateway).bookSlot("slot-2", "appointment-1");
        verify(scheduleServiceGateway).releaseSlot("slot-1");
        assertThat(notificationServiceGateway.rescheduledAppointment).isSameAs(appointment);
        assertThat(notificationServiceGateway.previousAppointmentDate).isEqualTo(LocalDate.of(2026, 4, 25));
        assertThat(notificationServiceGateway.previousStartTime).isEqualTo(LocalTime.of(11, 0));
        assertThat(notificationServiceGateway.previousEndTime).isEqualTo(LocalTime.of(11, 30));
    }

    @Test
    void shouldRejectRescheduleToDifferentProviderSlot() {
        Appointment appointment = buildAppointment();
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                "patient-1",
                "patient@medibook.com",
                Role.PATIENT);
        ScheduleSlotSummary otherProviderSlot = new ScheduleSlotSummary(
                "slot-9",
                "provider-2",
                LocalDate.of(2026, 4, 26),
                LocalTime.of(9, 0),
                LocalTime.of(9, 30),
                30,
                false,
                false,
                null);

        when(appointmentRepository.findByAppointmentId("appointment-1")).thenReturn(Optional.of(appointment));
        when(scheduleServiceGateway.getSlotById("slot-9")).thenReturn(otherProviderSlot);

        assertThatThrownBy(() -> appointmentService.rescheduleAppointment(
                        "appointment-1",
                        authenticatedUser,
                        new RescheduleAppointmentRequest("slot-9")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("requested provider");
    }

    @Test
    void shouldAllowProviderToCompleteOwnAppointment() {
        Appointment appointment = buildAppointment();
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                "provider-user-1",
                "doctor@medibook.com",
                Role.PROVIDER);

        when(appointmentRepository.findByAppointmentId("appointment-1")).thenReturn(Optional.of(appointment));
        when(providerServiceGateway.getProviderByUserId("provider-user-1"))
                .thenReturn(new ProviderSummary("provider-1", "provider-user-1", true, true));
        when(appointmentRepository.saveAndFlush(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppointmentResponse response = appointmentService.completeAppointment(
                "appointment-1",
                authenticatedUser,
                new CompleteAppointmentRequest("Consultation completed successfully"));

        assertThat(response.status()).isEqualTo(AppointmentStatus.COMPLETED);
        assertThat(response.notes()).isEqualTo("Consultation completed successfully");
        verify(scheduleServiceGateway).completeSlot("slot-1");
    }

    @Test
    void shouldReturnOnlyUpcomingProviderAppointments() {
        Appointment pastAppointment = buildAppointment();
        pastAppointment.setAppointmentId("appointment-past");
        pastAppointment.setAppointmentDate(LocalDate.of(2026, 4, 22));
        pastAppointment.setStartTime(LocalTime.of(9, 0));
        pastAppointment.setEndTime(LocalTime.of(9, 30));

        Appointment upcomingAppointment = buildAppointment();
        upcomingAppointment.setAppointmentId("appointment-upcoming");
        upcomingAppointment.setAppointmentDate(LocalDate.of(2026, 4, 23));
        upcomingAppointment.setStartTime(LocalTime.of(10, 0));
        upcomingAppointment.setEndTime(LocalTime.of(10, 30));

        AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                "provider-user-1",
                "doctor@medibook.com",
                Role.PROVIDER);

        when(providerServiceGateway.getProviderByUserId("provider-user-1"))
                .thenReturn(new ProviderSummary("provider-1", "provider-user-1", true, true));
        when(appointmentRepository.findUpcomingByProviderId(
                        "provider-1",
                        AppointmentStatus.SCHEDULED,
                        LocalDate.of(2026, 4, 22),
                        LocalTime.of(10, 0)))
                .thenReturn(java.util.List.of(upcomingAppointment));

        java.util.List<AppointmentResponse> response = appointmentService.getMyUpcomingProviderAppointments(authenticatedUser);

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().appointmentId()).isEqualTo("appointment-upcoming");
    }

    private Appointment buildAppointment() {
        Appointment appointment = new Appointment();
        appointment.setAppointmentId("appointment-1");
        appointment.setPatientId("patient-1");
        appointment.setProviderId("provider-1");
        appointment.setSlotId("slot-1");
        appointment.setServiceType("General Consultation");
        appointment.setAppointmentDate(LocalDate.of(2026, 4, 25));
        appointment.setStartTime(LocalTime.of(11, 0));
        appointment.setEndTime(LocalTime.of(11, 30));
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        appointment.setConsultationMode(ConsultationMode.IN_PERSON);
        appointment.setNotes("Initial booking");
        return appointment;
    }

    private static final class RecordingNotificationServiceGateway implements NotificationServiceGateway {

        private Appointment bookedAppointment;
        private Appointment cancelledAppointment;
        private Appointment rescheduledAppointment;
        private LocalDate previousAppointmentDate;
        private LocalTime previousStartTime;
        private LocalTime previousEndTime;

        @Override
        public void sendAppointmentBookedNotifications(Appointment appointment) {
            this.bookedAppointment = appointment;
        }

        @Override
        public void sendAppointmentCancelledNotifications(Appointment appointment) {
            this.cancelledAppointment = appointment;
        }

        @Override
        public void sendAppointmentRescheduledNotifications(
                Appointment appointment,
                LocalDate previousAppointmentDate,
                LocalTime previousStartTime,
                LocalTime previousEndTime) {
            this.rescheduledAppointment = appointment;
            this.previousAppointmentDate = previousAppointmentDate;
            this.previousStartTime = previousStartTime;
            this.previousEndTime = previousEndTime;
        }
    }
}
