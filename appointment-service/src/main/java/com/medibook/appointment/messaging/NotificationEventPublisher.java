package com.medibook.appointment.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medibook.appointment.entity.Appointment;
import java.time.LocalDate;
import java.time.LocalTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(NotificationEventPublisher.class);

    private static final String APPOINTMENT_BOOKED_ROUTING_KEY = "appointment.booked";
    private static final String APPOINTMENT_CANCELLED_ROUTING_KEY = "appointment.cancelled";
    private static final String APPOINTMENT_RESCHEDULED_ROUTING_KEY = "appointment.rescheduled";

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public NotificationEventPublisher(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    public void publishBooked(Appointment appointment) {
        publish(APPOINTMENT_BOOKED_ROUTING_KEY, new AppointmentBookedEvent(
                appointment.getAppointmentId(),
                appointment.getPatientId(),
                appointment.getProviderId(),
                appointment.getServiceType(),
                appointment.getConsultationMode(),
                appointment.getAppointmentDate(),
                appointment.getStartTime(),
                appointment.getEndTime()));
    }

    public void publishCancelled(Appointment appointment) {
        publish(APPOINTMENT_CANCELLED_ROUTING_KEY, new AppointmentCancelledEvent(
                appointment.getAppointmentId(),
                appointment.getPatientId(),
                appointment.getProviderId(),
                appointment.getAppointmentDate(),
                appointment.getStartTime(),
                appointment.getEndTime(),
                appointment.getCancellationReason()));
    }

    public void publishRescheduled(
            Appointment appointment,
            LocalDate previousAppointmentDate,
            LocalTime previousStartTime,
            LocalTime previousEndTime) {
        publish(APPOINTMENT_RESCHEDULED_ROUTING_KEY, new AppointmentRescheduledEvent(
                appointment.getAppointmentId(),
                appointment.getPatientId(),
                appointment.getProviderId(),
                previousAppointmentDate,
                previousStartTime,
                previousEndTime,
                appointment.getAppointmentDate(),
                appointment.getStartTime(),
                appointment.getEndTime(),
                appointment.getServiceType()));
    }

    private void publish(String routingKey, Object payload) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMessagingConfig.NOTIFICATION_EVENTS_EXCHANGE,
                    routingKey,
                    objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize notification event", exception);
        } catch (RuntimeException exception) {
            logger.warn("Failed to publish notification event with routing key {}", routingKey, exception);
            throw exception;
        }
    }
}
