package com.medibook.payment.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medibook.payment.entity.Payment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(NotificationEventPublisher.class);

    private static final String PAYMENT_PROCESSED_ROUTING_KEY = "payment.processed";
    private static final String PAYMENT_REFUNDED_ROUTING_KEY = "payment.refunded";

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public NotificationEventPublisher(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    public void publishProcessed(Payment payment) {
        publish(PAYMENT_PROCESSED_ROUTING_KEY, new PaymentProcessedEvent(
                payment.getPaymentId(),
                payment.getAppointmentId(),
                payment.getPatientId(),
                payment.getProviderId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getMode(),
                payment.getStatus()));
    }

    public void publishRefunded(Payment payment) {
        publish(PAYMENT_REFUNDED_ROUTING_KEY, new PaymentRefundedEvent(
                payment.getPaymentId(),
                payment.getAppointmentId(),
                payment.getPatientId(),
                payment.getProviderId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getRefundedAt(),
                payment.getNotes()));
    }

    private void publish(String routingKey, Object payload) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMessagingConfig.NOTIFICATION_EVENTS_EXCHANGE,
                    routingKey,
                    objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize payment notification event", exception);
        } catch (RuntimeException exception) {
            logger.warn("Failed to publish payment notification event with routing key {}", routingKey, exception);
            throw exception;
        }
    }
}
