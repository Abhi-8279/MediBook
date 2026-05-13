package com.medibook.notification.service.impl;

import com.medibook.notification.config.AppProperties;
import com.medibook.notification.service.EmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
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
        if (!StringUtils.hasText(appProperties.getEmail().getFromAddress())) {
            throw new IllegalStateException("NOTIFICATION_EMAIL_FROM must be configured for SMTP email delivery");
        }
        if (!StringUtils.hasText(toEmail)) {
            throw new IllegalArgumentException("Recipient email is required for email notifications");
        }
        if (!StringUtils.hasText(configuredUsername())) {
            throw new IllegalStateException("MAIL_USERNAME must be configured for SMTP email delivery");
        }
        if (!StringUtils.hasText(configuredPassword())) {
            throw new IllegalStateException("MAIL_PASSWORD must be configured for SMTP email delivery");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(appProperties.getEmail().getFromAddress());
        message.setTo(toEmail.trim());
        message.setSubject(subject);
        message.setText(body);
        try {
            javaMailSender.send(message);
        } catch (MailException exception) {
            logger.error("SMTP email delivery failed for {}", toEmail, exception);
            throw new IllegalStateException(
                    "SMTP email delivery failed. Check Gmail address, app password, and SMTP settings.",
                    exception);
        }
    }

    private String configuredUsername() {
        if (javaMailSender instanceof JavaMailSenderImpl mailSender) {
            return mailSender.getUsername();
        }
        return null;
    }

    private String configuredPassword() {
        if (javaMailSender instanceof JavaMailSenderImpl mailSender) {
            return mailSender.getPassword();
        }
        return null;
    }
}
