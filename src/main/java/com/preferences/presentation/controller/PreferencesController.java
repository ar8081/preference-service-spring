package com.preferences.presentation.controller;

import com.preferences.application.service.PreferencesService;
import com.preferences.domain.dto.PreferencesPatchRequest;
import com.preferences.domain.dto.PreferencesRequest;
import com.preferences.domain.dto.PreferencesResponse;
import com.preferences.domain.model.Preferences;
import com.preferences.domain.repository.PreferencesRepository;
import com.preferences.infrastructure.etag.ETagHelper;
import com.preferences.infrastructure.filter.RateLimitFilter;
import com.preferences.presentation.exception.PreconditionFailedException;
import com.preferences.presentation.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/preferences")
public class PreferencesController {

    private final PreferencesService preferencesService;
    private final RateLimitFilter rateLimitFilter;
    private final ETagHelper eTagHelper;
    private final PreferencesRepository preferencesRepository;

    public PreferencesController(PreferencesService preferencesService,
                                 RateLimitFilter rateLimitFilter,
                                 ETagHelper eTagHelper,
                                 PreferencesRepository preferencesRepository) {
        this.preferencesService = preferencesService;
        this.rateLimitFilter = rateLimitFilter;
        this.eTagHelper = eTagHelper;
        this.preferencesRepository = preferencesRepository;
    }

    @GetMapping("/{memberId}")
    public ResponseEntity<PreferencesResponse> getPreferences(
            @PathVariable String memberId,
            @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch,
            HttpServletRequest request) {

        rateLimitFilter.checkRateLimit(memberId);

        Preferences preferences = preferencesRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member preferences not found"));

        String currentETag = eTagHelper.computeETag(preferences);

        // Return 304 if client already has the latest version
        if (eTagHelper.matchesIfNoneMatch(ifNoneMatch, currentETag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .eTag(stripQuotes(currentETag))
                    .build();
        }

        PreferencesResponse response = preferencesService.getPreferences(memberId);
        return ResponseEntity.ok()
                .eTag(stripQuotes(currentETag))
                .body(response);
    }

    @PutMapping("/{memberId}")
    public ResponseEntity<PreferencesResponse> upsertPreferences(
            @PathVariable String memberId,
            @Valid @RequestBody PreferencesRequest request,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            HttpServletRequest httpRequest) {

        rateLimitFilter.checkRateLimit(memberId);

        // If resource exists and If-Match header is present, validate it
        preferencesRepository.findById(memberId).ifPresent(existing -> {
            String currentETag = eTagHelper.computeETag(existing);
            if (!eTagHelper.matchesIfMatch(ifMatch, currentETag)) {
                throw new PreconditionFailedException(
                        "Resource has been modified since your last read. Fetch the latest version and retry.");
            }
        });

        PreferencesResponse response = preferencesService.upsertPreferences(memberId, request);

        Preferences saved = preferencesRepository.findById(memberId).orElseThrow();
        String newETag = eTagHelper.computeETag(saved);

        return ResponseEntity.ok()
                .eTag(stripQuotes(newETag))
                .body(response);
    }

    @PatchMapping("/{memberId}")
    public ResponseEntity<PreferencesResponse> patchPreferences(
            @PathVariable String memberId,
            @Valid @RequestBody PreferencesPatchRequest request,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            HttpServletRequest httpRequest) {

        rateLimitFilter.checkRateLimit(memberId);

        // If resource exists and If-Match header is present, validate it
        preferencesRepository.findById(memberId).ifPresent(existing -> {
            String currentETag = eTagHelper.computeETag(existing);
            if (!eTagHelper.matchesIfMatch(ifMatch, currentETag)) {
                throw new PreconditionFailedException(
                        "Resource has been modified since your last read. Fetch the latest version and retry.");
            }
        });

        PreferencesResponse response = preferencesService.patchPreferences(memberId, request);

        Preferences saved = preferencesRepository.findById(memberId).orElseThrow();
        String newETag = eTagHelper.computeETag(saved);

        return ResponseEntity.ok()
                .eTag(stripQuotes(newETag))
                .body(response);
    }

    /**
     * Spring's .eTag() wraps the value in quotes automatically,
     * so strip existing quotes first to avoid double-quoting like ""abc123"".
     */
    private String stripQuotes(String eTag) {
        if (eTag != null && eTag.startsWith("\"") && eTag.endsWith("\"")) {
            return eTag.substring(1, eTag.length() - 1);
        }
        return eTag;
    }
}
