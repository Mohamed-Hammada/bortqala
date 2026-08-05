package com.bemo.hr.attendance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "biometric_devices")
public class BiometricDevice {
    @Id private String id;
    @TenantId @Column(name = "app_id", nullable = false) private String appId;
    @Column(nullable = false, length = 150) private String name;
    @Column(name = "endpoint_url", nullable = false, length = 1000) private String endpointUrl;
    @Column(name = "device_username", length = 150) private String username;
    @Column(name = "device_password_enc", length = 1000) private String passwordEncrypted;
    @Column(nullable = false) private boolean enabled;
    @Column(name = "sync_interval_minutes", nullable = false) private int syncIntervalMinutes;
    @Column(name = "last_sync_at") private Instant lastSyncAt;
    @Column(name = "last_successful_punch_at") private Instant lastSuccessfulPunchAt;
    @Column(name = "next_sync_at") private Instant nextSyncAt;
    @Column(name = "last_status", nullable = false, length = 20) private String lastStatus;
    @Column(name = "last_message", length = 500) private String lastMessage;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected BiometricDevice() { }

    public BiometricDevice(String name, String endpointUrl, boolean enabled, int syncIntervalMinutes) {
        this.id = UUID.randomUUID().toString();
        update(name, endpointUrl, enabled, syncIntervalMinutes);
        this.lastStatus = "NEVER_SYNCED";
    }

    public void update(String name, String endpointUrl, boolean enabled, int syncIntervalMinutes) {
        this.name = name.strip();
        this.endpointUrl = endpointUrl.strip();
        this.enabled = enabled;
        this.syncIntervalMinutes = Math.max(1, syncIntervalMinutes);
        this.nextSyncAt = enabled ? Instant.now() : null;
    }

    public void setCredentials(String username, String passwordEncrypted) {
        this.username = username == null || username.isBlank() ? null : username.strip();
        this.passwordEncrypted = passwordEncrypted == null || passwordEncrypted.isBlank() ? null : passwordEncrypted;
    }

    public boolean hasPassword() {
        return passwordEncrypted != null && !passwordEncrypted.isBlank();
    }

    public void syncSucceeded(int importedRows, Instant latestPunchAt) {
        Instant now = Instant.now();
        this.lastSyncAt = now;
        if (latestPunchAt != null && (lastSuccessfulPunchAt == null || latestPunchAt.isAfter(lastSuccessfulPunchAt))) {
            this.lastSuccessfulPunchAt = latestPunchAt;
        }
        this.nextSyncAt = enabled ? now.plusSeconds(syncIntervalMinutes * 60L) : null;
        this.lastStatus = "SUCCESS";
        this.lastMessage = "Imported " + importedRows + " punch records.";
    }

    public void syncFailed(String message) {
        Instant now = Instant.now();
        this.lastSyncAt = now;
        this.nextSyncAt = enabled ? now.plusSeconds(syncIntervalMinutes * 60L) : null;
        this.lastStatus = "FAILED";
        this.lastMessage = message == null ? "Device synchronization failed." : message.substring(0, Math.min(500, message.length()));
    }

    public boolean isDue(Instant now) {
        return enabled && (nextSyncAt == null || !nextSyncAt.isAfter(now));
    }

    @PrePersist void prePersist() { createdAt = Instant.now(); updatedAt = createdAt; }
    @PreUpdate void preUpdate() { updatedAt = Instant.now(); }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getEndpointUrl() { return endpointUrl; }
    public String getUsername() { return username; }
    public String getPasswordEncrypted() { return passwordEncrypted; }
    public boolean isEnabled() { return enabled; }
    public int getSyncIntervalMinutes() { return syncIntervalMinutes; }
    public Instant getLastSyncAt() { return lastSyncAt; }
    public Instant getLastSuccessfulPunchAt() { return lastSuccessfulPunchAt; }
    public Instant getNextSyncAt() { return nextSyncAt; }
    public String getLastStatus() { return lastStatus; }
    public String getLastMessage() { return lastMessage; }
    public Instant getCreatedAt() { return createdAt; }
}
