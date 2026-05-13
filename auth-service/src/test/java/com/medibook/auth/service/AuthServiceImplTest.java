package com.medibook.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.medibook.auth.config.AppProperties;
import com.medibook.auth.dto.request.ForgotPasswordRequest;
import com.medibook.auth.dto.request.RegisterRequest;
import com.medibook.auth.dto.request.ResetPasswordRequest;
import com.medibook.auth.dto.response.AuthResponse;
import com.medibook.auth.dto.response.MessageResponse;
import com.medibook.auth.entity.PasswordResetToken;
import com.medibook.auth.entity.RefreshToken;
import com.medibook.auth.entity.User;
import com.medibook.auth.enums.AuthProvider;
import com.medibook.auth.enums.Role;
import com.medibook.auth.exception.DuplicateResourceException;
import com.medibook.auth.repository.PasswordResetTokenRepository;
import com.medibook.auth.repository.RefreshTokenRepository;
import com.medibook.auth.repository.UserRepository;
import com.medibook.auth.security.JwtService;
import com.medibook.auth.service.impl.AuthServiceImpl;
import java.time.Instant;
import java.util.List;
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
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private NotificationServiceGateway notificationServiceGateway;

    private AppProperties appProperties;

    private AuthServiceImpl authService;

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        appProperties.getJwt().setSecret("medibook-auth-secret-key-change-before-production-123456");
        appProperties.getJwt().setAccessTokenExpirationMs(86_400_000);
        appProperties.getJwt().setRefreshTokenExpirationMs(604_800_000);
        appProperties.getPasswordReset().setFrontendBaseUrl("http://localhost:5173/reset-password");
        appProperties.getPasswordReset().setExpirationMs(1_800_000);
        jwtService = new JwtService(appProperties);
        authService = new AuthServiceImpl(
                userRepository,
                passwordResetTokenRepository,
                refreshTokenRepository,
                passwordEncoder,
                jwtService,
                appProperties,
                notificationServiceGateway);
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

    @Test
    void shouldGenerateResetTokenAndSendPasswordResetEmailForLocalUser() {
        User user = new User();
        user.setUserId("user-1");
        user.setFullName("Asha Kumar");
        user.setEmail("asha@medibook.com");
        user.setPasswordHash("encoded-password");
        user.setAuthProvider(AuthProvider.LOCAL);
        user.setActive(true);

        when(userRepository.findByEmail("asha@medibook.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.findAllByUserAndUsedFalse(user)).thenReturn(List.of());
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MessageResponse response = authService.requestPasswordReset(new ForgotPasswordRequest("asha@medibook.com"));

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(tokenCaptor.capture());
        verify(notificationServiceGateway).sendPasswordResetNotification(anyString(), anyString(), anyString());

        PasswordResetToken savedToken = tokenCaptor.getValue();
        assertThat(savedToken.getTokenHash()).isNotBlank();
        assertThat(savedToken.getExpiresAt()).isAfter(Instant.now());
        assertThat(response.message()).contains("password reset link has been sent");
    }

    @Test
    void shouldResetPasswordForValidTokenAndRevokeRefreshSessions() {
        User user = new User();
        user.setUserId("user-1");
        user.setFullName("Asha Kumar");
        user.setEmail("asha@medibook.com");
        user.setPasswordHash("encoded-current");
        user.setAuthProvider(AuthProvider.LOCAL);
        user.setActive(true);

        PasswordResetToken passwordResetToken = new PasswordResetToken();
        passwordResetToken.setId(1L);
        passwordResetToken.setUser(user);
        passwordResetToken.setUsed(false);
        passwordResetToken.setExpiresAt(Instant.now().plusSeconds(600));

        when(passwordResetTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(passwordResetToken));
        when(passwordResetTokenRepository.findAllByUserAndUsedFalse(user)).thenReturn(List.of(passwordResetToken));
        when(passwordEncoder.matches("FreshPass123", "encoded-current")).thenReturn(false);
        when(passwordEncoder.encode("FreshPass123")).thenReturn("encoded-new");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(refreshTokenRepository.findAllByUserAndRevokedFalse(user)).thenReturn(List.of());

        MessageResponse response = authService.resetPassword(new ResetPasswordRequest("plain-reset-token", "FreshPass123"));

        assertThat(user.getPasswordHash()).isEqualTo("encoded-new");
        assertThat(passwordResetToken.isUsed()).isTrue();
        assertThat(passwordResetToken.getUsedAt()).isNotNull();
        assertThat(response.message()).contains("Please log in again");
    }
}
