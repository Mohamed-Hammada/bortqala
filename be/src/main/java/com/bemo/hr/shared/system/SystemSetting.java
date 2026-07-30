package com.bemo.hr.shared.system;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "system_settings")
class SystemSetting {
    static final String CLIENT_CACHE_VERSION = "client_cache_version";

    @Id
    @Column(name = "setting_key", nullable = false, length = 100)
    private String key;

    @Column(name = "setting_value", nullable = false, length = 255)
    private String value;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by", length = 150)
    private String updatedBy;

    @Column(name = "change_reason", length = 500)
    private String changeReason;

    protected SystemSetting() {
    }

    String getValue() {
        return value;
    }

    String getUpdatedBy() {
        return updatedBy;
    }

    Instant getUpdatedAt() {
        return updatedAt;
    }

    void rotate(String actor, String reason) {
        value = Instant.now().toEpochMilli() + "-" + UUID.randomUUID().toString().substring(0, 8);
        updatedAt = Instant.now();
        updatedBy = actor;
        changeReason = reason == null || reason.isBlank() ? null : reason.strip();
    }
}
