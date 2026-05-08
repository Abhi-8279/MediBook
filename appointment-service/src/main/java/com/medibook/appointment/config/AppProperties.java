package com.medibook.appointment.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final DownstreamService authService = new DownstreamService();
    private final DownstreamService providerService = new DownstreamService();
    private final DownstreamService scheduleService = new DownstreamService();
    private final DownstreamService paymentService = new DownstreamService();
    private final DownstreamService notificationService = new DownstreamService();
    private final Internal internal = new Internal();
    private final Cancellation cancellation = new Cancellation();

    public DownstreamService getAuthService() {
        return authService;
    }

    public DownstreamService getProviderService() {
        return providerService;
    }

    public DownstreamService getScheduleService() {
        return scheduleService;
    }

    public DownstreamService getPaymentService() {
        return paymentService;
    }

    public DownstreamService getNotificationService() {
        return notificationService;
    }

    public Internal getInternal() {
        return internal;
    }

    public Cancellation getCancellation() {
        return cancellation;
    }

    public static class DownstreamService {
        @NotBlank
        private String baseUrl;

        @Min(1)
        private int connectTimeoutMs = 3000;

        @Min(1)
        private int readTimeoutMs = 5000;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
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

    public static class Cancellation {
        @Min(0)
        private int refundNoticeHours = 24;

        public int getRefundNoticeHours() {
            return refundNoticeHours;
        }

        public void setRefundNoticeHours(int refundNoticeHours) {
            this.refundNoticeHours = refundNoticeHours;
        }
    }
}
