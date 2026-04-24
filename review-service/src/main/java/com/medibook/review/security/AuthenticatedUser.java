package com.medibook.review.security;

import com.medibook.review.enums.Role;

public record AuthenticatedUser(String userId, String email, Role role) {
}
