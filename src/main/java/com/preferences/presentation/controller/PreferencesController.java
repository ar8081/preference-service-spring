package com.preferences.presentation.controller;

import com.preferences.application.service.PreferencesService;
import com.preferences.domain.dto.PreferencesPatchRequest;
import com.preferences.domain.dto.PreferencesRequest;
import com.preferences.domain.dto.PreferencesResponse;
import com.preferences.infrastructure.filter.RateLimitFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/preferences")
public class PreferencesController {
    
    private final PreferencesService preferencesService;
    private final RateLimitFilter rateLimitFilter;

    public PreferencesController(PreferencesService preferencesService, RateLimitFilter rateLimitFilter) {
        this.preferencesService = preferencesService;
        this.rateLimitFilter = rateLimitFilter;
    }

    @GetMapping("/{memberId}")
    public ResponseEntity<PreferencesResponse> getPreferences(
            @PathVariable String memberId,
            HttpServletRequest request) {
        rateLimitFilter.checkRateLimit(memberId);
        PreferencesResponse response = preferencesService.getPreferences(memberId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{memberId}")
    public ResponseEntity<PreferencesResponse> upsertPreferences(
            @PathVariable String memberId,
            @Valid @RequestBody PreferencesRequest request,
            HttpServletRequest httpRequest) {
        rateLimitFilter.checkRateLimit(memberId);
        PreferencesResponse response = preferencesService.upsertPreferences(memberId, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{memberId}")
    public ResponseEntity<PreferencesResponse> patchPreferences(
            @PathVariable String memberId,
            @Valid @RequestBody PreferencesPatchRequest request,
            HttpServletRequest httpRequest) {
        rateLimitFilter.checkRateLimit(memberId);
        PreferencesResponse response = preferencesService.patchPreferences(memberId, request);
        return ResponseEntity.ok(response);
    }
}
