package com.medibook.review.service;

public interface AuthServiceGateway {

    AuthTokenValidationResponse validateAccessToken(String accessToken);
}
