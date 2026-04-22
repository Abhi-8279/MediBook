package com.medibook.record.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.medibook.record.enums.Role;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AuthTokenValidationResponse(
        boolean valid,
        String userId,
        String email,
        Role role,
        boolean active) {
}
