package com.medibook.auth.service.impl;

import com.medibook.auth.config.AppProperties;
import com.medibook.auth.dto.request.ChangePasswordRequest;
import com.medibook.auth.dto.request.DeactivateAccountRequest;
import com.medibook.auth.dto.request.LoginRequest;
import com.medibook.auth.dto.request.RefreshTokenRequest;
import com.medibook.auth.dto.request.RegisterRequest;
import com.medibook.auth.dto.request.UpdateProfileRequest;
import com.medibook.auth.dto.request.UserStatusUpdateRequest;
import com.medibook.auth.dto.response.AuthResponse;
import com.medibook.auth.dto.response.MessageResponse;
import com.medibook.auth.dto.response.TokenValidationResponse;
import com.medibook.auth.dto.response.UserResponse;
import com.medibook.auth.dto.response.UserSummaryResponse;
import com.medibook.auth.entity.RefreshToken;
import com.medibook.auth.entity.User;
import com.medibook.auth.enums.AuthProvider;
import com.medibook.auth.enums.Role;
import com.medibook.auth.exception.AccountInactiveException;
import com.medibook.auth.exception.DuplicateResourceException;
import com.medibook.auth.exception.InvalidCredentialsException;
import com.medibook.auth.exception.ResourceNotFoundException;
import com.medibook.auth.exception.TokenValidationException;
import com.medibook.auth.repository.RefreshTokenRepository;
import com.medibook.auth.repository.UserRepository;
import com.medibook.auth.security.JwtService;
import com.medibook.auth.service.AuthService;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AppProperties appProperties;

    public AuthServiceImpl(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AppProperties appProperties) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.appProperties = appProperties;
    }

    @Override
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("An account already exists for this email");
        }

        validatePhoneUniqueness(request.phone(), null);

        if (request.role() == Role.ADMIN) {
            throw new IllegalArgumentException("Public registration for admin accounts is not allowed");
        }

        User user = new User();
        user.setUserId(UUID.randomUUID().toString());
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setPhone(normalizePhone(request.phone()));
        user.setRole(request.role());
        user.setAuthProvider(AuthProvider.LOCAL);
        user.setActive(true);
        user.setProfilePicUrl(blankToNull(request.profilePicUrl()));

        User savedUser = userRepository.save(user);
        return buildAuthResponse(savedUser, issueRefreshToken(savedUser));
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = findUserByEmailOrThrow(normalizeEmail(request.email()));
        assertActive(user);

        if (!StringUtils.hasText(user.getPasswordHash())) {
            throw new InvalidCredentialsException("This account was created with OAuth2. Please use Google or GitHub login.");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        return buildAuthResponse(user, issueRefreshToken(user));
    }

    @Override
    public MessageResponse logout(String email) {
        User user = findUserByEmailOrThrow(normalizeEmail(email));
        revokeAllTokens(user);
        return new MessageResponse("Logout successful");
    }

    @Override
    @Transactional(readOnly = true)
    public TokenValidationResponse validateToken(String accessToken) {
        String email = jwtService.extractUsername(accessToken);
        User user = findUserByEmailOrThrow(email);
        assertActive(user);
        return new TokenValidationResponse(
                true,
                user.getUserId(),
                user.getEmail(),
                user.getRole(),
                user.getAuthProvider(),
                user.isActive());
    }

    @Override
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> new TokenValidationException("Refresh token is invalid"));

        if (refreshToken.isRevoked() || refreshToken.getExpiresAt().isBefore(Instant.now())) {
            throw new TokenValidationException("Refresh token is expired or revoked");
        }

        User user = refreshToken.getUser();
        assertActive(user);

        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        return buildAuthResponse(user, issueRefreshToken(user));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {
        User user = findUserByEmailOrThrow(normalizeEmail(email));
        return toUserResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(String userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return toUserResponse(user);
    }

    @Override
    public UserResponse updateProfile(String email, UpdateProfileRequest request) {
        User user = findUserByEmailOrThrow(normalizeEmail(email));
        assertActive(user);

        validatePhoneUniqueness(request.phone(), user.getUserId());

        if (StringUtils.hasText(request.fullName())) {
            user.setFullName(request.fullName().trim());
        }
        if (request.phone() != null) {
            user.setPhone(normalizePhone(request.phone()));
        }
        if (request.profilePicUrl() != null) {
            user.setProfilePicUrl(blankToNull(request.profilePicUrl()));
        }

        return toUserResponse(userRepository.save(user));
    }

    @Override
    public MessageResponse changePassword(String email, ChangePasswordRequest request) {
        User user = findUserByEmailOrThrow(normalizeEmail(email));
        assertActive(user);

        if (!isLocalUser(user)) {
            throw new IllegalStateException("Password changes are only available for local accounts");
        }

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Current password is incorrect");
        }

        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("New password must be different from the current password");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        revokeAllTokens(user);
        return new MessageResponse("Password changed successfully. Please log in again.");
    }

    @Override
    public MessageResponse deactivateAccount(String email, DeactivateAccountRequest request) {
        User user = findUserByEmailOrThrow(normalizeEmail(email));
        assertActive(user);

        if (isLocalUser(user)) {
            if (!StringUtils.hasText(request.passwordConfirmation())
                    || !passwordEncoder.matches(request.passwordConfirmation(), user.getPasswordHash())) {
                throw new InvalidCredentialsException("Password confirmation is required to deactivate this account");
            }
        }

        user.setActive(false);
        userRepository.save(user);
        revokeAllTokens(user);
        return new MessageResponse("Account deactivated successfully");
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSummaryResponse> getUsers(Role role) {
        List<User> users = role == null ? userRepository.findAll() : userRepository.findAllByRole(role);
        return users.stream()
                .map(this::toUserSummaryResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UserSummaryResponse getUserSummaryById(String userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return toUserSummaryResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserSummaryResponse getUserSummaryByEmail(String email) {
        return toUserSummaryResponse(findUserByEmailOrThrow(normalizeEmail(email)));
    }

    @Override
    public UserResponse updateUserStatus(String userId, UserStatusUpdateRequest request) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setActive(request.active());
        userRepository.save(user);
        if (!user.isActive()) {
            revokeAllTokens(user);
        }
        return toUserResponse(user);
    }

    @Override
    public AuthResponse handleOAuthLogin(AuthProvider provider, Map<String, Object> attributes) {
        OAuthUserInfo userInfo = extractOAuthUserInfo(provider, attributes);
        User user = userRepository.findByEmail(userInfo.email())
                .map(existingUser -> updateOauthUser(existingUser, provider, userInfo))
                .orElseGet(() -> createOauthUser(provider, userInfo));
        assertActive(user);
        return buildAuthResponse(user, issueRefreshToken(user));
    }

    private User createOauthUser(AuthProvider provider, OAuthUserInfo userInfo) {
        User user = new User();
        user.setUserId(UUID.randomUUID().toString());
        user.setFullName(userInfo.fullName());
        user.setEmail(userInfo.email());
        user.setRole(Role.PATIENT);
        user.setAuthProvider(provider);
        user.setProfilePicUrl(userInfo.profilePicUrl());
        user.setActive(true);
        return userRepository.save(user);
    }

    private User updateOauthUser(User user, AuthProvider provider, OAuthUserInfo userInfo) {
        user.setAuthProvider(provider);
        if (StringUtils.hasText(userInfo.fullName())) {
            user.setFullName(userInfo.fullName());
        }
        if (StringUtils.hasText(userInfo.profilePicUrl())) {
            user.setProfilePicUrl(userInfo.profilePicUrl());
        }
        return userRepository.save(user);
    }

    private OAuthUserInfo extractOAuthUserInfo(AuthProvider provider, Map<String, Object> attributes) {
        if (provider == AuthProvider.GITHUB) {
            String login = stringValue(attributes.get("login"));
            String email = normalizeEmail(Optional.ofNullable(stringValue(attributes.get("email")))
                    .filter(StringUtils::hasText)
                    .orElse(login + "@users.noreply.github.com"));
            String fullName = Optional.ofNullable(stringValue(attributes.get("name")))
                    .filter(StringUtils::hasText)
                    .orElse(login);
            return new OAuthUserInfo(email, fullName, stringValue(attributes.get("avatar_url")));
        }

        String email = normalizeEmail(stringValue(attributes.get("email")));
        String fullName = stringValue(attributes.get("name"));
        return new OAuthUserInfo(email, fullName, stringValue(attributes.get("picture")));
    }

    private AuthResponse buildAuthResponse(User user, String refreshToken) {
        String accessToken = jwtService.generateAccessToken(user);
        return new AuthResponse(
                accessToken,
                refreshToken,
                "Bearer",
                jwtService.getAccessTokenExpirationMs() / 1000,
                toUserResponse(user));
    }

    private String issueRefreshToken(User user) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setUser(user);
        refreshToken.setExpiresAt(Instant.now().plusMillis(appProperties.getJwt().getRefreshTokenExpirationMs()));
        refreshToken.setRevoked(false);
        return refreshTokenRepository.save(refreshToken).getToken();
    }

    private void revokeAllTokens(User user) {
        var activeTokens = refreshTokenRepository.findAllByUserAndRevokedFalse(user);
        activeTokens.forEach(token -> token.setRevoked(true));
        refreshTokenRepository.saveAll(activeTokens);
    }

    private void validatePhoneUniqueness(String phone, String currentUserId) {
        String normalizedPhone = normalizePhone(phone);
        if (!StringUtils.hasText(normalizedPhone)) {
            return;
        }

        userRepository.findByPhone(normalizedPhone)
                .filter(existing -> !existing.getUserId().equals(currentUserId))
                .ifPresent(existing -> {
                    throw new DuplicateResourceException("This phone number is already linked to another account");
                });
    }

    private User findUserByEmailOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private void assertActive(User user) {
        if (!user.isActive()) {
            throw new AccountInactiveException("This account is deactivated. Please contact support.");
        }
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getUserId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.getAuthProvider(),
                user.isActive(),
                user.getCreatedAt(),
                user.getProfilePicUrl());
    }

    private UserSummaryResponse toUserSummaryResponse(User user) {
        return new UserSummaryResponse(
                user.getUserId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.getAuthProvider(),
                user.isActive(),
                user.getProfilePicUrl());
    }

    private boolean isLocalUser(User user) {
        return user.getAuthProvider() == AuthProvider.LOCAL;
    }

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new IllegalArgumentException("Email is required");
        }
        return email.trim().toLowerCase();
    }

    private String normalizePhone(String phone) {
        if (phone == null) {
            return null;
        }
        String trimmed = phone.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private record OAuthUserInfo(String email, String fullName, String profilePicUrl) {
    }
}
