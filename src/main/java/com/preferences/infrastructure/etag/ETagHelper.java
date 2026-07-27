package com.preferences.infrastructure.etag;

import com.preferences.domain.model.Preferences;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Component
public class ETagHelper {

    /**
     * Computes a strong ETag for the given Preferences object.
     * The ETag is an MD5 hash of all preference fields + updatedAt timestamp.
     * Result is returned as a quoted string, e.g. "a3f2d7c1b4e9..."
     */
    public String computeETag(Preferences preferences) {
        String content = preferences.getMemberId() + "|"
                + preferences.getEmailNotifications() + "|"
                + preferences.getSmsNotifications() + "|"
                + preferences.getLanguage() + "|"
                + preferences.getTimezone() + "|"
                + preferences.getMarketingConsent() + "|"
                + preferences.getUpdatedAt();
        return "\"" + md5(content) + "\"";
    }

    /**
     * Checks whether the request's If-None-Match header matches the current ETag.
     * Returns true if the client's cached version is still valid (→ send 304).
     */
    public boolean matchesIfNoneMatch(String ifNoneMatchHeader, String currentETag) {
        if (ifNoneMatchHeader == null || ifNoneMatchHeader.isBlank()) {
            return false;
        }
        return ifNoneMatchHeader.equals(currentETag);
    }

    /**
     * Checks whether the request's If-Match header matches the current ETag.
     * Returns true if the precondition is satisfied (→ allow write).
     */
    public boolean matchesIfMatch(String ifMatchHeader, String currentETag) {
        if (ifMatchHeader == null || ifMatchHeader.isBlank()) {
            return true; // No If-Match header means client doesn't care — allow write
        }
        return ifMatchHeader.equals(currentETag);
    }

    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not available", e);
        }
    }
}