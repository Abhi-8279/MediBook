package com.medibook.appointment.service.impl;

import com.medibook.appointment.dto.request.BookAppointmentRequest;
import com.medibook.appointment.dto.request.CancelAppointmentRequest;
import com.medibook.appointment.dto.request.CompleteAppointmentRequest;
import com.medibook.appointment.dto.request.RescheduleAppointmentRequest;
import com.medibook.appointment.dto.request.UpdateAppointmentStatusRequest;
import com.medibook.appointment.dto.response.AppointmentCountResponse;
import com.medibook.appointment.dto.response.AppointmentResponse;
import com.medibook.appointment.dto.response.CompletedAppointmentCheckResponse;
import com.medibook.appointment.entity.Appointment;
import com.medibook.appointment.enums.AppointmentStatus;
import com.medibook.appointment.enums.Role;
import com.medibook.appointment.exception.AppointmentConflictException;
import com.medibook.appointment.exception.ExternalServiceException;
import com.medibook.appointment.exception.ResourceNotFoundException;
import com.medibook.appointment.config.AppProperties;
import com.medibook.appointment.repository.AppointmentRepository;
import com.medibook.appointment.security.AuthenticatedUser;
import com.medibook.appointment.service.AppointmentService;
import com.medibook.appointment.service.PaymentServiceGateway;
import com.medibook.appointment.service.ProviderServiceGateway;
import com.medibook.appointment.service.ProviderSummary;
import com.medibook.appointment.service.ScheduleServiceGateway;
import com.medibook.appointment.service.ScheduleSlotSummary;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final ProviderServiceGateway providerServiceGateway;
    private final ScheduleServiceGateway scheduleServiceGateway;
    private final PaymentServiceGateway paymentServiceGateway;
    private final AppProperties appProperties;
    private final Clock clock;

    public AppointmentServiceImpl(
            AppointmentRepository appointmentRepository,
            ProviderServiceGateway providerServiceGateway,
            ScheduleServiceGateway scheduleServiceGateway,
            PaymentServiceGateway paymentServiceGateway,
            AppProperties appProperties,
            Clock clock) {
        this.appointmentRepository = appointmentRepository;
        this.providerServiceGateway = providerServiceGateway;
        this.scheduleServiceGateway = scheduleServiceGateway;
        this.paymentServiceGateway = paymentServiceGateway;
        this.appProperties = appProperties;
        this.clock = clock;
    }

    @Override
    public AppointmentResponse bookAppointment(AuthenticatedUser authenticatedUser, BookAppointmentRequest request) {
        assertPatient(authenticatedUser);
        providerServiceGateway.assertProviderPubliclyVisible(request.providerId());

        ScheduleSlotSummary slot = scheduleServiceGateway.getSlotById(request.slotId());
        validateBookableSlot(slot, request.providerId());

        Appointment appointment = new Appointment();
        appointment.setAppointmentId(UUID.randomUUID().toString());
        appointment.setPatientId(authenticatedUser.userId());
        appointment.setProviderId(request.providerId().trim());
        appointment.setSlotId(slot.slotId());
        appointment.setServiceType(request.serviceType().trim());
        appointment.setAppointmentDate(slot.date());
        appointment.setStartTime(slot.startTime());
        appointment.setEndTime(slot.endTime());
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        appointment.setNotes(blankToNull(request.notes()));
        appointment.setConsultationMode(request.modeOfConsultation());

        scheduleServiceGateway.bookSlot(slot.slotId(), appointment.getAppointmentId());
        try {
            return toResponse(appointmentRepository.saveAndFlush(appointment));
        } catch (RuntimeException exception) {
            safeReleaseSlot(slot.slotId());
            throw exception;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AppointmentResponse getAppointmentById(String appointmentId, AuthenticatedUser authenticatedUser) {
        Appointment appointment = findAppointmentOrThrow(appointmentId);
        assertCanView(appointment, authenticatedUser);
        return toResponse(appointment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getMyAppointments(AuthenticatedUser authenticatedUser) {
        assertPatient(authenticatedUser);
        return appointmentRepository.findByPatientIdOrderByAppointmentDateDescStartTimeDesc(authenticatedUser.userId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getMyUpcomingAppointments(AuthenticatedUser authenticatedUser) {
        assertPatient(authenticatedUser);
        return appointmentRepository.findUpcomingByPatientId(
                        authenticatedUser.userId(),
                        AppointmentStatus.SCHEDULED,
                        LocalDate.now(clock),
                        LocalTime.now(clock))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getMyProviderAppointments(AuthenticatedUser authenticatedUser) {
        String providerId = resolveCurrentProviderId(authenticatedUser);
        return appointmentRepository.findByProviderIdOrderByAppointmentDateDescStartTimeDesc(providerId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getMyUpcomingProviderAppointments(AuthenticatedUser authenticatedUser) {
        String providerId = resolveCurrentProviderId(authenticatedUser);
        return appointmentRepository.findUpcomingByProviderId(
                        providerId,
                        AppointmentStatus.SCHEDULED,
                        LocalDate.now(clock),
                        LocalTime.now(clock))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getMyProviderAppointmentsForDate(AuthenticatedUser authenticatedUser, LocalDate date) {
        String providerId = resolveCurrentProviderId(authenticatedUser);
        LocalDate effectiveDate = date == null ? LocalDate.now(clock) : date;
        return appointmentRepository.findByProviderIdAndAppointmentDateOrderByStartTimeAsc(providerId, effectiveDate)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getAdminAppointments(
            AppointmentStatus status,
            String patientId,
            String providerId,
            LocalDate appointmentDate) {
        return appointmentRepository.searchAppointments(status, blankToNull(patientId), blankToNull(providerId), appointmentDate)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getAppointmentsByProviderId(
            String providerId,
            LocalDate appointmentDate,
            AuthenticatedUser authenticatedUser) {
        if (isAdmin(authenticatedUser)) {
            return providerAppointments(providerId, appointmentDate);
        }
        String currentProviderId = resolveCurrentProviderId(authenticatedUser);
        if (!currentProviderId.equals(providerId)) {
            throw new AccessDeniedException("You can only access your own provider appointments");
        }
        return providerAppointments(providerId, appointmentDate);
    }

    @Override
    @Transactional(readOnly = true)
    public AppointmentCountResponse getAppointmentCountByProviderId(String providerId, AuthenticatedUser authenticatedUser) {
        if (!isAdmin(authenticatedUser)) {
            String currentProviderId = resolveCurrentProviderId(authenticatedUser);
            if (!currentProviderId.equals(providerId)) {
                throw new AccessDeniedException("You can only access your own provider appointment count");
            }
        }
        return new AppointmentCountResponse(providerId, appointmentRepository.countByProviderId(providerId));
    }

    @Override
    public AppointmentResponse cancelAppointment(
            String appointmentId,
            AuthenticatedUser authenticatedUser,
            CancelAppointmentRequest request) {
        Appointment appointment = findAppointmentOrThrow(appointmentId);
        assertCanCancel(appointment, authenticatedUser);
        ensureScheduled(appointment, "Only scheduled appointments can be cancelled");

        String previousReason = appointment.getCancellationReason();
        Instant previousCancelledAt = appointment.getCancelledAt();

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancellationReason(blankToNull(request.reason()));
        appointment.setCancelledAt(Instant.now(clock));

        Appointment saved = appointmentRepository.saveAndFlush(appointment);
        try {
            scheduleServiceGateway.releaseSlot(saved.getSlotId());
        } catch (RuntimeException exception) {
            appointment.setStatus(AppointmentStatus.SCHEDULED);
            appointment.setCancellationReason(previousReason);
            appointment.setCancelledAt(previousCancelledAt);
            appointmentRepository.saveAndFlush(appointment);
            throw exception;
        }

        requestRefundQuietly(saved);
        return toResponse(saved);
    }

    @Override
    public AppointmentResponse rescheduleAppointment(
            String appointmentId,
            AuthenticatedUser authenticatedUser,
            RescheduleAppointmentRequest request) {
        Appointment appointment = findAppointmentOrThrow(appointmentId);
        assertPatientOwner(appointment, authenticatedUser);
        ensureScheduled(appointment, "Only scheduled appointments can be rescheduled");

        if (appointment.getSlotId().equals(request.slotId().trim())) {
            throw new IllegalArgumentException("Please choose a different slot for rescheduling");
        }

        ScheduleSlotSummary newSlot = scheduleServiceGateway.getSlotById(request.slotId());
        validateBookableSlot(newSlot, appointment.getProviderId());

        AppointmentSnapshot snapshot = AppointmentSnapshot.from(appointment);
        scheduleServiceGateway.bookSlot(newSlot.slotId(), appointment.getAppointmentId());

        appointment.setSlotId(newSlot.slotId());
        appointment.setAppointmentDate(newSlot.date());
        appointment.setStartTime(newSlot.startTime());
        appointment.setEndTime(newSlot.endTime());

        try {
            appointmentRepository.saveAndFlush(appointment);
        } catch (RuntimeException exception) {
            safeReleaseSlot(newSlot.slotId());
            throw exception;
        }

        try {
            scheduleServiceGateway.releaseSlot(snapshot.slotId());
            return toResponse(appointment);
        } catch (RuntimeException exception) {
            snapshot.restore(appointment);
            appointmentRepository.saveAndFlush(appointment);
            safeReleaseSlot(newSlot.slotId());
            safeRebookSlot(snapshot.slotId(), appointment.getAppointmentId());
            throw exception;
        }
    }

    @Override
    public AppointmentResponse completeAppointment(
            String appointmentId,
            AuthenticatedUser authenticatedUser,
            CompleteAppointmentRequest request) {
        Appointment appointment = findAppointmentOrThrow(appointmentId);
        assertCanComplete(appointment, authenticatedUser);
        ensureScheduled(appointment, "Only scheduled appointments can be completed");

        String previousNotes = appointment.getNotes();
        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointment.setCompletedAt(Instant.now(clock));
        if (request != null && request.notes() != null) {
            appointment.setNotes(blankToNull(request.notes()));
        }
        Appointment saved = appointmentRepository.saveAndFlush(appointment);
        try {
            scheduleServiceGateway.completeSlot(saved.getSlotId());
            return toResponse(saved);
        } catch (RuntimeException exception) {
            appointment.setStatus(AppointmentStatus.SCHEDULED);
            appointment.setCompletedAt(null);
            appointment.setNotes(previousNotes);
            appointmentRepository.saveAndFlush(appointment);
            throw exception;
        }
    }

    @Override
    public AppointmentResponse updateStatus(
            String appointmentId,
            AuthenticatedUser authenticatedUser,
            UpdateAppointmentStatusRequest request) {
        Appointment appointment = findAppointmentOrThrow(appointmentId);
        AppointmentStatus targetStatus = request.status();

        return switch (targetStatus) {
            case CANCELLED -> cancelAppointment(
                    appointmentId,
                    authenticatedUser,
                    new CancelAppointmentRequest(request.cancellationReason()));
            case COMPLETED -> completeAppointment(
                    appointmentId,
                    authenticatedUser,
                    new CompleteAppointmentRequest(request.notes()));
            case NO_SHOW -> markNoShow(appointment, authenticatedUser, request.notes());
            case SCHEDULED -> throw new IllegalArgumentException("Manual reset to SCHEDULED is not supported");
        };
    }

    @Override
    @Transactional(readOnly = true)
    public AppointmentResponse getAppointmentByIdInternally(String appointmentId) {
        return toResponse(findAppointmentOrThrow(appointmentId));
    }

    @Override
    @Transactional(readOnly = true)
    public AppointmentCountResponse getAppointmentCountByProviderIdInternally(String providerId) {
        return new AppointmentCountResponse(providerId, appointmentRepository.countByProviderId(providerId));
    }

    @Override
    @Transactional(readOnly = true)
    public CompletedAppointmentCheckResponse hasCompletedAppointment(String patientId, String providerId) {
        boolean eligible = appointmentRepository.existsByPatientIdAndProviderIdAndStatus(
                patientId,
                providerId,
                AppointmentStatus.COMPLETED);
        return new CompletedAppointmentCheckResponse(eligible);
    }

    private AppointmentResponse markNoShow(
            Appointment appointment,
            AuthenticatedUser authenticatedUser,
            String notes) {
        assertCanComplete(appointment, authenticatedUser);
        ensureScheduled(appointment, "Only scheduled appointments can be marked as no-show");
        if (appointmentStartDateTime(appointment).isAfter(LocalDateTime.now(clock))) {
            throw new IllegalStateException("Future appointments cannot be marked as no-show");
        }
        appointment.setStatus(AppointmentStatus.NO_SHOW);
        if (notes != null) {
            appointment.setNotes(blankToNull(notes));
        }
        return toResponse(appointmentRepository.saveAndFlush(appointment));
    }

    private Appointment findAppointmentOrThrow(String appointmentId) {
        return appointmentRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));
    }

    private List<AppointmentResponse> providerAppointments(String providerId, LocalDate appointmentDate) {
        List<Appointment> appointments = appointmentDate == null
                ? appointmentRepository.findByProviderIdOrderByAppointmentDateDescStartTimeDesc(providerId)
                : appointmentRepository.findByProviderIdAndAppointmentDateOrderByStartTimeAsc(providerId, appointmentDate);
        return appointments.stream().map(this::toResponse).toList();
    }

    private void validateBookableSlot(ScheduleSlotSummary slot, String providerId) {
        if (!slot.providerId().equals(providerId.trim())) {
            throw new IllegalArgumentException("The selected slot does not belong to the requested provider");
        }
        if (slot.blocked()) {
            throw new AppointmentConflictException("The selected slot is blocked");
        }
        if (slot.booked()) {
            throw new AppointmentConflictException("The selected slot is already booked");
        }
        if (slotEndDateTime(slot).isBefore(LocalDateTime.now(clock))) {
            throw new IllegalArgumentException("Past slots cannot be booked");
        }
    }

    private void ensureScheduled(Appointment appointment, String message) {
        if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw new IllegalStateException(message);
        }
    }

    private void assertCanView(Appointment appointment, AuthenticatedUser authenticatedUser) {
        if (isAdmin(authenticatedUser)) {
            return;
        }
        if (isPatientOwner(appointment, authenticatedUser)) {
            return;
        }
        if (isProviderOwner(appointment, authenticatedUser)) {
            return;
        }
        throw new AccessDeniedException("You are not allowed to access this appointment");
    }

    private void assertCanCancel(Appointment appointment, AuthenticatedUser authenticatedUser) {
        if (isAdmin(authenticatedUser) || isPatientOwner(appointment, authenticatedUser) || isProviderOwner(appointment, authenticatedUser)) {
            return;
        }
        throw new AccessDeniedException("You are not allowed to cancel this appointment");
    }

    private void assertCanComplete(Appointment appointment, AuthenticatedUser authenticatedUser) {
        if (isAdmin(authenticatedUser) || isProviderOwner(appointment, authenticatedUser)) {
            return;
        }
        throw new AccessDeniedException("Only the provider or an admin can complete this appointment");
    }

    private void assertPatientOwner(Appointment appointment, AuthenticatedUser authenticatedUser) {
        if (!isPatientOwner(appointment, authenticatedUser)) {
            throw new AccessDeniedException("You can only manage your own appointments");
        }
    }

    private boolean isPatientOwner(Appointment appointment, AuthenticatedUser authenticatedUser) {
        return authenticatedUser != null
                && authenticatedUser.role() == Role.PATIENT
                && appointment.getPatientId().equals(authenticatedUser.userId());
    }

    private boolean isProviderOwner(Appointment appointment, AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null || authenticatedUser.role() != Role.PROVIDER) {
            return false;
        }
        try {
            return appointment.getProviderId().equals(resolveCurrentProviderId(authenticatedUser));
        } catch (ResourceNotFoundException exception) {
            return false;
        }
    }

    private String resolveCurrentProviderId(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null || authenticatedUser.role() != Role.PROVIDER) {
            throw new AccessDeniedException("Only providers can access this resource");
        }
        ProviderSummary provider = providerServiceGateway.getProviderByUserId(authenticatedUser.userId());
        return provider.providerId();
    }

    private void assertPatient(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null || authenticatedUser.role() != Role.PATIENT) {
            throw new AccessDeniedException("Only patients can book appointments");
        }
    }

    private boolean isAdmin(AuthenticatedUser authenticatedUser) {
        return authenticatedUser != null && authenticatedUser.role() == Role.ADMIN;
    }

    private void requestRefundQuietly(Appointment appointment) {
        if (!isRefundEligible(appointment)) {
            return;
        }
        try {
            paymentServiceGateway.requestRefund(
                    appointment.getAppointmentId(),
                    appointment.getCancellationReason());
        } catch (ExternalServiceException exception) {
            // Keep appointment cancellation working before payment-service is added.
        }
    }

    private void safeReleaseSlot(String slotId) {
        try {
            scheduleServiceGateway.releaseSlot(slotId);
        } catch (RuntimeException ignored) {
            // Best-effort rollback helper.
        }
    }

    private void safeRebookSlot(String slotId, String appointmentId) {
        try {
            scheduleServiceGateway.bookSlot(slotId, appointmentId);
        } catch (RuntimeException ignored) {
            // Best-effort rollback helper.
        }
    }

    private AppointmentResponse toResponse(Appointment appointment) {
        return new AppointmentResponse(
                appointment.getAppointmentId(),
                appointment.getPatientId(),
                appointment.getProviderId(),
                appointment.getSlotId(),
                appointment.getServiceType(),
                appointment.getAppointmentDate(),
                appointment.getStartTime(),
                appointment.getEndTime(),
                appointment.getStatus(),
                appointment.getNotes(),
                appointment.getConsultationMode(),
                appointment.getCancellationReason(),
                appointment.getCreatedAt(),
                appointment.getUpdatedAt(),
                appointment.getCancelledAt(),
                appointment.getCompletedAt());
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private LocalDateTime slotEndDateTime(ScheduleSlotSummary slot) {
        return LocalDateTime.of(slot.date(), slot.endTime());
    }

    private LocalDateTime appointmentStartDateTime(Appointment appointment) {
        return LocalDateTime.of(appointment.getAppointmentDate(), appointment.getStartTime());
    }

    private boolean isRefundEligible(Appointment appointment) {
        int refundNoticeHours = appProperties.getCancellation().getRefundNoticeHours();
        LocalDateTime refundDeadline = LocalDateTime.now(clock).plusHours(refundNoticeHours);
        return !appointmentStartDateTime(appointment).isBefore(refundDeadline);
    }

    private record AppointmentSnapshot(
            String slotId,
            LocalDate appointmentDate,
            LocalTime startTime,
            LocalTime endTime) {

        static AppointmentSnapshot from(Appointment appointment) {
            return new AppointmentSnapshot(
                    appointment.getSlotId(),
                    appointment.getAppointmentDate(),
                    appointment.getStartTime(),
                    appointment.getEndTime());
        }

        void restore(Appointment appointment) {
            appointment.setSlotId(slotId);
            appointment.setAppointmentDate(appointmentDate);
            appointment.setStartTime(startTime);
            appointment.setEndTime(endTime);
        }
    }
}
