package com.preferences.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public class PreferencesResponse {
    
    @JsonProperty("memberId")
    private String memberId;
    
    @JsonProperty("emailNotifications")
    private Boolean emailNotifications;
    
    @JsonProperty("smsNotifications")
    private Boolean smsNotifications;
    
    @JsonProperty("language")
    private String language;
    
    @JsonProperty("timezone")
    private String timezone;
    
    @JsonProperty("marketingConsent")
    private Boolean marketingConsent;
    
    @JsonProperty("createdAt")
    private LocalDateTime createdAt;
    
    @JsonProperty("updatedAt")
    private LocalDateTime updatedAt;

    public PreferencesResponse() {
    }

    public PreferencesResponse(String memberId, Boolean emailNotifications, Boolean smsNotifications,
                              String language, String timezone, Boolean marketingConsent,
                              LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.memberId = memberId;
        this.emailNotifications = emailNotifications;
        this.smsNotifications = smsNotifications;
        this.language = language;
        this.timezone = timezone;
        this.marketingConsent = marketingConsent;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getMemberId() {
        return memberId;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }

    public Boolean getEmailNotifications() {
        return emailNotifications;
    }

    public void setEmailNotifications(Boolean emailNotifications) {
        this.emailNotifications = emailNotifications;
    }

    public Boolean getSmsNotifications() {
        return smsNotifications;
    }

    public void setSmsNotifications(Boolean smsNotifications) {
        this.smsNotifications = smsNotifications;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public Boolean getMarketingConsent() {
        return marketingConsent;
    }

    public void setMarketingConsent(Boolean marketingConsent) {
        this.marketingConsent = marketingConsent;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
