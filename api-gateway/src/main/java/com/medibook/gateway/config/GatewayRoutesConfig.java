package com.medibook.gateway.config;

import java.net.URI;
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
                        .uri(routeUri(properties.getAuth())))
                .route("auth-service-oauth-start", route -> route
                        .path("/oauth2/**")
                        .uri(routeUri(properties.getAuth())))
                .route("auth-service-oauth-callback", route -> route
                        .path("/login/oauth2/**")
                        .uri(routeUri(properties.getAuth())))
                .route("provider-service", route -> route
                        .path("/api/v1/providers/**")
                        .uri(routeUri(properties.getProvider())))
                .route("schedule-service", route -> route
                        .path("/api/v1/schedules/**")
                        .uri(routeUri(properties.getSchedule())))
                .route("appointment-service", route -> route
                        .path("/api/v1/appointments/**")
                        .uri(routeUri(properties.getAppointment())))
                .route("payment-service", route -> route
                        .path("/api/v1/payments/**")
                        .uri(routeUri(properties.getPayment())))
                .route("review-service", route -> route
                        .path("/api/v1/reviews/**")
                        .uri(routeUri(properties.getReview())))
                .route("notification-service", route -> route
                        .path("/api/v1/notifications/**")
                        .uri(routeUri(properties.getNotification())))
                .route("record-service", route -> route
                        .path("/api/v1/records/**")
                        .uri(routeUri(properties.getRecord())))
                .build();
    }

    private String routeUri(URI uri) {
        return uri.toString();
    }
}
