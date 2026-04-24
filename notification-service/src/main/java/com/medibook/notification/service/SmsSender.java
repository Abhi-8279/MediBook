package com.medibook.notification.service;

public interface SmsSender {

    void send(String phoneNumber, String message);
}
