package com.medibook.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRoutesConfig {

    @Bean
    public RouteLocator medibookRoutes(RouteLocatorBuilder builder, ServiceRouteProperties properties) {
        return builder.routes()
                .route("auth-service", route -> route
                        .path("/api/v1/auth/**")
                        .uri(properties.getAuth()))
                .route("auth-service-oauth-start", route -> route
                        .path("/oauth2/**")
                        .uri(properties.getAuth()))
                .route("auth-service-oauth-callback", route -> route
                        .path("/login/oauth2/**")
                        .uri(properties.getAuth()))
                .route("provider-service", route -> route
                        .path("/api/v1/providers/**")
                        .uri(properties.getProvider()))
                .route("schedule-service", route -> route
                        .path("/api/v1/schedules/**")
                        .uri(properties.getSchedule()))
                .route("appointment-service", route -> route
                        .path("/api/v1/appointments/**")
                        .uri(properties.getAppointment()))
                .route("payment-service", route -> route
                        .path("/api/v1/payments/**")
                        .uri(properties.getPayment()))
                .route("review-service", route -> route
                        .path("/api/v1/reviews/**")
                        .uri(properties.getReview()))
                .route("notification-service", route -> route
                        .path("/api/v1/notifications/**")
                        .uri(properties.getNotification()))
                .route("record-service", route -> route
                        .path("/api/v1/records/**")
                        .uri(properties.getRecord()))
                .build();
    }
}
