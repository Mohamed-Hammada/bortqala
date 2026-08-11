package com.bemo.hr.shared.security;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

@Entity
@Table(name = "tenant_features")
@IdClass(TenantFeatureId.class)
public class TenantFeature {

    @Id
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Id
    @Column(name = "feature_key", nullable = false)
    private String featureKey;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "config_json", length = 2000)
    private String configJson;

    @Version
    private long version;

    @Column(name = "updated_by", length = 160)
    private String updatedBy;

    @Column(name = "change_reason", nullable = false, length = 500)
    private String changeReason;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TenantFeature() {
    }

    public TenantFeature(String appId, String featureKey, boolean enabled, String configJson, String updatedBy) {
        this.appId = appId;
        this.featureKey = featureKey;
        this.enabled = enabled;
        this.configJson = configJson;
        this.updatedBy = updatedBy;
        this.changeReason = "Initial entitlement configuration";
    }

    @PrePersist
    @PreUpdate
    void preSave() {
        updatedAt = Instant.now();
    }

    public String getAppId() {
        return appId;
    }

    public String getFeatureKey() {
        return featureKey;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getConfigJson() {
        return configJson;
    }

    public void setConfigJson(String configJson) {
        this.configJson = configJson;
    }

    public long getVersion() {
        return version;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public String getChangeReason() { return changeReason; }

    public void update(boolean enabled, String configJson, String reason, String actor) {
        this.enabled = enabled;
        this.configJson = configJson == null || configJson.isBlank() ? null : configJson.strip();
        this.changeReason = reason.strip();
        this.updatedBy = actor;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
