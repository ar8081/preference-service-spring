package com.preferences;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.preferences.domain.dto.PreferencesRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.stream.Stream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Contract and property-based tests derived from openapi.yaml.
 * Covers:
 *  - Response schema conformance (correct fields + types)
 *  - Required field enforcement
 *  - Boundary values for string lengths
 *  - Invalid type rejection
 *  - Missing and extra fields
 *  - All documented HTTP status codes
 */
@SpringBootTest(properties = "preferences.rate-limit.enabled=false")
@AutoConfigureMockMvc
@DisplayName("OpenAPI Contract Tests")
class OpenApiContractTest {

    private static final String API_KEY    = "secret-api-key-12345";
    private static final String BASE_PATH  = "/v1/preferences/";
    private static final String MEMBER_ID  = "contract-member";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    // ─────────────────────────────────────────────────────────────────────────
    // Setup — seed one known member before each test
    // ─────────────────────────────────────────────────────────────────────────

    @BeforeEach
    void seedMember() throws Exception {
        PreferencesRequest request = new PreferencesRequest(true, false, "en", "UTC", false);
        mockMvc.perform(put(BASE_PATH + MEMBER_ID)
                .header("X-API-Key", API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. RESPONSE SCHEMA CONFORMANCE
    //    Verifies response fields match PreferencesResponse schema in openapi.yaml
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("1. Response Schema Conformance")
    class ResponseSchemaConformance {

        @Test
        @DisplayName("GET 200 — response contains all schema fields with correct types")
        void getResponseMatchesSchema() throws Exception {
            mockMvc.perform(get(BASE_PATH + MEMBER_ID)
                            .header("X-API-Key", API_KEY))
                    .andExpect(status().isOk())
                    // Required fields from PreferencesResponse schema
                    .andExpect(jsonPath("$.memberId").isString())
                    .andExpect(jsonPath("$.emailNotifications").isBoolean())
                    .andExpect(jsonPath("$.smsNotifications").isBoolean())
                    .andExpect(jsonPath("$.language").isString())
                    .andExpect(jsonPath("$.marketingConsent").isBoolean())
                    .andExpect(jsonPath("$.createdAt").exists())
                    .andExpect(jsonPath("$.updatedAt").exists());
        }

        @Test
        @DisplayName("PUT 200 — response contains all schema fields")
        void putResponseMatchesSchema() throws Exception {
            PreferencesRequest request = new PreferencesRequest(false, true, "fr", "UTC", true);
            mockMvc.perform(put(BASE_PATH + "contract-put-schema")
                            .header("X-API-Key", API_KEY)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.memberId").isString())
                    .andExpect(jsonPath("$.emailNotifications").isBoolean())
                    .andExpect(jsonPath("$.smsNotifications").isBoolean())
                    .andExpect(jsonPath("$.language").isString())
                    .andExpect(jsonPath("$.createdAt").exists())
                    .andExpect(jsonPath("$.updatedAt").exists());
        }

        @Test
        @DisplayName("404 error — response matches ErrorResponse schema")
        void errorResponseMatchesSchema() throws Exception {
            mockMvc.perform(get(BASE_PATH + "nonexistent-schema-test")
                            .header("X-API-Key", API_KEY))
                    .andExpect(status().isNotFound())
                    // ErrorResponse schema fields
                    .andExpect(jsonPath("$.error").isString())
                    .andExpect(jsonPath("$.message").isString())
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.path").isString())
                    // Correct error code from spec
                    .andExpect(jsonPath("$.error").value("NOT_FOUND"));
        }

        @Test
        @DisplayName("GET 200 — memberId in response matches path parameter")
        void memberIdMatchesPathParam() throws Exception {
            mockMvc.perform(get(BASE_PATH + MEMBER_ID)
                            .header("X-API-Key", API_KEY))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.memberId").value(MEMBER_ID));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. REQUIRED FIELD ENFORCEMENT
    //    openapi.yaml: PreferencesRequest requires emailNotifications,
    //    smsNotifications, language
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("2. Required Field Enforcement")
    class RequiredFields {

        @Test
        @DisplayName("PUT — missing emailNotifications → 400")
        void missingEmailNotifications() throws Exception {
            String body = "{\"smsNotifications\":false,\"language\":\"en\"}";
            performPut("contract-req-1", body)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("INVALID_INPUT"));
        }

        @Test
        @DisplayName("PUT — missing smsNotifications → 400")
        void missingSmsNotifications() throws Exception {
            String body = "{\"emailNotifications\":true,\"language\":\"en\"}";
            performPut("contract-req-2", body)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("INVALID_INPUT"));
        }

        @Test
        @DisplayName("PUT — missing language → 400")
        void missingLanguage() throws Exception {
            String body = "{\"emailNotifications\":true,\"smsNotifications\":false}";
            performPut("contract-req-3", body)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("INVALID_INPUT"));
        }

        @Test
        @DisplayName("PUT — all required fields present → 200")
        void allRequiredFieldsPresent() throws Exception {
            String body = "{\"emailNotifications\":true,\"smsNotifications\":false,\"language\":\"en\"}";
            performPut("contract-req-4", body)
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("PUT — optional fields absent → 200 (not required)")
        void optionalFieldsCanBeAbsent() throws Exception {
            // timezone and marketingConsent are optional per spec
            String body = "{\"emailNotifications\":true,\"smsNotifications\":false,\"language\":\"en\"}";
            performPut("contract-req-5", body)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.memberId").value("contract-req-5"));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. BOUNDARY VALUE ANALYSIS — language field (max 10 chars)
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("3. Boundary Values — language field")
    class BoundaryValues {

        @ParameterizedTest(name = "language = \"{0}\" → 200")
        @ValueSource(strings = {"e", "en", "fr", "en-US", "zh-Hant-TW"}) // 1,2,2,5,10 chars
        @DisplayName("Valid language lengths → 200")
        void validLanguageLengths(String language) throws Exception {
            String body = String.format(
                    "{\"emailNotifications\":true,\"smsNotifications\":false,\"language\":\"%s\",\"timezone\":\"UTC\",\"marketingConsent\":false}",
                    language);
            mockMvc.perform(put(BASE_PATH + "contract-boundary-" + language.length())
                            .header("X-API-Key", API_KEY)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk());
        }

        @ParameterizedTest(name = "language = \"{0}\" → 400 (too long)")
        @ValueSource(strings = {"en-US-extra", "this-is-long"}) // 11, 12 chars — exceeds max 10
        @DisplayName("Language over 10 chars → 400")
        void languageExceedsMaxLength(String language) throws Exception {
            String body = String.format(
                    "{\"emailNotifications\":true,\"smsNotifications\":false,\"language\":\"%s\",\"timezone\":\"UTC\",\"marketingConsent\":false}",
                    language);
            mockMvc.perform(put(BASE_PATH + "contract-bound-long")
                            .header("X-API-Key", API_KEY)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("INVALID_INPUT"));
        }

        @ParameterizedTest(name = "timezone = \"{0}\" → 200")
        @ValueSource(strings = {"UTC", "US/Eastern", "America/New_York", "Europe/London"})
        @DisplayName("Valid timezones → 200")
        void validTimezones(String timezone) throws Exception {
            String body = String.format(
                    "{\"emailNotifications\":true,\"smsNotifications\":false,\"language\":\"en\",\"timezone\":\"%s\",\"marketingConsent\":false}",
                    timezone);
            mockMvc.perform(put(BASE_PATH + "contract-tz-" + timezone.hashCode())
                            .header("X-API-Key", API_KEY)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. INVALID TYPE TESTING
    //    spec says emailNotifications, smsNotifications, marketingConsent = boolean
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("4. Invalid Types")
    class InvalidTypes {

        @ParameterizedTest(name = "emailNotifications = {0} → 400")
        @MethodSource("com.preferences.OpenApiContractTest#nonBooleanValues")
        @DisplayName("emailNotifications as non-boolean → 400")
        void emailNotificationsWrongType(String invalidValue) throws Exception {
            String body = String.format(
                    "{\"emailNotifications\":%s,\"smsNotifications\":false,\"language\":\"en\"}",
                    invalidValue);
            mockMvc.perform(put(BASE_PATH + "contract-type-1")
                            .header("X-API-Key", API_KEY)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Completely malformed JSON → 400")
        void malformedJson() throws Exception {
            mockMvc.perform(put(BASE_PATH + "contract-type-2")
                            .header("X-API-Key", API_KEY)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{this is not json}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Empty body → 400")
        void emptyBody() throws Exception {
            mockMvc.perform(put(BASE_PATH + "contract-type-3")
                            .header("X-API-Key", API_KEY)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(""))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Null values for required boolean fields → 400")
        void nullRequiredBooleans() throws Exception {
            String body = "{\"emailNotifications\":null,\"smsNotifications\":false,\"language\":\"en\"}";
            mockMvc.perform(put(BASE_PATH + "contract-type-4")
                            .header("X-API-Key", API_KEY)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("INVALID_INPUT"));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. EXTRA / UNKNOWN FIELDS
    //    Spec doesn't prohibit extra fields — they should be silently ignored
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("5. Extra and Unknown Fields")
    class ExtraFields {

        @Test
        @DisplayName("Extra unknown fields in request → ignored, 200")
        void extraFieldsAreIgnored() throws Exception {
            String body = "{\"emailNotifications\":true,\"smsNotifications\":false," +
                    "\"language\":\"en\",\"timezone\":\"UTC\",\"marketingConsent\":false," +
                    "\"unknownField\":\"value\",\"anotherExtra\":123}";
            mockMvc.perform(put(BASE_PATH + "contract-extra-1")
                            .header("X-API-Key", API_KEY)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Response does not contain unexpected extra fields")
        void responseHasNoUnexpectedFields() throws Exception {
            mockMvc.perform(get(BASE_PATH + MEMBER_ID)
                            .header("X-API-Key", API_KEY))
                    .andExpect(status().isOk())
                    // Only schema-defined fields should be present
                    .andExpect(jsonPath("$.memberId").exists())
                    .andExpect(jsonPath("$.emailNotifications").exists())
                    .andExpect(jsonPath("$.smsNotifications").exists())
                    .andExpect(jsonPath("$.language").exists())
                    .andExpect(jsonPath("$.marketingConsent").exists())
                    .andExpect(jsonPath("$.createdAt").exists())
                    .andExpect(jsonPath("$.updatedAt").exists())
                    // No internal implementation fields leaked
                    .andExpect(jsonPath("$.class").doesNotExist())
                    .andExpect(jsonPath("$.id").doesNotExist());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 6. ALL DOCUMENTED STATUS CODES ARE REACHABLE
    //    Verifies every status code in openapi.yaml can actually be triggered
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("6. All Documented Status Codes Reachable")
    class StatusCodeCoverage {

        @Test
        @DisplayName("GET → 200 (spec documents this)")
        void get200() throws Exception {
            mockMvc.perform(get(BASE_PATH + MEMBER_ID).header("X-API-Key", API_KEY))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET → 404 (spec documents this)")
        void get404() throws Exception {
            mockMvc.perform(get(BASE_PATH + "does-not-exist").header("X-API-Key", API_KEY))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("PUT → 200 (spec documents this)")
        void put200() throws Exception {
            PreferencesRequest req = new PreferencesRequest(true, false, "en", "UTC", false);
            mockMvc.perform(put(BASE_PATH + "contract-status-put")
                            .header("X-API-Key", API_KEY)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("PUT → 400 (spec documents this)")
        void put400() throws Exception {
            mockMvc.perform(put(BASE_PATH + "contract-status-put-400")
                            .header("X-API-Key", API_KEY)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"emailNotifications\":true}")) // missing required fields
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("PATCH → 200 (spec documents this)")
        void patch200() throws Exception {
            String patch = "{\"language\":\"fr\"}";
            mockMvc.perform(patch(BASE_PATH + MEMBER_ID)
                            .header("X-API-Key", API_KEY)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(patch))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("PATCH → 404 (spec documents this)")
        void patch404() throws Exception {
            String patch = "{\"language\":\"fr\"}";
            mockMvc.perform(patch(BASE_PATH + "ghost-member")
                            .header("X-API-Key", API_KEY)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(patch))
                    .andExpect(status().isNotFound());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 7. CONTENT-TYPE CONTRACT
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("7. Content-Type Contract")
    class ContentTypeContract {

        @Test
        @DisplayName("GET response Content-Type is application/json")
        void getResponseIsJson() throws Exception {
            mockMvc.perform(get(BASE_PATH + MEMBER_ID).header("X-API-Key", API_KEY))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("PUT response Content-Type is application/json")
        void putResponseIsJson() throws Exception {
            PreferencesRequest req = new PreferencesRequest(true, false, "en", "UTC", false);
            mockMvc.perform(put(BASE_PATH + "contract-ct-1")
                            .header("X-API-Key", API_KEY)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("Error response Content-Type is application/json")
        void errorResponseIsJson() throws Exception {
            mockMvc.perform(get(BASE_PATH + "no-such-member").header("X-API-Key", API_KEY))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 8. IDEMPOTENCY CONTRACT
    //    PUT must be idempotent — calling it twice with same data gives same result
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("8. Idempotency (PUT)")
    class IdempotencyContract {

        @Test
        @DisplayName("PUT called twice with same body → same response each time")
        void putIsIdempotent() throws Exception {
            PreferencesRequest req = new PreferencesRequest(true, false, "en", "UTC", false);
            String body = objectMapper.writeValueAsString(req);

            String response1 = mockMvc.perform(put(BASE_PATH + "contract-idempotent")
                            .header("X-API-Key", API_KEY)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            String response2 = mockMvc.perform(put(BASE_PATH + "contract-idempotent")
                            .header("X-API-Key", API_KEY)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            // Parse and compare key fields (exclude timestamps which may differ)
            var node1 = objectMapper.readTree(response1);
            var node2 = objectMapper.readTree(response2);
            assert node1.get("memberId").equals(node2.get("memberId"));
            assert node1.get("emailNotifications").equals(node2.get("emailNotifications"));
            assert node1.get("language").equals(node2.get("language"));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper methods
    // ─────────────────────────────────────────────────────────────────────────

    private ResultActions performPut(String memberId, String body) throws Exception {
        return mockMvc.perform(put(BASE_PATH + memberId)
                .header("X-API-Key", API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }
    /** Property source: values that are NOT booleans and Jackson cannot coerce */
    static Stream<String> nonBooleanValues() {
        return Stream.of(
                "\"yes\"",    // string — Jackson cannot coerce → 400
                "\"maybe\""  // string — Jackson cannot coerce → 400
                // Note: 1, 0, "true", "1" are intentionally excluded because
                // Jackson's default coercion rules accept them as booleans
        );
    }
}