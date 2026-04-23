package com.medibook.notification.security;

import com.medibook.notification.enums.Role;

public record AuthenticatedUser(String userId, String email, Role role) {
}
