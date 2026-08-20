package com.bemo.hr.platform.deployment.api;

import java.util.List;

public final class PlatformDeploymentApi {

    private PlatformDeploymentApi() {}

    public record DiagnosticsResponse(
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
            long evaluatedAt,
            SecurityAuditSummary securityAudit
    ) {}

    public record SecurityAuditSummary(
            boolean trustedProxiesConfigured,
            String trustedProxiesCidr,
            boolean corsConfigured,
            String corsAllowedOrigins,
            boolean jwtConfigured,
            boolean failFastPassed
    ) {}

    public record TriggerBackupRequest(
            String snapshotName,
            String backupType // FULL, INCREMENTAL, TENANT_ONLY
    ) {}

    public record BackupSnapshotResponse(
            String id,
            String snapshotName,
            String backupType,
            long fileSizeBytes,
            String sha256Checksum,
            String encryptionAlgorithm,
            String storageLocation,
            String status,
            String verificationDrillStatus,
            Long verifiedAt,
            String verifiedBy,
            long createdAt,
            Long expiresAt
    ) {}

    public record DrRecoveryStatusResponse(
            int targetRpoMinutes,
            int targetRtoMinutes,
            int actualRpoMinutes,
            int actualRtoMinutes,
            String status,
            List<DrDrillResponse> recentDrills
    ) {}

    public record DrDrillResponse(
            String id,
            String drillName,
            int targetRpoMinutes,
            int targetRtoMinutes,
            int actualRpoMinutes,
            int actualRtoMinutes,
            String status,
            String drillDetailsJson,
            String conductedBy,
            long conductedAt
    ) {}

    public record InstallLicenseRequest(
            String licenseKey,
            String certificatePayload,
            String signatureEd25519,
            String deviceFingerprintHash
    ) {}

    public record LicenseStatusResponse(
            String id,
            String licenseKeyHash,
            String deviceFingerprintHash,
            int licensedSeats,
            List<String> licensedModules,
            long issueDate,
            Long expiryDate,
            boolean isPerpetual,
            int gracePeriodDays,
            long lastValidatedAt,
            String status,
            boolean isSignatureValid,
            int daysRemaining
    ) {}
}
