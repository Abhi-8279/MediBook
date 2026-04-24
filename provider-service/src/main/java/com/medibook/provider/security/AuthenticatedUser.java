package com.medibook.provider.security;

import com.medibook.provider.enums.Role;

public record AuthenticatedUser(
        String userId,
        String email,
        Role role) {
}
