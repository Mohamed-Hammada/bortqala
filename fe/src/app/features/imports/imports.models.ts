export type ImportStatus = 'COMPLETED' | 'COMPLETED_WITH_ERRORS' | 'REVERSED';
export interface RowError {
  rowNumber: number;
  message: string;
  rawLine: string | null;
}
export interface ImportPreview {
  fileName: string;
  checksum: string;
  totalRows: number;
  importedRows: number;
  errorRows: number;
  rows: PreviewRow[];
  errors: RowError[];
}
export interface PreviewRow {
  rowNumber: number;
  deviceUserId: string;
  employeeName: string | null;
  punchedAt: number;
  rawLine: string;
}
export interface ImportBatch {
  id: string;
  fileName: string;
  deviceName: string;
  status: ImportStatus;
  totalRows: number;
  importedRows: number;
  errorRows: number;
  importedBy: string;
  importedAt: number;
  duplicate: boolean;
  errors: RowError[];
}
export interface UnmatchedIdentity {
  deviceUserId: string;
  observedName: string | null;
  punchCount: number;
  firstPunch: number;
  lastPunch: number;
}
export interface BiometricDevice {
  id: string;
  name: string;
  endpointUrl: string;
  enabled: boolean;
  syncIntervalMinutes: number;
  lastSyncAt: number | null;
  lastSuccessfulPunchAt: number | null;
  nextSyncAt: number | null;
  lastStatus: 'NEVER_SYNCED' | 'SUCCESS' | 'FAILED';
  lastMessage: string | null;
  username: string | null;
  hasPassword: boolean;
  createdAt: number;
}
export interface BiometricDeviceRequest {
  name: string;
  endpointUrl: string;
  enabled: boolean;
  syncIntervalMinutes: number;
  username?: string;
  password?: string;
}
export interface BiometricDeviceSyncResult {
  device: BiometricDevice;
  receivedRows: number;
  importedRows: number;
  duplicateRows: number;
  duplicateBatch: boolean;
}
