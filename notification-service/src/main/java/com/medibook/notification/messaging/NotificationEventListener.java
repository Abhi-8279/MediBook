package com.medibook.notification.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medibook.notification.dto.request.FollowUpReminderRequest;
import com.medibook.notification.dto.request.SendAppointmentBookedNotificationRequest;
import com.medibook.notification.dto.request.SendAppointmentCancelledNotificationRequest;
import com.medibook.notification.dto.request.SendAppointmentRescheduledNotificationRequest;
import com.medibook.notification.dto.request.SendNotificationRequest;
import com.medibook.notification.enums.NotificationChannel;
import com.medibook.notification.enums.NotificationType;
import com.medibook.notification.messaging.event.AppointmentBookedEvent;
import com.medibook.notification.messaging.event.AppointmentCancelledEvent;
import com.medibook.notification.messaging.event.AppointmentRescheduledEvent;
import com.medibook.notification.messaging.event.FollowUpReminderEvent;
import com.medibook.notification.messaging.event.PaymentProcessedEvent;
import com.medibook.notification.messaging.event.PaymentRefundedEvent;
import com.medibook.notification.service.NotificationService;
import java.math.BigDecimal;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConditionalOnProperty(name = "app.messaging.rabbit.enabled", havingValue = "true")
public class NotificationEventListener {

    private static final Logger logger = LoggerFactory.getLogger(NotificationEventListener.class);
    private static final List<NotificationChannel> DEFAULT_PATIENT_CHANNELS =
            List.of(NotificationChannel.APP, NotificationChannel.EMAIL);

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    public NotificationEventListener(NotificationService notificationService, ObjectMapper objectMapper) {
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = RabbitMessagingConfig.APPOINTMENT_EVENTS_QUEUE)
    public void handleAppointmentEvent(String payload, Message message) {
        String routingKey = message.getMessageProperties().getReceivedRoutingKey();
        try {
            switch (routingKey) {
                case "appointment.booked" -> onAppointmentBooked(readValue(payload, AppointmentBookedEvent.class));
                case "appointment.cancelled" -> onAppointmentCancelled(readValue(payload, AppointmentCancelledEvent.class));
                case "appointment.rescheduled" -> onAppointmentRescheduled(readValue(payload, AppointmentRescheduledEvent.class));
                default -> logger.warn("Ignoring unsupported appointment routing key {}", routingKey);
            }
        } catch (Exception exception) {
            logger.error("Failed to process appointment notification event {}", routingKey, exception);
        }
    }

    @RabbitListener(queues = RabbitMessagingConfig.PAYMENT_EVENTS_QUEUE)
    public void handlePaymentEvent(String payload, Message message) {
        String routingKey = message.getMessageProperties().getReceivedRoutingKey();
        try {
            switch (routingKey) {
                case "payment.processed" -> onPaymentProcessed(readValue(payload, PaymentProcessedEvent.class));
                case "payment.refunded" -> onPaymentRefunded(readValue(payload, PaymentRefundedEvent.class));
                default -> logger.warn("Ignoring unsupported payment routing key {}", routingKey);
            }
        } catch (Exception exception) {
            logger.error("Failed to process payment notification event {}", routingKey, exception);
        }
    }

    @RabbitListener(queues = RabbitMessagingConfig.RECORD_EVENTS_QUEUE)
    public void handleRecordEvent(String payload, Message message) {
        String routingKey = message.getMessageProperties().getReceivedRoutingKey();
        try {
            if ("record.followup".equals(routingKey)) {
                FollowUpReminderEvent event = readValue(payload, FollowUpReminderEvent.class);
                notificationService.sendFollowUpReminder(new FollowUpReminderRequest(
                        event.recipientId(),
                        event.recordId(),
                        event.appointmentId(),
                        event.providerId(),
                        event.followUpDate(),
                        event.diagnosis(),
                        event.prescription()));
            } else {
                logger.warn("Ignoring unsupported record routing key {}", routingKey);
            }
        } catch (Exception exception) {
            logger.error("Failed to process record notification event {}", routingKey, exception);
        }
    }

    private void onAppointmentBooked(AppointmentBookedEvent event) {
        notificationService.sendAppointmentBookedNotifications(new SendAppointmentBookedNotificationRequest(
                event.appointmentId(),
                event.patientId(),
                event.providerId(),
                event.serviceType(),
                event.appointmentDate(),
                event.startTime(),
                event.endTime()));
    }

    private void onAppointmentCancelled(AppointmentCancelledEvent event) {
        notificationService.sendAppointmentCancelledNotifications(new SendAppointmentCancelledNotificationRequest(
                event.appointmentId(),
                event.patientId(),
                event.providerId(),
                event.appointmentDate(),
                event.startTime(),
                event.endTime(),
                event.cancellationReason()));
    }

    private void onAppointmentRescheduled(AppointmentRescheduledEvent event) {
        notificationService.sendAppointmentRescheduledNotifications(new SendAppointmentRescheduledNotificationRequest(
                event.appointmentId(),
                event.patientId(),
                event.providerId(),
                event.serviceType(),
                event.previousAppointmentDate(),
                event.previousStartTime(),
                event.previousEndTime(),
                event.appointmentDate(),
                event.startTime(),
                event.endTime()));
    }

    private void onPaymentProcessed(PaymentProcessedEvent event) {
        String status = blankSafe(event.status()).toUpperCase();
        String mode = blankSafe(event.mode()).toUpperCase();
        String title = "PAID".equals(status) ? "Payment received" : "Payment pending";
        String message = "PAID".equals(status)
                ? "We received your payment of " + formatMoney(event.amount(), event.currency())
                        + " for appointment " + event.appointmentId() + " via " + mode + "."
                : "Your payment of " + formatMoney(event.amount(), event.currency())
                        + " for appointment " + event.appointmentId() + " is pending via " + mode + ".";
        notificationService.send(new SendNotificationRequest(
                event.patientId(),
                NotificationType.PAYMENT,
                title,
                message,
                DEFAULT_PATIENT_CHANNELS,
                event.paymentId(),
                "PAYMENT"));
    }

    private void onPaymentRefunded(PaymentRefundedEvent event) {
        String notesSuffix = StringUtils.hasText(event.notes()) ? " Details: " + event.notes().trim() + "." : "";
        notificationService.send(new SendNotificationRequest(
                event.patientId(),
                NotificationType.PAYMENT,
                "Refund processed",
                "A refund of " + formatMoney(event.amount(), event.currency())
                        + " has been processed for appointment " + event.appointmentId() + "." + notesSuffix,
                DEFAULT_PATIENT_CHANNELS,
                event.paymentId(),
                "PAYMENT"));
    }

    private String formatMoney(BigDecimal amount, String currency) {
        return blankSafe(currency) + " " + (amount == null ? BigDecimal.ZERO : amount);
    }

    private String blankSafe(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }

    private <T> T readValue(String payload, Class<T> targetType) throws JsonProcessingException {
        return objectMapper.readValue(payload, targetType);
    }
}
