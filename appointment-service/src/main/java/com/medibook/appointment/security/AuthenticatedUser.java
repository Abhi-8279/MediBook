package com.medibook.appointment.security;

import com.medibook.appointment.enums.Role;

public record AuthenticatedUser(String userId, String email, Role role) {
}
