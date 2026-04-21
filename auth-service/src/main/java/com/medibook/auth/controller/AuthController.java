package com.medibook.auth.controller;

import com.medibook.auth.dto.request.ChangePasswordRequest;
import com.medibook.auth.dto.request.DeactivateAccountRequest;
import com.medibook.auth.dto.request.LoginRequest;
import com.medibook.auth.dto.request.RefreshTokenRequest;
import com.medibook.auth.dto.request.RegisterRequest;
import com.medibook.auth.dto.request.UpdateProfileRequest;
import com.medibook.auth.dto.request.ValidateTokenRequest;
import com.medibook.auth.dto.response.AuthResponse;
import com.medibook.auth.dto.response.MessageResponse;
import com.medibook.auth.dto.response.TokenValidationResponse;
import com.medibook.auth.dto.response.UserResponse;
import com.medibook.auth.service.AuthService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    @PostMapping("/logout")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('PATIENT','PROVIDER','ADMIN')")
    public ResponseEntity<MessageResponse> logout(Authentication authentication) {
        return ResponseEntity.ok(authService.logout(authentication.getName()));
    }

    @PostMapping("/validate")
    public ResponseEntity<TokenValidationResponse> validate(@Valid @RequestBody ValidateTokenRequest request) {
        return ResponseEntity.ok(authService.validateToken(request.accessToken()));
    }

    @GetMapping("/profile")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('PATIENT','PROVIDER','ADMIN')")
    public ResponseEntity<UserResponse> getProfile(Authentication authentication) {
        return ResponseEntity.ok(authService.getUserByEmail(authentication.getName()));
    }

    @PutMapping("/profile")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('PATIENT','PROVIDER','ADMIN')")
    public ResponseEntity<UserResponse> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(authService.updateProfile(authentication.getName(), request));
    }

    @PutMapping("/password")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('PATIENT','PROVIDER','ADMIN')")
    public ResponseEntity<MessageResponse> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request) {
        return ResponseEntity.ok(authService.changePassword(authentication.getName(), request));
    }

    @PutMapping("/deactivate")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('PATIENT','PROVIDER','ADMIN')")
    public ResponseEntity<MessageResponse> deactivateAccount(
            Authentication authentication,
            @RequestBody(required = false) DeactivateAccountRequest request) {
        DeactivateAccountRequest safeRequest = request == null ? new DeactivateAccountRequest(null) : request;
        return ResponseEntity.ok(authService.deactivateAccount(authentication.getName(), safeRequest));
    }
}
