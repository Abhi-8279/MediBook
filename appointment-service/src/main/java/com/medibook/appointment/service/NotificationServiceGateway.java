package com.medibook.appointment.service;

import com.medibook.appointment.entity.Appointment;
import java.time.LocalDate;
import java.time.LocalTime;

public interface NotificationServiceGateway {

    void sendAppointmentBookedNotifications(Appointment appointment);

    void sendAppointmentCancelledNotifications(Appointment appointment);

    void sendAppointmentRescheduledNotifications(
            Appointment appointment,
            LocalDate previousAppointmentDate,
            LocalTime previousStartTime,
            LocalTime previousEndTime);
}
