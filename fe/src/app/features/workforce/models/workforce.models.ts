export type ContractorAccountingModel = 'worker_net_total' | 'contractor_daily_rate' | 'worker_cost_plus_fee' | 'fixed_period_amount';
export type PaymentRouting = 'contractor_full' | 'worker_direct' | 'mixed';
export type LaborRequestStatus = 'DRAFT' | 'SENT' | 'PARTIAL' | 'APPROVED' | 'COMPLETED' | 'CLOSED' | 'CANCELLED' | 'REJECTED';
export type SettlementStatus = 'DRAFT' | 'CALCULATED' | 'REVIEWED' | 'APPROVED' | 'LOCKED' | 'POSTED' | 'PAID';
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
  scope?: 'EMPLOYEE' | 'WORKER' | 'BOTH';
  active?: boolean;
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
  projectId?: string;
  wbsNodeId?: string;
  costCodeId?: string;
  siteLocation?: string;
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
  projectId?: string;
  wbsNodeId?: string;
  costCodeId?: string;
}

export interface ManualAttendanceEntry extends AttendanceCell {
  id: string;
  source: string;
  createdAt: number;
  updatedAt: number;
}

export interface AttendanceCellError {
  workerId?: string;
  workDate?: string;
  field: string;
  message: string;
}

export interface BatchAttendanceResponse {
  createdCount: number;
  updatedCount: number;
  skippedCount: number;
  failedCount: number;
  savedEntries: ManualAttendanceEntry[];
  errors: AttendanceCellError[];
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

export interface ContractorSettlementLine {
  id: string;
  settlementId: string;
  workerId: string;
  workerName: string;
  projectId?: string;
  wbsNodeId?: string;
  costCodeId?: string;
  attendanceDays: number;
  dailyWage: number;
  grossWage: number;
  overtimeAmount: number;
  deductionsAmount: number;
  advanceInstallments: number;
  netWage: number;
}

export interface ProjectLaborCostItem {
  workerId: string;
  workerCode: string;
  workerName: string;
  contractorId: string;
  contractorName: string;
  wbsNodeId?: string;
  costCodeId?: string;
  attendanceDays: number;
  dailyWage: number;
  grossCost: number;
  overtimeAmount: number;
  netCost: number;
}

export interface ProjectLaborCostReport {
  projectId: string;
  projectName?: string;
  periodId?: string;
  totalWorkersCount: number;
  totalAttendanceDays: number;
  totalGrossLaborCost: number;
  totalOvertimeAmount: number;
  totalNetLaborCost: number;
  items: ProjectLaborCostItem[];
}

export interface ContractorSettlementAdjustment {
  id: string;
  settlementId: string;
  adjustmentType: string;
  description: string;
  amount: number;
  reason?: string;
  createdBy?: string;
  createdAt: number;
}

export interface ContractorSettlementDetail {
  id: string;
  periodId: string;
  contractorId: string;
  contractorName: string;
  accountingModel: ContractorAccountingModel;
  workersNetTotal: number;
  contractorRatesTotal: number;
  commissionAmount: number;
  fixedAmount: number;
  additionsAmount: number;
  deductionsAmount: number;
  grossAmount: number;
  netPayable: number;
  paidAmount: number;
  invoiceNumber?: string;
  invoiceDate?: number;
  postedJournalEntryId?: string;
  status: string;
  version: number;
  lines: ContractorSettlementLine[];
  adjustments: ContractorSettlementAdjustment[];
  createdAt: number;
  updatedAt: number;
}

export interface LinkInvoicePayload {
  invoiceNumber: string;
  invoiceDate: number;
  invoiceAmount?: number;
  notes?: string;
}

export interface SettlementPostingPayload {
  operationId: string;
  expectedVersion: number;
  reason?: string;
}

export interface RecordSettlementPaymentPayload {
  operationId: string;
  amount: number;
  paymentDate?: number;
  paymentReference?: string;
  notes?: string;
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
  recipientType: 'WORKER' | 'CONTRACTOR' | 'EMPLOYEE';
  workerId?: string;
  workerName?: string;
  contractorId?: string;
  contractorName?: string;
  employeeId?: string;
  employeeName?: string;
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
  appliedPolicyId?: string;
  appliedPolicyVersion?: number;
  appliedPolicySnapshot?: string;
}

export interface AdvancePolicy {
  id?: string;
  scopeType: 'GLOBAL' | 'CATEGORY' | 'WORKER' | 'EMPLOYEE_CATEGORY' | 'EMPLOYEE';
  scopeId?: string;
  scopeName?: string;
  deductionMode: 'AUTO' | 'MANUAL';
  deductionFrequency: string;
  maxDeductionPercent: number;
  defaultInstallments: number;
  deferralPeriods: number;
  version: number;
  effectiveFrom: string;
  effectiveTo?: string;
  active: boolean;
  updatedAt?: number;
}

export interface ResolvedDeductionPolicy {
  mode: string;
  cadence: string;
  source: 'CATEGORY' | 'GLOBAL' | 'DEFAULTS' | 'EMPLOYEE';
  policyId?: string | null;
  policyVersion?: number | null;
  manual: boolean;
}

export interface ManualDeductionLine {
  advanceId: string;
  appliedAmount: number;
}

export interface ManualDeductionResult {
  employeeId: string;
  periodId: string;
  appliedAmount: number;
  duplicate: boolean;
  lines: ManualDeductionLine[];
}

export interface AdvanceEmployeeOption {
  id: string;
  employeeCode: string;
  fullName: string;
  categoryId: string;
  categoryName: string;
  active: boolean;
}

export interface LaborDispatch {
  id: string;
  requestId: string;
  contractorId: string;
  projectId?: string;
  wbsNodeId?: string;
  costCodeId?: string;
  siteLocation?: string;
  dispatchDate: string;
  status: 'DRAFT' | 'DISPATCHED' | 'ACCEPTED' | 'CANCELLED';
  createdAt: number;
  updatedAt: number;
  version: number;
}

export interface WorkerAssignment {
  id: string;
  dispatchId: string;
  workerId: string;
  requestLineId?: string;
  contractorId: string;
  projectId?: string;
  wbsNodeId?: string;
  costCodeId?: string;
  siteLocation?: string;
  fromDate: string;
  toDate: string;
  agreedRateSnapshot: number;
  agreedHoursSnapshot: number;
  status: 'PROPOSED' | 'ACCEPTED' | 'REJECTED' | 'REPLACED' | 'COMPLETED';
  rejectionReason?: string;
  createdAt: number;
  updatedAt: number;
  version: number;
}

export interface WorkforceDispute {
  id: string;
  settlementPeriodId: string;
  contractorId: string;
  disputedAmount: number;
  reason: string;
  status: 'DRAFT' | 'UNDER_REVIEW' | 'RESOLVED' | 'REJECTED';
  resolutionNotes?: string;
  resolvedBy?: string;
  createdAt: number;
  updatedAt: number;
  version: number;
}

export interface CreateLaborDispatchPayload {
  requestId: string;
  contractorId: string;
  projectId?: string;
  wbsNodeId?: string;
  costCodeId?: string;
  siteLocation?: string;
  dispatchDate: string;
}

export interface CreateWorkerAssignmentPayload {
  workerId: string;
  requestLineId?: string;
  contractorId: string;
  projectId?: string;
  wbsNodeId?: string;
  costCodeId?: string;
  siteLocation?: string;
  fromDate: string;
  toDate: string;
  agreedRate: number;
  agreedHours: number;
}

export interface CreateWorkforceDisputePayload {
  contractorId: string;
  disputedAmount: number;
  reason: string;
}

