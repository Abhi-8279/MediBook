package com.medibook.appointment.service;

public interface PaymentServiceGateway {

    void requestRefund(String appointmentId, String reason);
}
