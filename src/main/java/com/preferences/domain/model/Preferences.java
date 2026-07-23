package com.preferences.domain.model;

import java.time.LocalDateTime;

public class Preferences {
    
    private String memberId;
    private Boolean emailNotifications;
    private Boolean smsNotifications;
    private String language;
    private String timezone;
    private Boolean marketingConsent;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Preferences(String memberId, Boolean emailNotifications, Boolean smsNotifications,
                      String language, String timezone, Boolean marketingConsent) {
        this.memberId = memberId;
        this.emailNotifications = emailNotifications;
        this.smsNotifications = smsNotifications;
        this.language = language;
        this.timezone = timezone;
        this.marketingConsent = marketingConsent != null ? marketingConsent : false;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public String getMemberId() {
        return memberId;
    }

    public Boolean getEmailNotifications() {
        return emailNotifications;
    }

    public void setEmailNotifications(Boolean emailNotifications) {
        this.emailNotifications = emailNotifications;
        this.updatedAt = LocalDateTime.now();
    }

    public Boolean getSmsNotifications() {
        return smsNotifications;
    }

    public void setSmsNotifications(Boolean smsNotifications) {
        this.smsNotifications = smsNotifications;
        this.updatedAt = LocalDateTime.now();
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
        this.updatedAt = LocalDateTime.now();
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
        this.updatedAt = LocalDateTime.now();
    }

    public Boolean getMarketingConsent() {
        return marketingConsent;
    }

    public void setMarketingConsent(Boolean marketingConsent) {
        this.marketingConsent = marketingConsent;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
