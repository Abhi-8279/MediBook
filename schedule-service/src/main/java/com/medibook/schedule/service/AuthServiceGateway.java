package com.medibook.schedule.service;

public interface AuthServiceGateway {

    AuthTokenValidationResponse validateAccessToken(String accessToken);
}
