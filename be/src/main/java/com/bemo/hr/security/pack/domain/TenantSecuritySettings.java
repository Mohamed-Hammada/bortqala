package com.bemo.hr.security.pack.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sec_tenant_security_settings")
public class TenantSecuritySettings {
    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "min_password_length", nullable = false)
    private int minPasswordLength = 8;

    @Column(name = "require_uppercase", nullable = false)
    private boolean requireUppercase = true;

    @Column(name = "require_lowercase", nullable = false)
    private boolean requireLowercase = true;

    @Column(name = "require_digits", nullable = false)
    private boolean requireDigits = true;

    @Column(name = "require_special_chars", nullable = false)
    private boolean requireSpecialChars = false;

    @Column(name = "password_history_count", nullable = false)
    private int passwordHistoryCount = 3;

    @Column(name = "max_password_age_days", nullable = false)
    private int maxPasswordAgeDays = 0;

    @Column(name = "session_timeout_minutes", nullable = false)
    private int sessionTimeoutMinutes = 30;

    @Column(name = "super_admin_ip_bypass", nullable = false)
    private boolean superAdminIpBypass = true;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public TenantSecuritySettings() {
    }

    public TenantSecuritySettings(String appId) {
        this.id = UUID.randomUUID().toString();
        this.appId = appId;
        this.minPasswordLength = 8;
        this.requireUppercase = true;
        this.requireLowercase = true;
        this.requireDigits = true;
        this.requireSpecialChars = false;
        this.passwordHistoryCount = 3;
        this.maxPasswordAgeDays = 0;
        this.sessionTimeoutMinutes = 30;
        this.superAdminIpBypass = true;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getAppId() {
        return appId;
    }

    public int getMinPasswordLength() {
        return minPasswordLength;
    }

    public void setMinPasswordLength(int minPasswordLength) {
        this.minPasswordLength = minPasswordLength;
        this.updatedAt = Instant.now();
    }

    public boolean isRequireUppercase() {
        return requireUppercase;
    }

    public void setRequireUppercase(boolean requireUppercase) {
        this.requireUppercase = requireUppercase;
        this.updatedAt = Instant.now();
    }

    public boolean isRequireLowercase() {
        return requireLowercase;
    }

    public void setRequireLowercase(boolean requireLowercase) {
        this.requireLowercase = requireLowercase;
        this.updatedAt = Instant.now();
    }

    public boolean isRequireDigits() {
        return requireDigits;
    }

    public void setRequireDigits(boolean requireDigits) {
        this.requireDigits = requireDigits;
        this.updatedAt = Instant.now();
    }

    public boolean isRequireSpecialChars() {
        return requireSpecialChars;
    }

    public void setRequireSpecialChars(boolean requireSpecialChars) {
        this.requireSpecialChars = requireSpecialChars;
        this.updatedAt = Instant.now();
    }

    public int getPasswordHistoryCount() {
        return passwordHistoryCount;
    }

    public void setPasswordHistoryCount(int passwordHistoryCount) {
        this.passwordHistoryCount = passwordHistoryCount;
        this.updatedAt = Instant.now();
    }

    public int getMaxPasswordAgeDays() {
        return maxPasswordAgeDays;
    }

    public void setMaxPasswordAgeDays(int maxPasswordAgeDays) {
        this.maxPasswordAgeDays = maxPasswordAgeDays;
        this.updatedAt = Instant.now();
    }

    public int getSessionTimeoutMinutes() {
        return sessionTimeoutMinutes;
    }

    public void setSessionTimeoutMinutes(int sessionTimeoutMinutes) {
        this.sessionTimeoutMinutes = sessionTimeoutMinutes;
        this.updatedAt = Instant.now();
    }

    public boolean isSuperAdminIpBypass() {
        return superAdminIpBypass;
    }

    public void setSuperAdminIpBypass(boolean superAdminIpBypass) {
        this.superAdminIpBypass = superAdminIpBypass;
        this.updatedAt = Instant.now();
    }

    public long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
