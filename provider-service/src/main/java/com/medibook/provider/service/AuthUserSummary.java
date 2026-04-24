package com.medibook.provider.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.medibook.provider.enums.Role;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AuthUserSummary(
        String userId,
        String fullName,
        String email,
        String phone,
        Role role,
        boolean active,
        String profilePicUrl) {
}
