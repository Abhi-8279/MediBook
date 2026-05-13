package com.medibook.auth.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Jwt jwt = new Jwt();
    private final OAuth2 oauth2 = new OAuth2();
    private final Admin admin = new Admin();
    private final Internal internal = new Internal();
    private final NotificationService notificationService = new NotificationService();
    private final PasswordReset passwordReset = new PasswordReset();

    public Jwt getJwt() {
        return jwt;
    }

    public OAuth2 getOauth2() {
        return oauth2;
    }

    public Admin getAdmin() {
        return admin;
    }

    public Internal getInternal() {
        return internal;
    }

    public NotificationService getNotificationService() {
        return notificationService;
    }

    public PasswordReset getPasswordReset() {
        return passwordReset;
    }

    public static class Jwt {
        @NotBlank
        private String secret;
        private long accessTokenExpirationMs;
        private long refreshTokenExpirationMs;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public long getAccessTokenExpirationMs() {
            return accessTokenExpirationMs;
        }

        public void setAccessTokenExpirationMs(long accessTokenExpirationMs) {
            this.accessTokenExpirationMs = accessTokenExpirationMs;
        }

        public long getRefreshTokenExpirationMs() {
            return refreshTokenExpirationMs;
        }

        public void setRefreshTokenExpirationMs(long refreshTokenExpirationMs) {
            this.refreshTokenExpirationMs = refreshTokenExpirationMs;
        }
    }

    public static class OAuth2 {
        @NotEmpty
        private List<String> authorizedRedirectUris = new ArrayList<>();
        private final Google google = new Google();

        public List<String> getAuthorizedRedirectUris() {
            return authorizedRedirectUris;
        }

        public void setAuthorizedRedirectUris(List<String> authorizedRedirectUris) {
            this.authorizedRedirectUris = authorizedRedirectUris;
        }

        public Google getGoogle() {
            return google;
        }
    }

    public static class Google {
        private String clientId;
        private String clientSecret;
        private String redirectUri;

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }

        public String getRedirectUri() {
            return redirectUri;
        }

        public void setRedirectUri(String redirectUri) {
            this.redirectUri = redirectUri;
        }
    }

    public static class Admin {
        @NotBlank
        private String email;
        @NotBlank
        private String password;
        @NotBlank
        private String fullName;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
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

    public static class NotificationService {
        @NotBlank
        private String baseUrl = "http://localhost:8087";
        private int connectTimeoutMs = 3000;
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

    public static class PasswordReset {
        @NotBlank
        private String frontendBaseUrl = "http://localhost:5173/reset-password";
        private long expirationMs = 1_800_000;

        public String getFrontendBaseUrl() {
            return frontendBaseUrl;
        }

        public void setFrontendBaseUrl(String frontendBaseUrl) {
            this.frontendBaseUrl = frontendBaseUrl;
        }

        public long getExpirationMs() {
            return expirationMs;
        }

        public void setExpirationMs(long expirationMs) {
            this.expirationMs = expirationMs;
        }
    }
}
