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

    @Column(name = "min_password_length", nullable = false)
    private int minPasswordLength = 8;

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
        this.minPasswordLength = 8;
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
    public int getMinPasswordLength() { return minPasswordLength; }
    public Instant getUpdatedAt() { return updatedAt; }
}
