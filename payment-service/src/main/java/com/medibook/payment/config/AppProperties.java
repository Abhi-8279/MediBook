package com.medibook.payment.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final DownstreamService authService = new DownstreamService();
    private final DownstreamService appointmentService = new DownstreamService();
    private final DownstreamService providerService = new DownstreamService();
    private final Internal internal = new Internal();
    private final Payment payment = new Payment();

    public DownstreamService getAuthService() {
        return authService;
    }

    public DownstreamService getAppointmentService() {
        return appointmentService;
    }

    public DownstreamService getProviderService() {
        return providerService;
    }

    public Internal getInternal() {
        return internal;
    }

    public Payment getPayment() {
        return payment;
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

    public static class Payment {
        @NotBlank
        private String defaultCurrency = "INR";
        private final Razorpay razorpay = new Razorpay();

        public String getDefaultCurrency() {
            return defaultCurrency;
        }

        public void setDefaultCurrency(String defaultCurrency) {
            this.defaultCurrency = defaultCurrency;
        }

        public Razorpay getRazorpay() {
            return razorpay;
        }
    }

    public static class Razorpay {
        private boolean enabled;
        private String keyId;
        private String keySecret;
        @NotBlank
        private String apiBaseUrl = "https://api.razorpay.com/v1";
        @Min(1)
        private int connectTimeoutMs = 5000;
        @Min(1)
        private int readTimeoutMs = 10000;
        @NotBlank
        private String checkoutName = "MediBook";
        @NotBlank
        private String checkoutDescription = "Appointment payment";
        private String checkoutImageUrl;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getKeyId() {
            return keyId;
        }

        public void setKeyId(String keyId) {
            this.keyId = keyId;
        }

        public String getKeySecret() {
            return keySecret;
        }

        public void setKeySecret(String keySecret) {
            this.keySecret = keySecret;
        }

        public String getApiBaseUrl() {
            return apiBaseUrl;
        }

        public void setApiBaseUrl(String apiBaseUrl) {
            this.apiBaseUrl = apiBaseUrl;
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

        public String getCheckoutName() {
            return checkoutName;
        }

        public void setCheckoutName(String checkoutName) {
            this.checkoutName = checkoutName;
        }

        public String getCheckoutDescription() {
            return checkoutDescription;
        }

        public void setCheckoutDescription(String checkoutDescription) {
            this.checkoutDescription = checkoutDescription;
        }

        public String getCheckoutImageUrl() {
            return checkoutImageUrl;
        }

        public void setCheckoutImageUrl(String checkoutImageUrl) {
            this.checkoutImageUrl = checkoutImageUrl;
        }
    }
}
