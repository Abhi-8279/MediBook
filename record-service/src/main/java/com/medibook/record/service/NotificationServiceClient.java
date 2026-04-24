package com.medibook.record.service;

import com.medibook.record.config.AppProperties;
import com.medibook.record.exception.ExternalServiceException;
import java.time.LocalDate;
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
    public void sendFollowUpReminder(MedicalRecordReminderPayload payload) {
        HttpHeaders headers = createInternalHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<FollowUpReminderRequest> entity = new HttpEntity<>(
                new FollowUpReminderRequest(
                        payload.recipientId(),
                        payload.recordId(),
                        payload.appointmentId(),
                        payload.providerId(),
                        payload.followUpDate(),
                        payload.diagnosis(),
                        payload.prescription()),
                headers);
        try {
            restTemplate.exchange(
                    notificationServiceBaseUrl() + "/api/v1/notifications/internal/follow-up-reminders",
                    HttpMethod.POST,
                    entity,
                    Void.class);
        } catch (HttpStatusCodeException | ResourceAccessException exception) {
            throw new ExternalServiceException("Notification service follow-up reminder dispatch failed", exception);
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

    private record FollowUpReminderRequest(
            String recipientId,
            String recordId,
            String appointmentId,
            String providerId,
            LocalDate followUpDate,
            String diagnosis,
            String prescription) {
    }
}
