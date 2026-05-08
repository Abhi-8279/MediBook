package com.medibook.appointment.service;

import com.medibook.appointment.config.AppProperties;
import com.medibook.appointment.entity.Appointment;
import com.medibook.appointment.exception.ExternalServiceException;
import java.time.LocalDate;
import java.time.LocalTime;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Component
public class NotificationServiceClient implements NotificationServiceGateway {

    private final RestTemplate restTemplate;
    private final AppProperties appProperties;

    public NotificationServiceClient(RestTemplate restTemplate, AppProperties appProperties) {
        this.restTemplate = restTemplate;
        this.appProperties = appProperties;
    }

    @Override
    public void sendAppointmentBookedNotifications(Appointment appointment) {
        exchange(
                "/api/v1/notifications/internal/appointments/booked",
                new AppointmentBookedNotificationRequest(
                        appointment.getAppointmentId(),
                        appointment.getPatientId(),
                        appointment.getProviderId(),
                        appointment.getServiceType(),
                        appointment.getAppointmentDate(),
                        appointment.getStartTime(),
                        appointment.getEndTime()));
    }

    @Override
    public void sendAppointmentCancelledNotifications(Appointment appointment) {
        exchange(
                "/api/v1/notifications/internal/appointments/cancelled",
                new AppointmentCancelledNotificationRequest(
                        appointment.getAppointmentId(),
                        appointment.getPatientId(),
                        appointment.getProviderId(),
                        appointment.getAppointmentDate(),
                        appointment.getStartTime(),
                        appointment.getEndTime(),
                        appointment.getCancellationReason()));
    }

    @Override
    public void sendAppointmentRescheduledNotifications(
            Appointment appointment,
            LocalDate previousAppointmentDate,
            LocalTime previousStartTime,
            LocalTime previousEndTime) {
        exchange(
                "/api/v1/notifications/internal/appointments/rescheduled",
                new AppointmentRescheduledNotificationRequest(
                        appointment.getAppointmentId(),
                        appointment.getPatientId(),
                        appointment.getProviderId(),
                        appointment.getServiceType(),
                        previousAppointmentDate,
                        previousStartTime,
                        previousEndTime,
                        appointment.getAppointmentDate(),
                        appointment.getStartTime(),
                        appointment.getEndTime()));
    }

    private void exchange(String path, Object payload) {
        HttpHeaders headers = createInternalHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Object> entity = new HttpEntity<>(payload, headers);
        try {
            restTemplate.exchange(
                    notificationServiceBaseUrl() + path,
                    HttpMethod.POST,
                    entity,
                    Void.class);
        } catch (HttpStatusCodeException | ResourceAccessException exception) {
            throw new ExternalServiceException("Notification service appointment dispatch failed", exception);
        }
    }

    private HttpHeaders createInternalHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(appProperties.getInternal().getHeaderName(), appProperties.getInternal().getApiKey());
        return headers;
    }

    private String notificationServiceBaseUrl() {
        return appProperties.getNotificationService().getBaseUrl().replaceAll("/+$", "");
    }

    private record AppointmentBookedNotificationRequest(
            String appointmentId,
            String patientId,
            String providerId,
            String serviceType,
            LocalDate appointmentDate,
            LocalTime startTime,
            LocalTime endTime) {
    }

    private record AppointmentCancelledNotificationRequest(
            String appointmentId,
            String patientId,
            String providerId,
            LocalDate appointmentDate,
            LocalTime startTime,
            LocalTime endTime,
            String cancellationReason) {
    }

    private record AppointmentRescheduledNotificationRequest(
            String appointmentId,
            String patientId,
            String providerId,
            String serviceType,
            LocalDate previousAppointmentDate,
            LocalTime previousStartTime,
            LocalTime previousEndTime,
            LocalDate appointmentDate,
            LocalTime startTime,
            LocalTime endTime) {
    }
}
