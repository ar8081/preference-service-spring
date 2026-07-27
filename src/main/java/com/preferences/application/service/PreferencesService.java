package com.preferences.application.service;

import com.preferences.domain.dto.PreferencesPatchRequest;
import com.preferences.domain.dto.PreferencesRequest;
import com.preferences.domain.dto.PreferencesResponse;
import com.preferences.domain.model.Preferences;
import com.preferences.domain.repository.PreferencesRepository;
import com.preferences.infrastructure.config.CacheConfig;
import com.preferences.presentation.exception.ResourceNotFoundException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class PreferencesService {

    private static final Logger logger = LoggerFactory.getLogger(PreferencesService.class);
    private final PreferencesRepository repository;
    private final MeterRegistry meterRegistry;

    public PreferencesService(PreferencesRepository repository, MeterRegistry meterRegistry) {
        this.repository = repository;
        this.meterRegistry = meterRegistry;
    }

    @Cacheable(value = CacheConfig.PREFERENCES_CACHE, key = "#memberId")
    public PreferencesResponse getPreferences(String memberId) {
        long startTime = System.nanoTime();
        try {
            MDC.put("memberId", memberId);
            MDC.put("operation", "GET_PREFERENCES");

            logger.info("Fetching preferences for member: {} (cache miss)", memberId);
            Preferences preferences = repository.findById(memberId)
                    .orElseThrow(() -> new ResourceNotFoundException("Member preferences not found"));

            meterRegistry.counter("preferences.get.success", "memberId", memberId).increment();
            logger.debug("Successfully retrieved preferences for member: {}", memberId);

            return mapToResponse(preferences);
        } finally {
            long durationNanos = System.nanoTime() - startTime;
            Timer.builder("preferences.operation.duration")
                    .tag("operation", "GET")
                    .publishPercentiles(0.95, 0.99)
                    .register(meterRegistry)
                    .record(durationNanos, java.util.concurrent.TimeUnit.NANOSECONDS);
            MDC.clear();
        }
    }

    @CacheEvict(value = CacheConfig.PREFERENCES_CACHE, key = "#memberId")
    public PreferencesResponse upsertPreferences(String memberId, PreferencesRequest request) {
        long startTime = System.nanoTime();
        try {
            MDC.put("memberId", memberId);
            MDC.put("operation", "UPSERT_PREFERENCES");

            logger.info("Upserting preferences for member: {} (cache evicted)", memberId);
            Preferences preferences = new Preferences(
                    memberId,
                    request.getEmailNotifications(),
                    request.getSmsNotifications(),
                    request.getLanguage(),
                    request.getTimezone(),
                    request.getMarketingConsent()
            );

            Preferences saved = repository.save(preferences);
            meterRegistry.counter("preferences.put.success", "memberId", memberId).increment();
            logger.info("Successfully upserted preferences for member: {}", memberId);

            return mapToResponse(saved);
        } finally {
            long durationNanos = System.nanoTime() - startTime;
            Timer.builder("preferences.operation.duration")
                    .tag("operation", "PUT")
                    .publishPercentiles(0.95, 0.99)
                    .register(meterRegistry)
                    .record(durationNanos, java.util.concurrent.TimeUnit.NANOSECONDS);
            MDC.clear();
        }
    }

    @CacheEvict(value = CacheConfig.PREFERENCES_CACHE, key = "#memberId")
    public PreferencesResponse patchPreferences(String memberId, PreferencesPatchRequest request) {
        long startTime = System.nanoTime();
        try {
            MDC.put("memberId", memberId);
            MDC.put("operation", "PATCH_PREFERENCES");

            logger.info("Patching preferences for member: {} (cache evicted)", memberId);
            Preferences preferences = repository.findById(memberId)
                    .orElseThrow(() -> new ResourceNotFoundException("Member preferences not found"));

            if (request.getEmailNotifications() != null) {
                preferences.setEmailNotifications(request.getEmailNotifications());
            }
            if (request.getSmsNotifications() != null) {
                preferences.setSmsNotifications(request.getSmsNotifications());
            }
            if (request.getLanguage() != null) {
                preferences.setLanguage(request.getLanguage());
            }
            if (request.getTimezone() != null) {
                preferences.setTimezone(request.getTimezone());
            }
            if (request.getMarketingConsent() != null) {
                preferences.setMarketingConsent(request.getMarketingConsent());
            }

            Preferences updated = repository.save(preferences);
            meterRegistry.counter("preferences.patch.success", "memberId", memberId).increment();
            logger.info("Successfully patched preferences for member: {}", memberId);

            return mapToResponse(updated);
        } finally {
            long durationNanos = System.nanoTime() - startTime;
            Timer.builder("preferences.operation.duration")
                    .tag("operation", "PATCH")
                    .publishPercentiles(0.95, 0.99)
                    .register(meterRegistry)
                    .record(durationNanos, java.util.concurrent.TimeUnit.NANOSECONDS);
            MDC.clear();
        }
    }

    private PreferencesResponse mapToResponse(Preferences preferences) {
        return new PreferencesResponse(
                preferences.getMemberId(),
                preferences.getEmailNotifications(),
                preferences.getSmsNotifications(),
                preferences.getLanguage(),
                preferences.getTimezone(),
                preferences.getMarketingConsent(),
                preferences.getCreatedAt(),
                preferences.getUpdatedAt()
        );
    }
}