export interface EnrolledDevice {
  id: string;
  deviceIdentifier: string;
  deviceName: string;
  algorithm: string;
  status: 'ACTIVE' | 'REVOKED' | 'SUSPENDED';
  revokedReason?: string | null;
  enrolledAt: string;
  revokedAt?: string | null;
  lastUsedAt?: string | null;
}

export interface EnrollDeviceRequest {
  deviceIdentifier: string;
  deviceName: string;
  publicKey: string;
  algorithm: string;
}

export interface SigningChallenge {
  challengeId: string;
  deviceId: string;
  nonce: string;
  operationType: string;
  payloadHash: string;
  expiresAt: string;
}

export interface VerificationResult {
  verified: boolean;
  challengeId: string;
  operationType?: string;
  verifiedAt: string;
}
