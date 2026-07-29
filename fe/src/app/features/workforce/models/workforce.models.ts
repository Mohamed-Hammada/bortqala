export type ContractorAccountingModel = 'worker_net_total' | 'contractor_daily_rate' | 'worker_cost_plus_fee' | 'fixed_period_amount';
export type PaymentRouting = 'contractor_full' | 'worker_direct' | 'mixed';
export type LaborRequestStatus = 'DRAFT' | 'SENT' | 'PARTIAL' | 'APPROVED' | 'COMPLETED' | 'CLOSED' | 'CANCELLED' | 'REJECTED';
export type SettlementStatus = 'DRAFT' | 'CALCULATED' | 'REVIEWED' | 'APPROVED' | 'LOCKED';
export type TermType = 'SHORT_TERM' | 'LONG_TERM';

export interface Contractor {
  id: string;
  code: string;
  name: string;
  tradeName?: string;
  phone: string;
  secondaryPhone?: string;
  taxId?: string;
  address?: string;
  accountingModel: ContractorAccountingModel;
  paymentRouting: PaymentRouting;
  settlementCycleDays: number;
  defaultDailyRate: number;
  feeType?: string;
  feeValue?: number;
  feeBase?: string;
  fixedPeriodAmount?: number;
  status: string;
  notes?: string;
  createdAt: number;
  updatedAt: number;
}

export interface WorkerCategory {
  id: string;
  code: string;
  name: string;
  description?: string;
  defaultDailyRate: number;
  standardDailyHours: number;
  defaultSettlementCycle: string;
  status: string;
  createdAt: number;
  updatedAt: number;
}

export interface Worker {
  id: string;
  code: string;
  fullName: string;
  contractorId: string;
  contractorName?: string;
  categoryId: string;
  categoryName?: string;
  defaultDailyRate: number;
  standardDailyHours: number;
  branchId?: string;
  attendanceMode: string;
  status: string;
  phone?: string;
  nationalId?: string;
  notes?: string;
  createdAt: number;
  updatedAt: number;
}

export interface LaborRequestItem {
  id?: string;
  categoryId: string;
  categoryName?: string;
  requestedCount: number;
  sentCount: number;
  acceptedCount: number;
  varianceCount: number;
}

export interface LaborRequest {
  id: string;
  requestNumber: string;
  requestDate: number;
  branchId?: string;
  shiftName?: string;
  contractorId: string;
  contractorName?: string;
  status: LaborRequestStatus;
  notes?: string;
  createdBy?: string;
  approvedBy?: string;
  items: LaborRequestItem[];
  createdAt: number;
  updatedAt: number;
}

export interface AdvanceRepayRequest {
  amount: number;
  repaymentType: 'PARTIAL' | 'FULL';
  repaymentDate?: string;
  paymentMethod?: string;
  receiptRef?: string;
  notes?: string;
}

export interface AttendanceCell {
  workerId: string;
  workDate: string;
  attendanceValue: number; // 1, 0.5, 0
  checkIn?: string;
  checkOut?: string;
  actualHours?: number;
  overtimeHours?: number;
  deductionHours?: number;
  effectiveDailyRate?: number;
  notes?: string;
}

export interface SettlementPeriod {
  id: string;
  periodCode: string;
  startDate: string;
  endDate: string;
  cycleType: string;
  status: SettlementStatus;
  calculationVersion: number;
  lastCalculatedAt?: number;
  lastCalculatedBy?: string;
  lastCalculationFailedAt?: number;
  lastCalculationError?: string;
  needsRecalculation: boolean;
  resultRecordCount: number;
  resultGrossAmount: number;
  resultDeductions: number;
  resultAdvances: number;
  resultNetAmount: number;
  resultWarningCount: number;
  resultErrorCount: number;
  createdAt: number;
  updatedAt: number;
}

export interface SettlementCalculationSummary {
  periodId: string;
  periodCode: string;
  totalWorkers: number;
  totalContractors: number;
  totalAttendanceUnits: number;
  grossWorkersAmount: number;
  totalDeductions: number;
  totalAdvanceDeductions: number;
  netWorkersAmount: number;
  netContractorsPayable: number;
  status: SettlementStatus;
  calculationVersion: number;
  executedAt: number;
  executedBy: string;
  warningCount: number;
  errorCount: number;
  issues: SettlementIssue[];
}

export interface SettlementIssue {
  id: string;
  workerId?: string;
  workerName?: string;
  severity: 'WARNING' | 'ERROR';
  code: string;
  message: string;
}

export type WorkforceImportStatus = 'UPLOADED' | 'MAPPED' | 'VALIDATED' | 'READY' | 'IMPORTED' | 'REVERSED';
export interface WorkforceImportBatch {
  id: string; fileName: string; checksum: string; status: WorkforceImportStatus;
  headers: string[]; columnMapping: Record<string, string>; totalRows: number;
  validRows: number; invalidRows: number; importedRows: number; createdBy: string;
  createdAt: number; importedAt?: number; reversedAt?: number;
}
export interface WorkforceImportRow {
  rowNumber: number; workerCode?: string; workerName?: string; workDate?: string;
  attendanceValue?: number; validationStatus: 'VALID' | 'INVALID'; errorCode?: string; errorMessage?: string;
}
export interface WorkforceImportValidation {
  batch: WorkforceImportBatch; preview: WorkforceImportRow[]; warningCount: number;
  canCommitAll: boolean; canCommitValidRows: boolean;
}
export interface WorkforceImportCommit {
  batch: WorkforceImportBatch; createdRows: number; updatedRows: number;
  skippedInvalidRows: number; idempotentReplay: boolean;
}

export interface WorkforceAdvance {
  id: string;
  recipientType: 'WORKER' | 'CONTRACTOR';
  workerId?: string;
  workerName?: string;
  contractorId?: string;
  contractorName?: string;
  amount: number;
  termType: TermType;
  totalInstallments: number;
  installmentAmount: number;
  remainingBalance: number;
  deductionFrequency: string;
  maxDeductionPercent: number;
  status: string;
  reason?: string;
  createdAt: number;
}

export interface AdvancePolicy {
  id?: string;
  scopeType: 'GLOBAL' | 'CATEGORY' | 'WORKER';
  scopeId?: string;
  scopeName?: string;
  deductionMode: 'AUTO' | 'MANUAL';
  deductionFrequency: string;
  maxDeductionPercent: number;
  defaultInstallments: number;
  deferralPeriods: number;
  active: boolean;
  updatedAt?: number;
}
