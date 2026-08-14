export type PaymentStatus =
  | 'DRAFT'
  | 'CALCULATED'
  | 'REVIEWED'
  | 'APPROVED'
  | 'POSTED'
  | 'PAID'
  | 'REVERSED'
  | 'PENDING'
  | 'CANCELLED';

export interface SalaryPaymentExplanation {
  id: string;
  salaryPaymentId: string;
  componentType: string;
  formula: string;
  inputValuesJson?: string;
  calculatedAmount: number;
  explanationTextAr?: string;
  explanationTextEn?: string;
  createdAt: number;
}

export type PaymentMethod = 'CASH' | 'BANK_TRANSFER' | 'CHEQUE';

export interface PayrollRow {
  id: string | null;
  employeeId: string;
  employeeCode: string;
  employeeName: string;
  categoryId: string;
  categoryName: string;
  employmentType: string;
  reportId: string | null;
  periodYear: number;
  periodMonth: number;
  periodKind: string;
  periodStart: string;
  periodEnd: string;
  baseSalary: number;
  attendanceBonus: number;
  attendanceDeduction: number;
  activeAdvancesBalance: number;
  grossAmount: number;
  advancesDeducted: number;
  otherDeductions: number;
  bonuses: number;
  netAmount: number;
  paymentStatus: PaymentStatus;
  paidAt: string | null;
  paymentMethod: PaymentMethod | null;
  referenceCode: string | null;
  note: string | null;
  incompleteProfile: boolean;
  createdBy: string;
  createdAt: string;
  paidBy: string | null;
  reversedBy: string | null;
  reversedAt: string | null;
  reversalReason: string | null;
  version: number;
}

export interface PayrollSummary {
  totalEmployees: number;
  paidCount: number;
  pendingCount: number;
  totalGrossAmount: number;
  totalPaidAmount: number;
  totalPendingAmount: number;
  totalAdvancesDeducted: number;
}

export interface SheetResponse {
  periodYear: number;
  periodMonth: number;
  periodStatus: PaymentStatus;
  summary: PayrollSummary;
  rows: PayrollRow[];
}

export interface PaymentRequest {
  employeeId: string;
  periodYear: number;
  periodMonth: number;
  periodKind?: string;
  paymentMethod?: PaymentMethod;
  referenceCode?: string;
  note?: string;
  paidAtEpochMs?: number;
  expectedVersion: number;
}

export interface BulkPaymentRequest {
  periodYear: number;
  periodMonth: number;
  categoryId?: string;
  paymentMethod?: PaymentMethod;
  referenceCode?: string;
  note?: string;
}

export interface StatusTransitionRequest {
  periodYear: number;
  periodMonth: number;
  targetStatus: PaymentStatus;
}

export interface PayrollGlPosting {
  payrollPeriodId: string;
  journalId: string;
  grossAmount: number;
  netAmount: number;
  postedAt: number;
}

export interface ReversePaymentRequest {
  paymentId: string;
  reason: string;
  expectedVersion: number;
}
