package com.medibook.auth.service;

import com.medibook.auth.config.AppProperties;
import com.medibook.auth.exception.ExternalServiceException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

    private static final Pattern MESSAGE_PATTERN = Pattern.compile("\"message\"\\s*:\\s*\"([^\"]+)\"");

    private final RestTemplate restTemplate;
    private final AppProperties appProperties;

    public NotificationServiceClient(RestTemplate restTemplate, AppProperties appProperties) {
        this.restTemplate = restTemplate;
        this.appProperties = appProperties;
    }

    @Override
    public void sendPasswordResetNotification(String email, String title, String message) {
        HttpHeaders headers = createInternalHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<SendDirectEmailRequest> entity = new HttpEntity<>(
                new SendDirectEmailRequest(
                        email,
                        title,
                        message),
                headers);

        try {
            restTemplate.exchange(
                    notificationServiceBaseUrl() + "/api/v1/notifications/internal/email",
                    HttpMethod.POST,
                    entity,
                    Void.class);
        } catch (HttpStatusCodeException exception) {
            throw new ExternalServiceException(resolveBackendMessage(exception), exception);
        } catch (ResourceAccessException exception) {
            throw new ExternalServiceException(
                    "Password reset email could not be sent because notification-service is unreachable.",
                    exception);
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

    private String resolveBackendMessage(HttpStatusCodeException exception) {
        String responseBody = exception.getResponseBodyAsString();
        if (responseBody != null) {
            Matcher matcher = MESSAGE_PATTERN.matcher(responseBody);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return "Password reset email could not be sent right now. Please try again later.";
    }

    private record SendDirectEmailRequest(
            String toEmail,
            String subject,
            String message) {
    }
}
