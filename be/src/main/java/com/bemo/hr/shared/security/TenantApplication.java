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

    @Column(name = "commercial_state", nullable = false, length = 20)
    private String commercialState = "PAID";

    @Column(name = "trial_started_at") private Instant trialStartedAt;
    @Column(name = "trial_ends_at") private Instant trialEndsAt;
    @Column(name = "converted_at") private Instant convertedAt;
    @Column(name = "last_trial_operation_id", length = 80) private String lastTrialOperationId;
    @Column(name = "last_conversion_operation_id", length = 80) private String lastConversionOperationId;
    @Column(name = "demo_tenant", nullable = false) private boolean demoTenant;
    @Column(name = "demo_template_code", length = 80) private String demoTemplateCode;
    @Column(name = "demo_template_version") private Integer demoTemplateVersion;
    @Column(name = "last_demo_reset_operation_id", length = 80) private String lastDemoResetOperationId;
    @Column(name = "last_demo_reset_at") private Instant lastDemoResetAt;
    @Column(name = "last_demo_reset_by", length = 100) private String lastDemoResetBy;

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

    public void startTrial(Instant startedAt, Instant endsAt, boolean demo, String templateCode, Integer templateVersion, String operationId) {
        if (!endsAt.isAfter(startedAt)) throw new IllegalArgumentException("Trial end must follow its start");
        commercialState = "TRIAL"; trialStartedAt = startedAt; trialEndsAt = endsAt; convertedAt = null;
        lastTrialOperationId = operationId;
        demoTenant = demo; demoTemplateCode = demo ? templateCode : null; demoTemplateVersion = demo ? templateVersion : null;
    }

    public void convertTrial(Instant at, String operationId) { commercialState = "PAID"; convertedAt = at; lastConversionOperationId = operationId; }

    public void recordDemoReset(String operationId, String actor, Instant at, String templateCode, int templateVersion) {
        lastDemoResetOperationId = operationId; lastDemoResetBy = actor; lastDemoResetAt = at;
        demoTemplateCode = templateCode; demoTemplateVersion = templateVersion;
    }

    public boolean isTrialExpired(Instant at) { return "TRIAL".equals(commercialState) && trialEndsAt != null && !at.isBefore(trialEndsAt); }

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
    public String getCommercialState() { return commercialState; }
    public Instant getTrialStartedAt() { return trialStartedAt; }
    public Instant getTrialEndsAt() { return trialEndsAt; }
    public Instant getConvertedAt() { return convertedAt; }
    public String getLastTrialOperationId() { return lastTrialOperationId; }
    public String getLastConversionOperationId() { return lastConversionOperationId; }
    public boolean isDemoTenant() { return demoTenant; }
    public String getDemoTemplateCode() { return demoTemplateCode; }
    public Integer getDemoTemplateVersion() { return demoTemplateVersion; }
    public String getLastDemoResetOperationId() { return lastDemoResetOperationId; }
    public Instant getLastDemoResetAt() { return lastDemoResetAt; }
    public String getLastDemoResetBy() { return lastDemoResetBy; }
}
