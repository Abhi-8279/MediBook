package com.medibook.payment.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.medibook.payment.enums.Role;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AuthTokenValidationResponse(
        boolean valid,
        String userId,
        String email,
        Role role,
        boolean active) {
}
