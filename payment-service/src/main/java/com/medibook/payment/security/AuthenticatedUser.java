package com.medibook.payment.security;

import com.medibook.payment.enums.Role;

public record AuthenticatedUser(String userId, String email, Role role) {
}
