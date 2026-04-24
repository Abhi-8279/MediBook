package com.medibook.notification.service;

import com.medibook.notification.enums.Role;
import java.util.List;

public interface AuthServiceGateway {

    AuthTokenValidationResponse validateAccessToken(String accessToken);

    AuthUserSummary getUserById(String userId);

    List<AuthUserSummary> getUsers(Role role);
}
