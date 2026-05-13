package com.medibook.notification.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.medibook.notification.enums.Role;

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
