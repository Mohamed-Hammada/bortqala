package com.bemo.hr.shared.security;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "apps")
public class TenantApplication {
    @Id
    private String id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "session_timeout_minutes", nullable = false)
    private int sessionTimeoutMinutes;

    @Column(name = "session_timeout_enabled", nullable = false)
    private boolean sessionTimeoutEnabled = true;

    @Column(name = "show_report_presets", nullable = false)
    private boolean showReportPresets = true;

    @Column(name = "attendance_anomaly_threshold_percent", nullable = false)
    private int attendanceAnomalyThresholdPercent = 70;

    @Column(name = "automatic_procurement_numbering", nullable = false)
    private boolean automaticProcurementNumbering = true;

    @Column(name = "automatic_document_numbering", nullable = false)
    private boolean automaticDocumentNumbering = true;

    @Column(name = "admin_dashboard_customization_enabled", nullable = false)
    private boolean adminDashboardCustomizationEnabled = true;

    @Column(name = "min_password_length", nullable = false)
    private int minPasswordLength = 8;

    @Column(name = "require_uppercase", nullable = false)
    private boolean requireUppercase;

    @Column(name = "require_lowercase", nullable = false)
    private boolean requireLowercase;

    @Column(name = "require_numbers", nullable = false)
    private boolean requireNumbers;

    @Column(name = "require_special_chars", nullable = false)
    private boolean requireSpecialChars;

    @Column(name = "disallow_spaces", nullable = false)
    private boolean disallowSpaces;

    @Column(name = "max_password_length", nullable = false)
    private int maxPasswordLength = 128;

    @Column(name = "password_expiry_days", nullable = false)
    private int passwordExpiryDays;

    @Column(name = "password_history_count", nullable = false)
    private int passwordHistoryCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TenantApplication() { }

    public TenantApplication(String code, String name) {
        this.id = UUID.randomUUID().toString();
        this.code = code.strip().toUpperCase(Locale.ROOT);
        this.name = name.strip();
        this.active = true;
        this.sessionTimeoutMinutes = 480;
        this.sessionTimeoutEnabled = true;
        this.showReportPresets = true;
        this.attendanceAnomalyThresholdPercent = 70;
        this.automaticProcurementNumbering = true;
        this.automaticDocumentNumbering = true;
        this.adminDashboardCustomizationEnabled = true;
        this.minPasswordLength = 8;
        this.maxPasswordLength = 128;
    }

    public void updateSettings(int sessionTimeoutMinutes, boolean sessionTimeoutEnabled, boolean showReportPresets) {
        updateSettings(sessionTimeoutMinutes, sessionTimeoutEnabled, showReportPresets, this.minPasswordLength);
    }

    public void updateSettings(int sessionTimeoutMinutes, boolean sessionTimeoutEnabled, boolean showReportPresets, int minPasswordLength) {
        this.sessionTimeoutMinutes = sessionTimeoutMinutes;
        this.sessionTimeoutEnabled = sessionTimeoutEnabled;
        this.showReportPresets = showReportPresets;
        this.minPasswordLength = minPasswordLength <= 0 ? 8 : minPasswordLength;
    }

    public void updateProcurementNumbering(boolean automaticProcurementNumbering) {
        this.automaticProcurementNumbering = automaticProcurementNumbering;
    }

    public void updateDocumentNumbering(boolean automaticDocumentNumbering) {
        this.automaticDocumentNumbering = automaticDocumentNumbering;
    }

    public void updateAttendanceAnomalyThreshold(int attendanceAnomalyThresholdPercent) {
        if (attendanceAnomalyThresholdPercent < 1 || attendanceAnomalyThresholdPercent > 100) {
            throw new com.bemo.hr.shared.domain.BusinessRuleException("نسبة اكتشاف شذوذ البصمة يجب أن تكون بين 1 و100.");
        }
        this.attendanceAnomalyThresholdPercent = attendanceAnomalyThresholdPercent;
    }

    public void updateDashboardPolicy(boolean adminDashboardCustomizationEnabled) {
        this.adminDashboardCustomizationEnabled = adminDashboardCustomizationEnabled;
    }

    public void updatePasswordPolicy(int minPasswordLength, boolean requireUppercase, boolean requireLowercase,
                                     boolean requireNumbers, boolean requireSpecialChars, boolean disallowSpaces,
                                     int maxPasswordLength, int passwordExpiryDays, int passwordHistoryCount) {
        this.minPasswordLength = minPasswordLength <= 0 ? 8 : Math.min(minPasswordLength, 128);
        this.requireUppercase = requireUppercase;
        this.requireLowercase = requireLowercase;
        this.requireNumbers = requireNumbers;
        this.requireSpecialChars = requireSpecialChars;
        this.disallowSpaces = disallowSpaces;
        this.maxPasswordLength = maxPasswordLength <= 0 ? 128 : Math.max(maxPasswordLength, this.minPasswordLength);
        this.passwordExpiryDays = Math.max(passwordExpiryDays, 0);
        this.passwordHistoryCount = Math.max(passwordHistoryCount, 0);
    }

    @PrePersist
    void prePersist() { createdAt = Instant.now(); updatedAt = createdAt; }

    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }

    public String getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public boolean isActive() { return active; }
    public int getSessionTimeoutMinutes() { return sessionTimeoutMinutes; }
    public boolean isSessionTimeoutEnabled() { return sessionTimeoutEnabled; }
    public boolean isShowReportPresets() { return showReportPresets; }
    public int getAttendanceAnomalyThresholdPercent() { return attendanceAnomalyThresholdPercent; }
    public boolean isAutomaticProcurementNumbering() { return automaticProcurementNumbering; }
    public boolean isAutomaticDocumentNumbering() { return automaticDocumentNumbering; }
    public boolean isAdminDashboardCustomizationEnabled() { return adminDashboardCustomizationEnabled; }
    public int getMinPasswordLength() { return minPasswordLength; }
    public boolean isRequireUppercase() { return requireUppercase; }
    public boolean isRequireLowercase() { return requireLowercase; }
    public boolean isRequireNumbers() { return requireNumbers; }
    public boolean isRequireSpecialChars() { return requireSpecialChars; }
    public boolean isDisallowSpaces() { return disallowSpaces; }
    public int getMaxPasswordLength() { return maxPasswordLength; }
    public int getPasswordExpiryDays() { return passwordExpiryDays; }
    public int getPasswordHistoryCount() { return passwordHistoryCount; }
    public Instant getUpdatedAt() { return updatedAt; }
}
