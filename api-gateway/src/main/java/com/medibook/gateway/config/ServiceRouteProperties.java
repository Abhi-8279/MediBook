package com.medibook.gateway.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "medibook.services")
public class ServiceRouteProperties {

    @NotBlank
    private String auth = "http://localhost:8081";

    @NotBlank
    private String provider = "http://localhost:8082";

    @NotBlank
    private String schedule = "http://localhost:8083";

    @NotBlank
    private String appointment = "http://localhost:8084";

    @NotBlank
    private String payment = "http://localhost:8085";

    @NotBlank
    private String review = "http://localhost:8086";

    @NotBlank
    private String notification = "http://localhost:8087";

    @NotBlank
    private String record = "http://localhost:8088";

    public String getAuth() {
        return normalizeRouteUri(auth);
    }

    public void setAuth(String auth) {
        this.auth = normalizeRouteUri(auth);
    }

    public String getProvider() {
        return normalizeRouteUri(provider);
    }

    public void setProvider(String provider) {
        this.provider = normalizeRouteUri(provider);
    }

    public String getSchedule() {
        return normalizeRouteUri(schedule);
    }

    public void setSchedule(String schedule) {
        this.schedule = normalizeRouteUri(schedule);
    }

    public String getAppointment() {
        return normalizeRouteUri(appointment);
    }

    public void setAppointment(String appointment) {
        this.appointment = normalizeRouteUri(appointment);
    }

    public String getPayment() {
        return normalizeRouteUri(payment);
    }

    public void setPayment(String payment) {
        this.payment = normalizeRouteUri(payment);
    }

    public String getReview() {
        return normalizeRouteUri(review);
    }

    public void setReview(String review) {
        this.review = normalizeRouteUri(review);
    }

    public String getNotification() {
        return normalizeRouteUri(notification);
    }

    public void setNotification(String notification) {
        this.notification = normalizeRouteUri(notification);
    }

    public String getRecord() {
        return normalizeRouteUri(record);
    }

    public void setRecord(String record) {
        this.record = normalizeRouteUri(record);
    }

    private String normalizeRouteUri(String value) {
        if (value == null || value.isBlank() || value.contains("://")) {
            return value;
        }
        return "http://" + value;
    }
}
