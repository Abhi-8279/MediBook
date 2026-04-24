package com.medibook.provider.service;

public interface AuthServiceGateway {

    AuthTokenValidationResponse validateAccessToken(String accessToken);

    AuthUserSummary getUserById(String userId);
}
