package com.medibook.schedule.security;

import com.medibook.schedule.enums.Role;

public record AuthenticatedUser(String userId, String email, Role role) {
}
