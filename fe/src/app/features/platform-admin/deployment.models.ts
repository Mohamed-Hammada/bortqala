export interface SecurityAuditSummary {
  trustedProxiesConfigured: boolean;
  trustedProxiesCidr: string;
  corsConfigured: boolean;
  corsAllowedOrigins: string;
  jwtConfigured: boolean;
  failFastPassed: boolean;
}

export interface DiagnosticsResponse {
  serviceStatus: string;
  dbStatus: string;
  dbLatencyMs: number;
  storageUsedBytes: number;
  storageTotalBytes: number;
  activeSessionsCount: number;
  backgroundJobsHealth: string;
  jvmMemoryUsedMb: number;
  jvmMemoryMaxMb: number;
  uptimeSeconds: number;
  correlationId?: string;
  evaluatedAt: number;
  securityAudit?: SecurityAuditSummary;
}

export interface BackupSnapshot {
  id: string;
  snapshotName: string;
  backupType: string;
  fileSizeBytes: number;
  sha256Checksum: string;
  encryptionAlgorithm: string;
  storageLocation: string;
  status: string;
  verificationDrillStatus: string;
  verifiedAt?: number;
  verifiedBy?: string;
  createdAt: number;
  expiresAt?: number;
}

export interface DrDrillRecord {
  id: string;
  drillName: string;
  targetRpoMinutes: number;
  targetRtoMinutes: number;
  actualRpoMinutes: number;
  actualRtoMinutes: number;
  status: string;
  drillDetailsJson?: string;
  conductedBy: string;
  conductedAt: number;
}

export interface DrRecoveryStatus {
  targetRpoMinutes: number;
  targetRtoMinutes: number;
  actualRpoMinutes: number;
  actualRtoMinutes: number;
  status: string;
  recentDrills: DrDrillRecord[];
}

export interface LicenseStatus {
  id: string;
  licenseKeyHash: string;
  deviceFingerprintHash?: string;
  licensedSeats: number;
  licensedModules: string[];
  issueDate: number;
  expiryDate?: number;
  isPerpetual: boolean;
  gracePeriodDays: number;
  lastValidatedAt: number;
  status: string;
  isSignatureValid: boolean;
  daysRemaining: number;
}
