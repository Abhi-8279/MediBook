package com.medibook.payment.service;

import com.medibook.payment.config.AppProperties;
import com.medibook.payment.exception.ExternalServiceException;
import com.medibook.payment.exception.ResourceNotFoundException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Component
public class AppointmentServiceClient implements AppointmentServiceGateway {

    private final RestTemplate restTemplate;
    private final AppProperties appProperties;

    public AppointmentServiceClient(RestTemplate restTemplate, AppProperties appProperties) {
        this.restTemplate = restTemplate;
        this.appProperties = appProperties;
    }

    @Override
    public AppointmentSummary getAppointmentById(String appointmentId) {
        HttpEntity<Void> entity = new HttpEntity<>(createInternalHeaders());
        try {
            ResponseEntity<AppointmentSummary> response = restTemplate.exchange(
                    appointmentServiceBaseUrl() + "/api/v1/appointments/internal/" + appointmentId,
                    HttpMethod.GET,
                    entity,
                    AppointmentSummary.class);
            if (response.getBody() == null) {
                throw new ResourceNotFoundException("Appointment not found");
            }
            return response.getBody();
        } catch (HttpStatusCodeException exception) {
            if (exception.getStatusCode().value() == 404) {
                throw new ResourceNotFoundException("Appointment not found");
            }
            throw new ExternalServiceException("Appointment service failed while fetching the appointment", exception);
        } catch (ResourceAccessException exception) {
            throw new ExternalServiceException("Appointment service is currently unreachable", exception);
        }
    }

    private org.springframework.http.HttpHeaders createInternalHeaders() {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.set(appProperties.getInternal().getHeaderName(), appProperties.getInternal().getApiKey());
        return headers;
    }

    private String appointmentServiceBaseUrl() {
        return appProperties.getAppointmentService().getBaseUrl().replaceAll("/+$", "");
    }
}
