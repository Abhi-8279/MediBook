package com.medibook.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.medibook.auth.config.AppProperties;
import com.medibook.auth.dto.request.RegisterRequest;
import com.medibook.auth.dto.response.AuthResponse;
import com.medibook.auth.entity.RefreshToken;
import com.medibook.auth.entity.User;
import com.medibook.auth.enums.AuthProvider;
import com.medibook.auth.enums.Role;
import com.medibook.auth.exception.DuplicateResourceException;
import com.medibook.auth.repository.RefreshTokenRepository;
import com.medibook.auth.repository.UserRepository;
import com.medibook.auth.security.JwtService;
import com.medibook.auth.service.impl.AuthServiceImpl;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AppProperties appProperties;

    private AuthServiceImpl authService;

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        appProperties.getJwt().setSecret("medibook-auth-secret-key-change-before-production-123456");
        appProperties.getJwt().setAccessTokenExpirationMs(86_400_000);
        appProperties.getJwt().setRefreshTokenExpirationMs(604_800_000);
        jwtService = new JwtService(appProperties);
        authService = new AuthServiceImpl(
                userRepository,
                refreshTokenRepository,
                passwordEncoder,
                jwtService,
                appProperties);
    }

    @Test
    void shouldRegisterLocalPatientAndIssueTokens() {
        RegisterRequest request = new RegisterRequest(
                "Asha Kumar",
                "asha@medibook.com",
                "SecurePass123",
                "+919999999999",
                Role.PATIENT,
                null);

        when(userRepository.existsByEmail("asha@medibook.com")).thenReturn(false);
        when(userRepository.findByPhone("+919999999999")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("SecurePass123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> {
            RefreshToken token = invocation.getArgument(0);
            token.setId(1L);
            token.setCreatedAt(Instant.now());
            return token;
        });

        AuthResponse response = authService.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getEmail()).isEqualTo("asha@medibook.com");
        assertThat(savedUser.getPasswordHash()).isEqualTo("encoded-password");
        assertThat(savedUser.getRole()).isEqualTo(Role.PATIENT);
        assertThat(savedUser.getAuthProvider()).isEqualTo(AuthProvider.LOCAL);

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(response.user().email()).isEqualTo("asha@medibook.com");
    }

    @Test
    void shouldRejectDuplicateEmailRegistration() {
        RegisterRequest request = new RegisterRequest(
                "Asha Kumar",
                "asha@medibook.com",
                "SecurePass123",
                null,
                Role.PATIENT,
                null);

        when(userRepository.existsByEmail("asha@medibook.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");
    }
}
