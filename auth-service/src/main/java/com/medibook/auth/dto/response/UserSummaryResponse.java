package com.medibook.auth.dto.response;

import com.medibook.auth.enums.AuthProvider;
import com.medibook.auth.enums.Role;

public record UserSummaryResponse(
        String userId,
        String fullName,
        String email,
        String phone,
        Role role,
        AuthProvider authProvider,
        boolean active,
        String profilePicUrl) {
}
