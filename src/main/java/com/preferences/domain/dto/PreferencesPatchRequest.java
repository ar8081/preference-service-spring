package com.preferences.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PreferencesPatchRequest {
    
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

    public PreferencesPatchRequest() {
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
}
