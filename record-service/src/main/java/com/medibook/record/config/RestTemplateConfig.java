package com.medibook.record.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(AppProperties appProperties) {
        int connectTimeout = Math.max(
                Math.max(appProperties.getAuthService().getConnectTimeoutMs(), appProperties.getAppointmentService().getConnectTimeoutMs()),
                Math.max(appProperties.getProviderService().getConnectTimeoutMs(), appProperties.getNotificationService().getConnectTimeoutMs()));
        int readTimeout = Math.max(
                Math.max(appProperties.getAuthService().getReadTimeoutMs(), appProperties.getAppointmentService().getReadTimeoutMs()),
                Math.max(appProperties.getProviderService().getReadTimeoutMs(), appProperties.getNotificationService().getReadTimeoutMs()));

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        return new RestTemplate(factory);
    }
}
