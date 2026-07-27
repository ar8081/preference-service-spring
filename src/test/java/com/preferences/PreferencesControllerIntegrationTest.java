package com.preferences;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.preferences.domain.dto.PreferencesPatchRequest;
import com.preferences.domain.dto.PreferencesRequest;
import com.preferences.infrastructure.config.CacheConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PreferencesControllerIntegrationTest {

    private static final String VALID_API_KEY = "secret-api-key-12345";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CacheManager cacheManager;

    @Test
    void testGetPreferencesNotFound() throws Exception {
        mockMvc.perform(get("/v1/preferences/999")
                        .header("X-API-Key", VALID_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Member preferences not found"));
    }

    @Test
    void testUpsertPreferences() throws Exception {
        PreferencesRequest request = new PreferencesRequest(
                true, false, "en", "UTC", false
        );

        mockMvc.perform(put("/v1/preferences/123")
                        .header("X-API-Key", VALID_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberId").value("123"))
                .andExpect(jsonPath("$.emailNotifications").value(true))
                .andExpect(jsonPath("$.language").value("en"));
    }

    @Test
    void testUpsertWithInvalidData() throws Exception {
        String invalidRequest = "{ \"emailNotifications\": true }";

        mockMvc.perform(put("/v1/preferences/123")
                        .header("X-API-Key", VALID_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_INPUT"));
    }

    @Test
    void testPatchPreferences() throws Exception {
        PreferencesRequest initialRequest = new PreferencesRequest(
                true, false, "en", "UTC", false
        );

        mockMvc.perform(put("/v1/preferences/456")
                        .header("X-API-Key", VALID_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initialRequest)))
                .andExpect(status().isOk());
        PreferencesPatchRequest patchRequest = new PreferencesPatchRequest();
        patchRequest.setEmailNotifications(false);

        patchRequest.setLanguage("es");

        mockMvc.perform(patch("/v1/preferences/456")
                        .header("X-API-Key", VALID_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patchRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailNotifications").value(false))
                .andExpect(jsonPath("$.language").value("es"));
    }

    @Test
    void testPatchNonExistent() throws Exception {
        PreferencesPatchRequest patchRequest = new PreferencesPatchRequest();
        patchRequest.setLanguage("fr");

        mockMvc.perform(patch("/v1/preferences/999")
                        .header("X-API-Key", VALID_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patchRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetPreferencesAfterUpsert() throws Exception {
        PreferencesRequest request = new PreferencesRequest(
                true, true, "en", "UTC", true
        );

        mockMvc.perform(put("/v1/preferences/789")
                        .header("X-API-Key", VALID_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/v1/preferences/789")
                        .header("X-API-Key", VALID_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberId").value("789"))
                .andExpect(jsonPath("$.emailNotifications").value(true))
                .andExpect(jsonPath("$.smsNotifications").value(true))
                .andExpect(jsonPath("$.language").value("en"))
                .andExpect(jsonPath("$.marketingConsent").value(true));
    }

    @Test
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void testMetricsEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetReturnsETag() throws Exception {
        PreferencesRequest request = new PreferencesRequest(true, false, "en", "UTC", false);
        mockMvc.perform(put("/v1/preferences/etag-member-1")
                        .header("X-API-Key", VALID_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/v1/preferences/etag-member-1")
                        .header("X-API-Key", VALID_API_KEY))
                .andExpect(status().isOk())
                .andExpect(header().exists("ETag"));
    }

    @Test
    void testGetReturns304WhenETagMatches() throws Exception {
        PreferencesRequest request = new PreferencesRequest(true, false, "en", "UTC", false);
        mockMvc.perform(put("/v1/preferences/etag-member-2")
                        .header("X-API-Key", VALID_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // First GET — capture the ETag
        String etag = mockMvc.perform(get("/v1/preferences/etag-member-2")
                        .header("X-API-Key", VALID_API_KEY))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getHeader("ETag");

        // Second GET with same ETag — expect 304
        mockMvc.perform(get("/v1/preferences/etag-member-2")
                        .header("X-API-Key", VALID_API_KEY)
                        .header("If-None-Match", etag))
                .andExpect(status().isNotModified());
    }

    @Test
    void testGetReturns200WithNewETagAfterChange() throws Exception {
        PreferencesRequest request = new PreferencesRequest(true, false, "en", "UTC", false);
        mockMvc.perform(put("/v1/preferences/etag-member-3")
                        .header("X-API-Key", VALID_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        String oldEtag = mockMvc.perform(get("/v1/preferences/etag-member-3")
                        .header("X-API-Key", VALID_API_KEY))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getHeader("ETag");

        // Update the resource
        PreferencesRequest updated = new PreferencesRequest(false, true, "fr", "UTC", true);
        mockMvc.perform(put("/v1/preferences/etag-member-3")
                        .header("X-API-Key", VALID_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk());

        // GET with old ETag — should get 200 with new body and new ETag
        mockMvc.perform(get("/v1/preferences/etag-member-3")
                        .header("X-API-Key", VALID_API_KEY)
                        .header("If-None-Match", oldEtag))
                .andExpect(status().isOk())
                .andExpect(header().exists("ETag"));
    }

    @Test
    void testPutWithCorrectIfMatchSucceeds() throws Exception {
        PreferencesRequest request = new PreferencesRequest(true, false, "en", "UTC", false);
        mockMvc.perform(put("/v1/preferences/etag-member-4")
                        .header("X-API-Key", VALID_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        String etag = mockMvc.perform(get("/v1/preferences/etag-member-4")
                        .header("X-API-Key", VALID_API_KEY))
                .andReturn()
                .getResponse()
                .getHeader("ETag");

        // PUT with correct ETag in If-Match — should succeed
        PreferencesRequest updated = new PreferencesRequest(false, true, "fr", "UTC", true);
        mockMvc.perform(put("/v1/preferences/etag-member-4")
                        .header("X-API-Key", VALID_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("If-Match", etag)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(header().exists("ETag"));
    }

    @Test
    void testPutWithWrongIfMatchReturns412() throws Exception {
        PreferencesRequest request = new PreferencesRequest(true, false, "en", "UTC", false);
        mockMvc.perform(put("/v1/preferences/etag-member-5")
                        .header("X-API-Key", VALID_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // PUT with a stale/wrong ETag — should return 412
        PreferencesRequest updated = new PreferencesRequest(false, true, "fr", "UTC", true);
        mockMvc.perform(put("/v1/preferences/etag-member-5")
                        .header("X-API-Key", VALID_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("If-Match", "\"wrong-etag-value\"")
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isPreconditionFailed())
                .andExpect(jsonPath("$.error").value("PRECONDITION_FAILED"));
    }

    @Test
    void testPatchWithWrongIfMatchReturns412() throws Exception {
        PreferencesRequest request = new PreferencesRequest(true, false, "en", "UTC", false);
        mockMvc.perform(put("/v1/preferences/etag-member-6")
                        .header("X-API-Key", VALID_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        PreferencesPatchRequest patchRequest = new PreferencesPatchRequest();
        patchRequest.setLanguage("de");

        // PATCH with a stale/wrong ETag — should return 412
        mockMvc.perform(patch("/v1/preferences/etag-member-6")
                        .header("X-API-Key", VALID_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("If-Match", "\"stale-etag\"")
                        .content(objectMapper.writeValueAsString(patchRequest)))
                .andExpect(status().isPreconditionFailed())
                .andExpect(jsonPath("$.error").value("PRECONDITION_FAILED"));
    }

    @Test
    void testCacheHitReturnsSameData() throws Exception {
        PreferencesRequest request = new PreferencesRequest(true, false, "en", "UTC", false);
        mockMvc.perform(put("/v1/preferences/cache-member-1")
                        .header("X-API-Key", VALID_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // First GET — cache miss, populates cache
        String body1 = mockMvc.perform(get("/v1/preferences/cache-member-1")
                        .header("X-API-Key", VALID_API_KEY))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Second GET — cache hit, returns identical data
        String body2 = mockMvc.perform(get("/v1/preferences/cache-member-1")
                        .header("X-API-Key", VALID_API_KEY))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assert body1.equals(body2) : "Cache hit should return same data";
    }

    @Test
    void testCacheInvalidatedAfterPut() throws Exception {
        PreferencesRequest initial = new PreferencesRequest(true, false, "en", "UTC", false);
        mockMvc.perform(put("/v1/preferences/cache-member-2")
                        .header("X-API-Key", VALID_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initial)))
                .andExpect(status().isOk());

        // Warm up the cache
        mockMvc.perform(get("/v1/preferences/cache-member-2")
                        .header("X-API-Key", VALID_API_KEY))
                .andExpect(status().isOk());

        // Update — evicts cache
        PreferencesRequest updated = new PreferencesRequest(false, true, "fr", "UTC", true);
        mockMvc.perform(put("/v1/preferences/cache-member-2")
                        .header("X-API-Key", VALID_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk());

        // GET after update — must return fresh data, not stale cached value
        mockMvc.perform(get("/v1/preferences/cache-member-2")
                        .header("X-API-Key", VALID_API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.language").value("fr"))
                .andExpect(jsonPath("$.emailNotifications").value(false));
    }

    @Test
    void testCacheInvalidatedAfterPatch() throws Exception {
        PreferencesRequest initial = new PreferencesRequest(true, false, "en", "UTC", false);
        mockMvc.perform(put("/v1/preferences/cache-member-3")
                        .header("X-API-Key", VALID_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initial)))
                .andExpect(status().isOk());

        // Warm up cache
        mockMvc.perform(get("/v1/preferences/cache-member-3")
                        .header("X-API-Key", VALID_API_KEY))
                .andExpect(status().isOk());

        // Patch — evicts cache
        PreferencesPatchRequest patch = new PreferencesPatchRequest();
        patch.setLanguage("de");
        mockMvc.perform(patch("/v1/preferences/cache-member-3")
                        .header("X-API-Key", VALID_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patch)))
                .andExpect(status().isOk());

        // GET after patch — must return patched data, not stale cache
        mockMvc.perform(get("/v1/preferences/cache-member-3")
                        .header("X-API-Key", VALID_API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.language").value("de"));
    }

    @Test
    void testCacheEntryExistsAfterGet() throws Exception {
        PreferencesRequest request = new PreferencesRequest(true, true, "en", "UTC", true);
        mockMvc.perform(put("/v1/preferences/cache-member-4")
                        .header("X-API-Key", VALID_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/v1/preferences/cache-member-4")
                        .header("X-API-Key", VALID_API_KEY))
                .andExpect(status().isOk());

        // Verify cache has an entry for this member
        var cache = cacheManager.getCache(CacheConfig.PREFERENCES_CACHE);
        assert cache != null : "Cache should exist";
        assert cache.get("cache-member-4") != null : "Cache entry should exist after GET";
    }

    @Test
    void testCacheEntryRemovedAfterPut() throws Exception {
        PreferencesRequest request = new PreferencesRequest(true, true, "en", "UTC", true);
        mockMvc.perform(put("/v1/preferences/cache-member-5")
                        .header("X-API-Key", VALID_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Warm up cache
        mockMvc.perform(get("/v1/preferences/cache-member-5")
                        .header("X-API-Key", VALID_API_KEY))
                .andExpect(status().isOk());

        var cache = cacheManager.getCache(CacheConfig.PREFERENCES_CACHE);
        assert cache != null && cache.get("cache-member-5") != null : "Cache should be populated";

        // PUT evicts the cache entry
        mockMvc.perform(put("/v1/preferences/cache-member-5")
                        .header("X-API-Key", VALID_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        assert cache.get("cache-member-5") == null : "Cache entry should be evicted after PUT";
    }

    @Test
    void testNotFoundErrorInEnglishByDefault() throws Exception {
        mockMvc.perform(get("/v1/preferences/i18n-unknown")
                        .header("X-API-Key", VALID_API_KEY)
                        .header("Accept-Language", "en"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Member preferences not found"));
    }

    @Test
    void testNotFoundErrorInFrench() throws Exception {
        mockMvc.perform(get("/v1/preferences/i18n-unknown-fr")
                        .header("X-API-Key", VALID_API_KEY)
                        .header("Accept-Language", "fr"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Préférences du membre introuvables"));
    }

    @Test
    void testFallbackToEnglishForUnsupportedLanguage() throws Exception {
        mockMvc.perform(get("/v1/preferences/i18n-unknown-de")
                        .header("X-API-Key", VALID_API_KEY)
                        .header("Accept-Language", "de"))  // German — not supported
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Member preferences not found"));
    }

    // ─── API Key Security Tests ───────────────────────────────────────────────────

    @Test
    void testMissingApiKeyReturns401() throws Exception {
        mockMvc.perform(get("/v1/preferences/member-1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testInvalidApiKeyReturns403() throws Exception {
        mockMvc.perform(get("/v1/preferences/member-1")
                        .header("X-API-Key", "wrong-key"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testValidApiKeyAllowsAccess() throws Exception {
        mockMvc.perform(get("/v1/preferences/nonexistent-security-test")
                        .header("X-API-Key", VALID_API_KEY))
                .andExpect(status().isNotFound()); // 404 not 401/403
    }

    @Test
    void testActuatorBypassesApiKey() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk()); // no API key needed
    }

    // ─── Input Validation Tests ───────────────────────────────────────────────────

    @Test
    void testLanguageTooLongReturns400() throws Exception {
        String body = "{\"emailNotifications\":true,\"smsNotifications\":false," +
                "\"language\":\"this-is-way-too-long\",\"timezone\":\"UTC\",\"marketingConsent\":false}";
        mockMvc.perform(put("/v1/preferences/sec-member-1")
                        .header("X-API-Key", VALID_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_INPUT"));
    }

    @Test
    void testLanguageWithInvalidCharsReturns400() throws Exception {
        String body = "{\"emailNotifications\":true,\"smsNotifications\":false," +
                "\"language\":\"en<script>\",\"timezone\":\"UTC\",\"marketingConsent\":false}";
        mockMvc.perform(put("/v1/preferences/sec-member-2")
                        .header("X-API-Key", VALID_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_INPUT"));
    }

    @Test
    void testValidInputWithApiKeySucceeds() throws Exception {
        PreferencesRequest request = new PreferencesRequest(true, false, "en", "UTC", false);
        mockMvc.perform(put("/v1/preferences/sec-member-3")
                        .header("X-API-Key", VALID_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}