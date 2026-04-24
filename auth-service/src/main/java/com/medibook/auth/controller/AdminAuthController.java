package com.medibook.auth.controller;

import com.medibook.auth.dto.request.UserStatusUpdateRequest;
import com.medibook.auth.dto.response.UserResponse;
import com.medibook.auth.dto.response.UserSummaryResponse;
import com.medibook.auth.enums.Role;
import com.medibook.auth.service.AuthService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/admin")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAuthController {

    private final AuthService authService;

    public AdminAuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserSummaryResponse>> getUsers(@RequestParam(required = false) Role role) {
        return ResponseEntity.ok(authService.getUsers(role));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<UserSummaryResponse> getUserById(@PathVariable String userId) {
        return ResponseEntity.ok(authService.getUserSummaryById(userId));
    }

    @PatchMapping("/users/{userId}/status")
    public ResponseEntity<UserResponse> updateUserStatus(
            @PathVariable String userId,
            @Valid @RequestBody UserStatusUpdateRequest request) {
        return ResponseEntity.ok(authService.updateUserStatus(userId, request));
    }
}

