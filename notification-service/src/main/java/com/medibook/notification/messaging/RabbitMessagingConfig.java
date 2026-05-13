package com.medibook.notification.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMessagingConfig {

    public static final String NOTIFICATION_EVENTS_EXCHANGE = "medibook.notification.events";

    public static final String APPOINTMENT_EVENTS_QUEUE = "medibook.notification.appointment-events";
    public static final String PAYMENT_EVENTS_QUEUE = "medibook.notification.payment-events";
    public static final String RECORD_EVENTS_QUEUE = "medibook.notification.record-events";

    @Bean
    public TopicExchange notificationEventsExchange() {
        return new TopicExchange(NOTIFICATION_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public Queue appointmentEventsQueue() {
        return new Queue(APPOINTMENT_EVENTS_QUEUE, true);
    }

    @Bean
    public Queue paymentEventsQueue() {
        return new Queue(PAYMENT_EVENTS_QUEUE, true);
    }

    @Bean
    public Queue recordEventsQueue() {
        return new Queue(RECORD_EVENTS_QUEUE, true);
    }

    @Bean
    public Binding appointmentBookedBinding(Queue appointmentEventsQueue, TopicExchange notificationEventsExchange) {
        return BindingBuilder.bind(appointmentEventsQueue).to(notificationEventsExchange).with("appointment.booked");
    }

    @Bean
    public Binding appointmentCancelledBinding(Queue appointmentEventsQueue, TopicExchange notificationEventsExchange) {
        return BindingBuilder.bind(appointmentEventsQueue).to(notificationEventsExchange).with("appointment.cancelled");
    }

    @Bean
    public Binding appointmentRescheduledBinding(Queue appointmentEventsQueue, TopicExchange notificationEventsExchange) {
        return BindingBuilder.bind(appointmentEventsQueue).to(notificationEventsExchange).with("appointment.rescheduled");
    }

    @Bean
    public Binding paymentProcessedBinding(Queue paymentEventsQueue, TopicExchange notificationEventsExchange) {
        return BindingBuilder.bind(paymentEventsQueue).to(notificationEventsExchange).with("payment.processed");
    }

    @Bean
    public Binding paymentRefundedBinding(Queue paymentEventsQueue, TopicExchange notificationEventsExchange) {
        return BindingBuilder.bind(paymentEventsQueue).to(notificationEventsExchange).with("payment.refunded");
    }

    @Bean
    public Binding recordFollowUpBinding(Queue recordEventsQueue, TopicExchange notificationEventsExchange) {
        return BindingBuilder.bind(recordEventsQueue).to(notificationEventsExchange).with("record.followup");
    }
}
