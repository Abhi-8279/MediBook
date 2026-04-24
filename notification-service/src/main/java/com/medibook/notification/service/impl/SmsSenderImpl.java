package com.medibook.notification.service.impl;

import com.medibook.notification.config.AppProperties;
import com.medibook.notification.service.SmsSender;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Component
public class SmsSenderImpl implements SmsSender {

    private static final Logger logger = LoggerFactory.getLogger(SmsSenderImpl.class);

    private final RestTemplate restTemplate;
    private final AppProperties appProperties;

    public SmsSenderImpl(RestTemplate restTemplate, AppProperties appProperties) {
        this.restTemplate = restTemplate;
        this.appProperties = appProperties;
    }

    @Override
    public void send(String phoneNumber, String message) {
        if (!appProperties.getSms().isEnabled()) {
            logger.info("SMS delivery disabled. Would send SMS to {}", phoneNumber);
            return;
        }
        if (!StringUtils.hasText(phoneNumber)) {
            throw new IllegalArgumentException("Recipient phone number is required for SMS notifications");
        }
        if (!StringUtils.hasText(appProperties.getSms().getProviderUrl())) {
            throw new IllegalStateException("SMS provider URL is required when SMS delivery is enabled");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (StringUtils.hasText(appProperties.getSms().getAuthToken())) {
            headers.setBearerAuth(appProperties.getSms().getAuthToken().trim());
        }

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(Map.of(
                "to", phoneNumber.trim(),
                "message", message,
                "senderId", appProperties.getSms().getSenderId()), headers);
        restTemplate.postForEntity(appProperties.getSms().getProviderUrl().trim(), entity, Void.class);
    }
}
