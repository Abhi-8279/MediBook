package com.medibook.notification.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final DownstreamService authService = new DownstreamService();
    private final Internal internal = new Internal();
    private final Email email = new Email();
    private final Sms sms = new Sms();
    private final Reminders reminders = new Reminders();

    public DownstreamService getAuthService() {
        return authService;
    }

    public Internal getInternal() {
        return internal;
    }

    public Email getEmail() {
        return email;
    }

    public Sms getSms() {
        return sms;
    }

    public Reminders getReminders() {
        return reminders;
    }

    public static class DownstreamService {
        @NotBlank
        private String baseUrl;

        @Min(1)
        private int connectTimeoutMs = 3000;

        @Min(1)
        private int readTimeoutMs = 5000;

        public String getBaseUrl() {
            return normalizeBaseUrl(baseUrl);
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = normalizeBaseUrl(baseUrl);
        }

        public int getConnectTimeoutMs() {
            return connectTimeoutMs;
        }

        public void setConnectTimeoutMs(int connectTimeoutMs) {
            this.connectTimeoutMs = connectTimeoutMs;
        }

        public int getReadTimeoutMs() {
            return readTimeoutMs;
        }

        public void setReadTimeoutMs(int readTimeoutMs) {
            this.readTimeoutMs = readTimeoutMs;
        }

        private String normalizeBaseUrl(String value) {
            if (value == null || value.isBlank() || value.contains("://")) {
                return value;
            }
            return "http://" + value;
        }
    }

    public static class Internal {
        @NotBlank
        private String apiKey;

        @NotBlank
        private String headerName;

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getHeaderName() {
            return headerName;
        }

        public void setHeaderName(String headerName) {
            this.headerName = headerName;
        }
    }

    public static class Email {
        private boolean enabled;

        @NotBlank
        private String fromAddress = "no-reply@medibook.local";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getFromAddress() {
            return fromAddress;
        }

        public void setFromAddress(String fromAddress) {
            this.fromAddress = fromAddress;
        }
    }

    public static class Sms {
        private boolean enabled;

        private String providerUrl;

        private String authToken;

        @NotBlank
        private String senderId = "MediBook";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getProviderUrl() {
            return providerUrl;
        }

        public void setProviderUrl(String providerUrl) {
            this.providerUrl = providerUrl;
        }

        public String getAuthToken() {
            return authToken;
        }

        public void setAuthToken(String authToken) {
            this.authToken = authToken;
        }

        public String getSenderId() {
            return senderId;
        }

        public void setSenderId(String senderId) {
            this.senderId = senderId;
        }
    }

    public static class Reminders {
        @Min(1000)
        private long dispatchIntervalMs = 30_000L;

        public long getDispatchIntervalMs() {
            return dispatchIntervalMs;
        }

        public void setDispatchIntervalMs(long dispatchIntervalMs) {
            this.dispatchIntervalMs = dispatchIntervalMs;
        }
    }
}
