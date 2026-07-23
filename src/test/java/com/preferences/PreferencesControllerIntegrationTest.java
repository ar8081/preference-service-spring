package com.preferences;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.preferences.domain.dto.PreferencesPatchRequest;
import com.preferences.domain.dto.PreferencesRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PreferencesControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testGetPreferencesNotFound() throws Exception {
        mockMvc.perform(get("/v1/preferences/999")
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
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(initialRequest)))
                .andExpect(status().isOk());

        PreferencesPatchRequest patchRequest = new PreferencesPatchRequest();
        patchRequest.setEmailNotifications(false);
        patchRequest.setLanguage("es");

        mockMvc.perform(patch("/v1/preferences/456")
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
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/v1/preferences/789")
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
}
