package com.medibook.auth.dto.response;

import com.medibook.auth.enums.AuthProvider;
import com.medibook.auth.enums.Role;

public record TokenValidationResponse(
        boolean valid,
        String userId,
        String email,
        Role role,
        AuthProvider authProvider,
        boolean active) {
}
