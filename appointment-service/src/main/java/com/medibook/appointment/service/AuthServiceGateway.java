package com.medibook.appointment.service;

public interface AuthServiceGateway {

    AuthTokenValidationResponse validateAccessToken(String accessToken);
}
