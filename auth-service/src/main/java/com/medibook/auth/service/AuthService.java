package com.medibook.auth.service;

import com.medibook.auth.dto.request.ChangePasswordRequest;
import com.medibook.auth.dto.request.DeactivateAccountRequest;
import com.medibook.auth.dto.request.ForgotPasswordRequest;
import com.medibook.auth.dto.request.LoginRequest;
import com.medibook.auth.dto.request.RefreshTokenRequest;
import com.medibook.auth.dto.request.RegisterRequest;
import com.medibook.auth.dto.request.ResetPasswordRequest;
import com.medibook.auth.dto.request.UpdateProfileRequest;
import com.medibook.auth.dto.request.UserStatusUpdateRequest;
import com.medibook.auth.dto.response.AuthResponse;
import com.medibook.auth.dto.response.MessageResponse;
import com.medibook.auth.dto.response.TokenValidationResponse;
import com.medibook.auth.dto.response.UserResponse;
import com.medibook.auth.dto.response.UserSummaryResponse;
import com.medibook.auth.enums.AuthProvider;
import com.medibook.auth.enums.Role;
import java.util.Map;
import java.util.List;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    MessageResponse requestPasswordReset(ForgotPasswordRequest request);

    MessageResponse resetPassword(ResetPasswordRequest request);

    MessageResponse logout(String email);

    TokenValidationResponse validateToken(String accessToken);

    AuthResponse refreshToken(RefreshTokenRequest request);

    UserResponse getUserByEmail(String email);

    UserResponse getUserById(String userId);

    UserResponse updateProfile(String email, UpdateProfileRequest request);

    MessageResponse changePassword(String email, ChangePasswordRequest request);

    MessageResponse deactivateAccount(String email, DeactivateAccountRequest request);

    List<UserSummaryResponse> getUsers(Role role);

    UserSummaryResponse getUserSummaryById(String userId);

    UserSummaryResponse getUserSummaryByEmail(String email);

    UserResponse updateUserStatus(String userId, UserStatusUpdateRequest request);

    AuthResponse handleOAuthLogin(AuthProvider provider, Role requestedRole, Map<String, Object> attributes);
}
