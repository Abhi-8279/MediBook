package com.medibook.payment.service;

public interface AuthServiceGateway {

    AuthTokenValidationResponse validateAccessToken(String accessToken);
}
