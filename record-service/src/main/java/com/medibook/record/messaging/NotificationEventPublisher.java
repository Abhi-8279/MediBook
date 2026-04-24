package com.medibook.record.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medibook.record.service.MedicalRecordReminderPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(NotificationEventPublisher.class);

    private static final String FOLLOW_UP_ROUTING_KEY = "record.followup";

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public NotificationEventPublisher(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    public void publishFollowUpReminder(MedicalRecordReminderPayload payload) {
        publish(FOLLOW_UP_ROUTING_KEY, new FollowUpReminderEvent(
                payload.recipientId(),
                payload.recordId(),
                payload.appointmentId(),
                payload.providerId(),
                payload.followUpDate(),
                payload.diagnosis(),
                payload.prescription()));
    }

    private void publish(String routingKey, Object payload) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMessagingConfig.NOTIFICATION_EVENTS_EXCHANGE,
                    routingKey,
                    objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize follow-up notification event", exception);
        } catch (RuntimeException exception) {
            logger.warn("Failed to publish follow-up notification event with routing key {}", routingKey, exception);
            throw exception;
        }
    }
}
