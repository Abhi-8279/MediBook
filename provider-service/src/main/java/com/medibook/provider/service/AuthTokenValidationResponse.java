package com.medibook.provider.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.medibook.provider.enums.Role;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AuthTokenValidationResponse(
        boolean valid,
        String userId,
        String email,
        Role role,
        boolean active) {
}
