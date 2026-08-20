package com.bemo.hr.platform.deployment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "backup_snapshots")
public class BackupSnapshot {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @TenantId
    @Column(name = "tenant_id", length = 64, nullable = false)
    private String tenantId;

    @Column(name = "snapshot_name", length = 120, nullable = false)
    private String snapshotName;

    @Column(name = "backup_type", length = 32, nullable = false)
    private String backupType; // FULL, INCREMENTAL, TENANT_ONLY

    @Column(name = "file_size_bytes", nullable = false)
    private long fileSizeBytes;

    @Column(name = "sha256_checksum", length = 64, nullable = false)
    private String sha256Checksum;

    @Column(name = "encryption_algorithm", length = 32, nullable = false)
    private String encryptionAlgorithm; // AES_256_GCM

    @Column(name = "storage_location", length = 255, nullable = false)
    private String storageLocation;

    @Column(name = "status", length = 32, nullable = false)
    private String status; // COMPLETED, VERIFIED, FAILED

    @Column(name = "verification_drill_status", length = 32, nullable = false)
    private String verificationDrillStatus; // PENDING, PASSED, FAILED

    @Column(name = "verified_at")
    private Long verifiedAt;

    @Column(name = "verified_by", length = 64)
    private String verifiedBy;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "expires_at")
    private Long expiresAt;

    protected BackupSnapshot() {}

    public BackupSnapshot(
            String snapshotName,
            String backupType,
            long fileSizeBytes,
            String sha256Checksum,
            String encryptionAlgorithm,
            String storageLocation,
            String status,
            String verificationDrillStatus,
            long createdAt,
            Long expiresAt) {
        this.id = UUID.randomUUID().toString();
        this.snapshotName = Objects.requireNonNull(snapshotName);
        this.backupType = Objects.requireNonNull(backupType);
        this.fileSizeBytes = fileSizeBytes;
        this.sha256Checksum = Objects.requireNonNull(sha256Checksum);
        this.encryptionAlgorithm = Objects.requireNonNull(encryptionAlgorithm);
        this.storageLocation = Objects.requireNonNull(storageLocation);
        this.status = Objects.requireNonNull(status);
        this.verificationDrillStatus = Objects.requireNonNull(verificationDrillStatus);
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public void markVerified(String username, long timestamp) {
        this.verificationDrillStatus = "PASSED";
        this.status = "VERIFIED";
        this.verifiedAt = timestamp;
        this.verifiedBy = username;
    }

    public void markDrillFailed(String username, long timestamp) {
        this.verificationDrillStatus = "FAILED";
        this.verifiedAt = timestamp;
        this.verifiedBy = username;
    }

    public String getId() { return id; }
    public String getTenantId() { return tenantId; }
    public String getSnapshotName() { return snapshotName; }
    public String getBackupType() { return backupType; }
    public long getFileSizeBytes() { return fileSizeBytes; }
    public String getSha256Checksum() { return sha256Checksum; }
    public String getEncryptionAlgorithm() { return encryptionAlgorithm; }
    public String getStorageLocation() { return storageLocation; }
    public String getStatus() { return status; }
    public String getVerificationDrillStatus() { return verificationDrillStatus; }
    public Long getVerifiedAt() { return verifiedAt; }
    public String getVerifiedBy() { return verifiedBy; }
    public long getCreatedAt() { return createdAt; }
    public Long getExpiresAt() { return expiresAt; }
}
