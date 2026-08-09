package com.bemo.hr.finance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

@Entity
@Table(name = "exchange_rate_hint_settings")
public class ExchangeRateHintSetting {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "refresh_interval_hours", nullable = false)
    private int refreshIntervalHours;

    @Column(name = "last_attempt_at")
    private Long lastAttemptAt;

    @Column(name = "last_success_at")
    private Long lastSuccessAt;

    @Column(name = "last_error_code", length = 80)
    private String lastErrorCode;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected ExchangeRateHintSetting() {}

    public static ExchangeRateHintSetting createDefault() {
        ExchangeRateHintSetting setting = new ExchangeRateHintSetting();
        setting.id = UUID.randomUUID().toString();
        setting.enabled = true;
        setting.refreshIntervalHours = 4;
        return setting;
    }

    public void update(boolean enabled, int refreshIntervalHours) {
        this.enabled = enabled;
        this.refreshIntervalHours = Math.max(1, Math.min(168, refreshIntervalHours));
    }

    public void recordAttempt(long attemptedAt) {
        this.lastAttemptAt = attemptedAt;
    }

    public void recordSuccess(long successfulAt) {
        this.lastAttemptAt = successfulAt;
        this.lastSuccessAt = successfulAt;
        this.lastErrorCode = null;
    }

    public void recordFailure(long attemptedAt, String errorCode) {
        this.lastAttemptAt = attemptedAt;
        this.lastErrorCode = errorCode == null ? "FRANKFURTER_UNAVAILABLE" : errorCode;
    }

    public boolean isDue(long now) {
        if (!enabled) return false;
        if (lastAttemptAt == null) return true;
        long intervalMs = refreshIntervalHours * 60L * 60L * 1000L;
        return lastAttemptAt + intervalMs <= now;
    }

    public Long nextRefreshAt() {
        if (!enabled) return null;
        if (lastAttemptAt == null) return System.currentTimeMillis();
        return lastAttemptAt + refreshIntervalHours * 60L * 60L * 1000L;
    }

    @PrePersist
    void prePersist() {
        long now = System.currentTimeMillis();
        createdAt = now;
        updatedAt = now;
        if (id == null) id = UUID.randomUUID().toString();
        if (refreshIntervalHours <= 0) refreshIntervalHours = 4;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public boolean isEnabled() { return enabled; }
    public int getRefreshIntervalHours() { return refreshIntervalHours; }
    public Long getLastAttemptAt() { return lastAttemptAt; }
    public Long getLastSuccessAt() { return lastSuccessAt; }
    public String getLastErrorCode() { return lastErrorCode; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
