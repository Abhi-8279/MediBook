package com.medibook.notification.service.impl;

import com.medibook.notification.config.AppProperties;
import com.medibook.notification.service.EmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class EmailSenderImpl implements EmailSender {

    private static final Logger logger = LoggerFactory.getLogger(EmailSenderImpl.class);

    private final JavaMailSender javaMailSender;
    private final AppProperties appProperties;

    public EmailSenderImpl(JavaMailSender javaMailSender, AppProperties appProperties) {
        this.javaMailSender = javaMailSender;
        this.appProperties = appProperties;
    }

    @Override
    public void send(String toEmail, String subject, String body) {
        if (!appProperties.getEmail().isEnabled()) {
            logger.info("Email delivery disabled. Would send email to {} with subject {}", toEmail, subject);
            return;
        }
        if (!StringUtils.hasText(toEmail)) {
            throw new IllegalArgumentException("Recipient email is required for email notifications");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(appProperties.getEmail().getFromAddress());
        message.setTo(toEmail.trim());
        message.setSubject(subject);
        message.setText(body);
        javaMailSender.send(message);
    }
}
