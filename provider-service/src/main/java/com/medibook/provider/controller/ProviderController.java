package com.medibook.provider.controller;

import com.medibook.provider.dto.request.ProviderAvailabilityUpdateRequest;
import com.medibook.provider.dto.request.ProviderVerificationRequest;
import com.medibook.provider.dto.request.RegisterProviderRequest;
import com.medibook.provider.dto.request.UpdateProviderRequest;
import com.medibook.provider.dto.response.MessageResponse;
import com.medibook.provider.dto.response.ProviderResponse;
import com.medibook.provider.dto.response.ProviderSummaryResponse;
import com.medibook.provider.dto.response.SpecializationCountResponse;
import com.medibook.provider.security.AuthenticatedUser;
import com.medibook.provider.service.ProviderService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/providers")
public class ProviderController {

    private final ProviderService providerService;

    public ProviderController(ProviderService providerService) {
        this.providerService = providerService;
    }

    @PostMapping("/register")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<ProviderResponse> registerProvider(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody RegisterProviderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(providerService.registerProvider(authenticatedUser, request));
    }

    @GetMapping
    public ResponseEntity<List<ProviderSummaryResponse>> getProviders(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String specialization,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) Boolean available,
            @RequestParam(required = false) Boolean verified) {
        return ResponseEntity.ok(providerService.searchProviders(
                search,
                specialization,
                location,
                available,
                verified,
                authenticatedUser));
    }

    @GetMapping("/all")
    public ResponseEntity<List<ProviderSummaryResponse>> getAllProviders(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam(required = false) Boolean available,
            @RequestParam(required = false) Boolean verified) {
        return ResponseEntity.ok(providerService.searchProviders(
                null,
                null,
                null,
                available,
                verified,
                authenticatedUser));
    }

    @GetMapping("/search")
    public ResponseEntity<List<ProviderSummaryResponse>> searchProviders(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam String q,
            @RequestParam(required = false) String specialization,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) Boolean available,
            @RequestParam(required = false) Boolean verified) {
        return ResponseEntity.ok(providerService.searchProviders(
                q,
                specialization,
                location,
                available,
                verified,
                authenticatedUser));
    }

    @GetMapping("/specialization/{specialization}")
    public ResponseEntity<List<ProviderSummaryResponse>> getBySpecialization(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable String specialization,
            @RequestParam(required = false) Boolean available,
            @RequestParam(required = false) Boolean verified) {
        return ResponseEntity.ok(providerService.searchProviders(
                null,
                specialization,
                null,
                available,
                verified,
                authenticatedUser));
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<ProviderResponse> getMyProviderProfile(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return ResponseEntity.ok(providerService.getMyProviderProfile(authenticatedUser));
    }

    @PostMapping("/me/sync-auth-profile")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<ProviderResponse> syncAuthProfile(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return ResponseEntity.ok(providerService.syncAuthProfile(authenticatedUser));
    }

    @PutMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<ProviderResponse> updateMyProviderProfile(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody UpdateProviderRequest request) {
        return ResponseEntity.ok(providerService.updateMyProviderProfile(authenticatedUser, request));
    }

    @GetMapping("/admin/specialization-counts")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SpecializationCountResponse>> getAdminSpecializationCounts() {
        return ResponseEntity.ok(providerService.getSpecializationCounts());
    }

    @GetMapping("/{providerId}")
    public ResponseEntity<ProviderResponse> getProviderById(
            @PathVariable String providerId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return ResponseEntity.ok(providerService.getProviderById(providerId, authenticatedUser));
    }

    @PutMapping("/{providerId}/verify")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProviderResponse> verifyProvider(
            @PathVariable String providerId,
            @Valid @RequestBody ProviderVerificationRequest request) {
        return ResponseEntity.ok(providerService.verifyProvider(providerId, request));
    }

    @PutMapping("/{providerId}/availability")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('PROVIDER','ADMIN')")
    public ResponseEntity<ProviderResponse> updateAvailability(
            @PathVariable String providerId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody ProviderAvailabilityUpdateRequest request) {
        return ResponseEntity.ok(providerService.updateAvailability(providerId, authenticatedUser, request));
    }

    @DeleteMapping("/{providerId}")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> deleteProvider(@PathVariable String providerId) {
        providerService.deleteProvider(providerId);
        return ResponseEntity.ok(new MessageResponse("Provider profile deleted successfully"));
    }
}
