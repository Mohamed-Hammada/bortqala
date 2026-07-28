export type ContractorAccountingModel = 'worker_net_total' | 'contractor_daily_rate' | 'worker_cost_plus_fee' | 'fixed_period_amount';
export type PaymentRouting = 'contractor_full' | 'worker_direct' | 'mixed';
export type LaborRequestStatus = 'DRAFT' | 'SENT' | 'PARTIAL' | 'APPROVED' | 'COMPLETED' | 'CLOSED' | 'CANCELLED' | 'REJECTED';
export type SettlementStatus = 'DRAFT' | 'REVIEW' | 'APPROVED' | 'POSTED' | 'DISBURSED';
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
