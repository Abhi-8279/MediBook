package com.medibook.payment.service.impl;

import com.medibook.payment.config.AppProperties;
import com.medibook.payment.exception.ExternalServiceException;
import com.medibook.payment.service.RazorpayGateway;
import com.medibook.payment.service.RazorpayOrder;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class RazorpayGatewayClient implements RazorpayGateway {

    private final RestTemplate restTemplate;
    private final AppProperties appProperties;

    public RazorpayGatewayClient(RestTemplate restTemplate, AppProperties appProperties) {
        this.restTemplate = restTemplate;
        this.appProperties = appProperties;
    }

    @Override
    public RazorpayOrder createOrder(String receipt, BigDecimal amount, String currency, Map<String, String> notes) {
        AppProperties.Razorpay razorpay = appProperties.getPayment().getRazorpay();
        validateConfiguration(razorpay);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth(razorpay.getKeyId().trim(), razorpay.getKeySecret().trim());

        Map<String, Object> body = Map.of(
                "amount", toSubunits(amount),
                "currency", currency,
                "receipt", receipt,
                "notes", notes == null ? Map.of() : notes);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    normalizeBaseUrl(razorpay.getApiBaseUrl()) + "/orders",
                    new HttpEntity<>(body, headers),
                    Map.class);
            Map<?, ?> payload = response.getBody();
            if (payload == null) {
                throw new IllegalStateException("Razorpay did not return an order response");
            }
            return new RazorpayOrder(
                    stringValue(payload.get("id")),
                    longValue(payload.get("amount")),
                    stringValue(payload.get("currency")),
                    stringValue(payload.get("status")),
                    stringValue(payload.get("receipt")));
        } catch (ResourceAccessException exception) {
            throw new ExternalServiceException("Razorpay is currently unreachable", exception);
        } catch (RestClientException exception) {
            throw new ExternalServiceException("Razorpay rejected the order creation request", exception);
        }
    }

    @Override
    public String getKeyId() {
        return appProperties.getPayment().getRazorpay().getKeyId();
    }

    private void validateConfiguration(AppProperties.Razorpay razorpay) {
        if (!razorpay.isEnabled()) {
            throw new IllegalStateException("Razorpay checkout is not enabled for this payment service");
        }
        if (!StringUtils.hasText(razorpay.getKeyId()) || !StringUtils.hasText(razorpay.getKeySecret())) {
            throw new IllegalStateException("Razorpay credentials are missing from the payment service configuration");
        }
    }

    private String normalizeBaseUrl(String baseUrl) {
        return baseUrl.replaceAll("/+$", "");
    }

    private long toSubunits(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP)
                .movePointRight(2)
                .longValueExact();
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(stringValue(value));
    }
}
