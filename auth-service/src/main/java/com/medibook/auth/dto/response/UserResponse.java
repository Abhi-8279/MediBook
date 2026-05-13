package com.medibook.auth.dto.response;

import com.medibook.auth.enums.AuthProvider;
import com.medibook.auth.enums.Role;
import java.time.Instant;

public record UserResponse(
        String userId,
        String fullName,
        String email,
        String phone,
        Role role,
        AuthProvider authProvider,
        boolean active,
        Instant createdAt,
        String profilePicUrl) {
}

