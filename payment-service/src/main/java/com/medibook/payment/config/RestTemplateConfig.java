package com.medibook.payment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(AppProperties appProperties) {
        int connectTimeout = Math.max(
                appProperties.getAuthService().getConnectTimeoutMs(),
                Math.max(
                        appProperties.getAppointmentService().getConnectTimeoutMs(),
                        appProperties.getProviderService().getConnectTimeoutMs()));
        int readTimeout = Math.max(
                appProperties.getAuthService().getReadTimeoutMs(),
                Math.max(
                        appProperties.getAppointmentService().getReadTimeoutMs(),
                        appProperties.getProviderService().getReadTimeoutMs()));

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        return new RestTemplate(factory);
    }
}
