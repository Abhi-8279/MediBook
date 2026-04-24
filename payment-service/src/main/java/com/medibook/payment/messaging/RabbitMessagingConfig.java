package com.medibook.payment.messaging;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMessagingConfig {

    public static final String NOTIFICATION_EVENTS_EXCHANGE = "medibook.notification.events";

    @Bean
    public TopicExchange notificationEventsExchange() {
        return new TopicExchange(NOTIFICATION_EVENTS_EXCHANGE, true, false);
    }
}
