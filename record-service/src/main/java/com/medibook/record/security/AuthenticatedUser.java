package com.medibook.record.security;

import com.medibook.record.enums.Role;

public record AuthenticatedUser(String userId, String email, Role role) {
}
