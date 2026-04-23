package com.medibook.auth.controller;

import com.medibook.auth.dto.request.ValidateTokenRequest;
import com.medibook.auth.dto.response.TokenValidationResponse;
import com.medibook.auth.dto.response.UserSummaryResponse;
import com.medibook.auth.enums.Role;
import com.medibook.auth.service.AuthService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/internal")
public class InternalAuthController {

    private final AuthService authService;

    public InternalAuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/tokens/validate")
    public ResponseEntity<TokenValidationResponse> validateToken(@Valid @RequestBody ValidateTokenRequest request) {
        return ResponseEntity.ok(authService.validateToken(request.accessToken()));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<UserSummaryResponse> getUserById(@PathVariable String userId) {
        return ResponseEntity.ok(authService.getUserSummaryById(userId));
    }

    @GetMapping("/users/by-email")
    public ResponseEntity<UserSummaryResponse> getUserByEmail(@RequestParam String email) {
        return ResponseEntity.ok(authService.getUserSummaryByEmail(email));
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserSummaryResponse>> getUsers(@RequestParam(required = false) Role role) {
        return ResponseEntity.ok(authService.getUsers(role));
    }
}
