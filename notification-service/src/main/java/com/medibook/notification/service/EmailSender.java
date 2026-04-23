package com.medibook.notification.service;

public interface EmailSender {

    void send(String toEmail, String subject, String body);
}
