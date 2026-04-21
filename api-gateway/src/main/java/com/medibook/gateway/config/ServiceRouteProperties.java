package com.medibook.gateway.config;

import jakarta.validation.constraints.NotNull;
import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "medibook.services")
public class ServiceRouteProperties {

    @NotNull
    private URI auth = URI.create("http://localhost:8081");

    @NotNull
    private URI provider = URI.create("http://localhost:8082");

    @NotNull
    private URI schedule = URI.create("http://localhost:8083");

    @NotNull
    private URI appointment = URI.create("http://localhost:8084");

    @NotNull
    private URI payment = URI.create("http://localhost:8085");

    @NotNull
    private URI review = URI.create("http://localhost:8086");

    @NotNull
    private URI notification = URI.create("http://localhost:8087");

    @NotNull
    private URI record = URI.create("http://localhost:8088");

    public URI getAuth() {
        return auth;
    }

    public void setAuth(URI auth) {
        this.auth = auth;
    }

    public URI getProvider() {
        return provider;
    }

    public void setProvider(URI provider) {
        this.provider = provider;
    }

    public URI getSchedule() {
        return schedule;
    }

    public void setSchedule(URI schedule) {
        this.schedule = schedule;
    }

    public URI getAppointment() {
        return appointment;
    }

    public void setAppointment(URI appointment) {
        this.appointment = appointment;
    }

    public URI getPayment() {
        return payment;
    }

    public void setPayment(URI payment) {
        this.payment = payment;
    }

    public URI getReview() {
        return review;
    }

    public void setReview(URI review) {
        this.review = review;
    }

    public URI getNotification() {
        return notification;
    }

    public void setNotification(URI notification) {
        this.notification = notification;
    }

    public URI getRecord() {
        return record;
    }

    public void setRecord(URI record) {
        this.record = record;
    }
}
