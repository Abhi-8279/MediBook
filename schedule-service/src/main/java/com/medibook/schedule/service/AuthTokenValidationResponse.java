package com.medibook.schedule.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.medibook.schedule.enums.Role;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AuthTokenValidationResponse(
        boolean valid,
        String userId,
        String email,
        Role role,
        boolean active) {
}
