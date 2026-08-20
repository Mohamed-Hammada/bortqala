package com.bemo.hr.platform.deployment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "deployment_health_snapshots")
public class DeploymentHealthSnapshot {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @TenantId
    @Column(name = "tenant_id", length = 64, nullable = false)
    private String tenantId;

    @Column(name = "service_status", length = 32, nullable = false)
    private String serviceStatus;

    @Column(name = "db_status", length = 32, nullable = false)
    private String dbStatus;

    @Column(name = "db_latency_ms", nullable = false)
    private long dbLatencyMs;

    @Column(name = "storage_used_bytes", nullable = false)
    private long storageUsedBytes;

    @Column(name = "storage_total_bytes", nullable = false)
    private long storageTotalBytes;

    @Column(name = "active_sessions_count", nullable = false)
    private int activeSessionsCount;

    @Column(name = "background_jobs_health", length = 32, nullable = false)
    private String backgroundJobsHealth;

    @Column(name = "jvm_memory_used_mb", nullable = false)
    private long jvmMemoryUsedMb;

    @Column(name = "jvm_memory_max_mb", nullable = false)
    private long jvmMemoryMaxMb;

    @Column(name = "uptime_seconds", nullable = false)
    private long uptimeSeconds;

    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    protected DeploymentHealthSnapshot() {}

    public DeploymentHealthSnapshot(
            String serviceStatus,
            String dbStatus,
            long dbLatencyMs,
            long storageUsedBytes,
            long storageTotalBytes,
            int activeSessionsCount,
            String backgroundJobsHealth,
            long jvmMemoryUsedMb,
            long jvmMemoryMaxMb,
            long uptimeSeconds,
            String correlationId,
            long createdAt) {
        this.id = UUID.randomUUID().toString();
        this.serviceStatus = Objects.requireNonNull(serviceStatus);
        this.dbStatus = Objects.requireNonNull(dbStatus);
        this.dbLatencyMs = dbLatencyMs;
        this.storageUsedBytes = storageUsedBytes;
        this.storageTotalBytes = storageTotalBytes;
        this.activeSessionsCount = activeSessionsCount;
        this.backgroundJobsHealth = Objects.requireNonNull(backgroundJobsHealth);
        this.jvmMemoryUsedMb = jvmMemoryUsedMb;
        this.jvmMemoryMaxMb = jvmMemoryMaxMb;
        this.uptimeSeconds = uptimeSeconds;
        this.correlationId = correlationId;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public String getTenantId() { return tenantId; }
    public String getServiceStatus() { return serviceStatus; }
    public String getDbStatus() { return dbStatus; }
    public long getDbLatencyMs() { return dbLatencyMs; }
    public long getStorageUsedBytes() { return storageUsedBytes; }
    public long getStorageTotalBytes() { return storageTotalBytes; }
    public int getActiveSessionsCount() { return activeSessionsCount; }
    public String getBackgroundJobsHealth() { return backgroundJobsHealth; }
    public long getJvmMemoryUsedMb() { return jvmMemoryUsedMb; }
    public long getJvmMemoryMaxMb() { return jvmMemoryMaxMb; }
    public long getUptimeSeconds() { return uptimeSeconds; }
    public String getCorrelationId() { return correlationId; }
    public long getCreatedAt() { return createdAt; }
}
