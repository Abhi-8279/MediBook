package com.medibook.appointment.service;

import com.medibook.appointment.config.AppProperties;
import com.medibook.appointment.exception.ExternalServiceException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Component
public class PaymentServiceClient implements PaymentServiceGateway {

    private final RestTemplate restTemplate;
    private final AppProperties appProperties;

    public PaymentServiceClient(RestTemplate restTemplate, AppProperties appProperties) {
        this.restTemplate = restTemplate;
        this.appProperties = appProperties;
    }

    @Override
    public void requestRefund(String appointmentId, String reason) {
        HttpHeaders headers = createInternalHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<RefundRequest> entity = new HttpEntity<>(new RefundRequest(reason), headers);
        try {
            restTemplate.exchange(
                    paymentServiceBaseUrl() + "/api/v1/payments/internal/appointments/" + appointmentId + "/refund",
                    HttpMethod.POST,
                    entity,
                    Void.class);
        } catch (HttpStatusCodeException | ResourceAccessException exception) {
            throw new ExternalServiceException("Payment service refund trigger failed", exception);
        }
    }

    private HttpHeaders createInternalHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(appProperties.getInternal().getHeaderName(), appProperties.getInternal().getApiKey());
        return headers;
    }

    private String paymentServiceBaseUrl() {
        return appProperties.getPaymentService().getBaseUrl().replaceAll("/+$", "");
    }

    private record RefundRequest(String reason) {
    }
}
