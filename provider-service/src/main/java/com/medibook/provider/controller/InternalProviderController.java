package com.medibook.provider.controller;

import com.medibook.provider.dto.request.ProviderAvailabilityUpdateRequest;
import com.medibook.provider.dto.request.ProviderRatingUpdateRequest;
import com.medibook.provider.dto.response.ProviderResponse;
import com.medibook.provider.dto.response.SpecializationCountResponse;
import com.medibook.provider.service.ProviderService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/providers/internal")
public class InternalProviderController {

    private final ProviderService providerService;

    public InternalProviderController(ProviderService providerService) {
        this.providerService = providerService;
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<ProviderResponse> getProviderByUserId(@PathVariable String userId) {
        return ResponseEntity.ok(providerService.getProviderByUserId(userId));
    }

    @PutMapping("/{providerId}/rating")
    public ResponseEntity<ProviderResponse> updateRating(
            @PathVariable String providerId,
            @Valid @RequestBody ProviderRatingUpdateRequest request) {
        return ResponseEntity.ok(providerService.updateRating(providerId, request));
    }

    @PutMapping("/{providerId}/availability")
    public ResponseEntity<ProviderResponse> updateAvailability(
            @PathVariable String providerId,
            @Valid @RequestBody ProviderAvailabilityUpdateRequest request) {
        return ResponseEntity.ok(providerService.updateAvailabilityInternally(providerId, request));
    }

    @GetMapping("/specialization-counts")
    public ResponseEntity<List<SpecializationCountResponse>> getSpecializationCounts() {
        return ResponseEntity.ok(providerService.getSpecializationCounts());
    }
}
