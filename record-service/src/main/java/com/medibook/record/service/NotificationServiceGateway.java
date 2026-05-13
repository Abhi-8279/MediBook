package com.medibook.record.service;

public interface NotificationServiceGateway {

    void sendFollowUpReminder(MedicalRecordReminderPayload payload);
}
