export interface TotpEnrollResponse {
  secret: string;
  otpauthUri: string;
  backupCodes: string[];
}

export interface TotpStatusResponse {
  enabled: boolean;
  enabledAt: string | null;
  remainingBackupCodes: number;
}

export interface SecurityPolicyResponse {
  minPasswordLength: number;
  requireUppercase: boolean;
  requireLowercase: boolean;
  requireDigits: boolean;
  requireSpecialChars: boolean;
  passwordHistoryCount: number;
  maxPasswordAgeDays: number;
  sessionTimeoutMinutes: number;
  superAdminIpBypass: boolean;
}

export interface SecurityPolicyUpdateRequest {
  minPasswordLength: number;
  requireUppercase: boolean;
  requireLowercase: boolean;
  requireDigits: boolean;
  requireSpecialChars: boolean;
  passwordHistoryCount: number;
  maxPasswordAgeDays: number;
  sessionTimeoutMinutes: number;
  superAdminIpBypass: boolean;
}

export interface TrustedDeviceResponse {
  id: string;
  deviceId: string;
  deviceLabel: string;
  userAgent: string | null;
  ipAddress: string | null;
  lastSeenAt: string;
  revoked: boolean;
  revokedAt: string | null;
}

export interface RoleIpRuleResponse {
  id: string;
  roleCode: string;
  cidrBlock: string;
  description: string | null;
  createdAt: string;
}

export interface RoleIpRuleCreateRequest {
  roleCode: string;
  cidrBlock: string;
  description?: string;
}
