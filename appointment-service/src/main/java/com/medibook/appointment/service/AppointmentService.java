package com.medibook.appointment.service;

import com.medibook.appointment.dto.request.BookAppointmentRequest;
import com.medibook.appointment.dto.request.CancelAppointmentRequest;
import com.medibook.appointment.dto.request.CompleteAppointmentRequest;
import com.medibook.appointment.dto.request.RescheduleAppointmentRequest;
import com.medibook.appointment.dto.request.UpdateAppointmentStatusRequest;
import com.medibook.appointment.dto.response.AppointmentCountResponse;
import com.medibook.appointment.dto.response.AppointmentResponse;
import com.medibook.appointment.dto.response.CompletedAppointmentCheckResponse;
import com.medibook.appointment.enums.AppointmentStatus;
import com.medibook.appointment.security.AuthenticatedUser;
import java.time.LocalDate;
import java.util.List;

public interface AppointmentService {

    AppointmentResponse bookAppointment(AuthenticatedUser authenticatedUser, BookAppointmentRequest request);

    AppointmentResponse getAppointmentById(String appointmentId, AuthenticatedUser authenticatedUser);

    List<AppointmentResponse> getMyAppointments(AuthenticatedUser authenticatedUser);

    List<AppointmentResponse> getMyUpcomingAppointments(AuthenticatedUser authenticatedUser);

    List<AppointmentResponse> getMyProviderAppointments(AuthenticatedUser authenticatedUser);

    List<AppointmentResponse> getMyUpcomingProviderAppointments(AuthenticatedUser authenticatedUser);

    List<AppointmentResponse> getMyProviderAppointmentsForDate(AuthenticatedUser authenticatedUser, LocalDate date);

    List<AppointmentResponse> getAdminAppointments(
            AppointmentStatus status,
            String patientId,
            String providerId,
            LocalDate appointmentDate);

    List<AppointmentResponse> getAppointmentsByProviderId(
            String providerId,
            LocalDate appointmentDate,
            AuthenticatedUser authenticatedUser);

    AppointmentCountResponse getAppointmentCountByProviderId(String providerId, AuthenticatedUser authenticatedUser);

    AppointmentResponse cancelAppointment(
            String appointmentId,
            AuthenticatedUser authenticatedUser,
            CancelAppointmentRequest request);

    AppointmentResponse rescheduleAppointment(
            String appointmentId,
            AuthenticatedUser authenticatedUser,
            RescheduleAppointmentRequest request);

    AppointmentResponse completeAppointment(
            String appointmentId,
            AuthenticatedUser authenticatedUser,
            CompleteAppointmentRequest request);

    AppointmentResponse updateStatus(
            String appointmentId,
            AuthenticatedUser authenticatedUser,
            UpdateAppointmentStatusRequest request);

    AppointmentResponse getAppointmentByIdInternally(String appointmentId);

    AppointmentCountResponse getAppointmentCountByProviderIdInternally(String providerId);

    CompletedAppointmentCheckResponse hasCompletedAppointment(String patientId, String providerId);
}
