package com.medibook.auth.service;

public interface NotificationServiceGateway {

    void sendPasswordResetNotification(String email, String title, String message);
}
