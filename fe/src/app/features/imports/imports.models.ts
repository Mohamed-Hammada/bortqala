export type ImportStatus = 'COMPLETED' | 'COMPLETED_WITH_ERRORS';
export interface RowError {
  rowNumber: number;
  message: string;
  rawLine: string | null;
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
