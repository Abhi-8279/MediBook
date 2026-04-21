package com.medibook.auth.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.medibook.auth.config.AppProperties;
import com.medibook.auth.entity.User;
import com.medibook.auth.enums.AuthProvider;
import com.medibook.auth.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        AppProperties properties = new AppProperties();
        properties.getJwt().setSecret("medibook-auth-secret-key-change-before-production-123456");
        properties.getJwt().setAccessTokenExpirationMs(86_400_000);
        properties.getJwt().setRefreshTokenExpirationMs(604_800_000);
        jwtService = new JwtService(properties);
    }

    @Test
    void shouldGenerateAndParseAccessToken() {
        User user = new User();
        user.setUserId("user-123");
        user.setEmail("patient@medibook.com");
        user.setRole(Role.PATIENT);
        user.setAuthProvider(AuthProvider.LOCAL);

        String token = jwtService.generateAccessToken(user);

        assertThat(jwtService.extractUsername(token)).isEqualTo("patient@medibook.com");
        assertThat(jwtService.extractUserId(token)).isEqualTo("user-123");
        assertThat(jwtService.extractRole(token)).isEqualTo(Role.PATIENT);
    }
}

