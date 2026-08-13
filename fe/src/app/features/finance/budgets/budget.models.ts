export type BudgetPeriodType = 'ANNUAL' | 'MONTHLY';
export type EncumbranceStatus = 'ACTIVE' | 'RELEASED';

export interface BudgetResponse {
  id: string;
  fiscalYear: number;
  periodType: BudgetPeriodType;
  periodMonth: number | null;
  departmentId: string;
  departmentName: string | null;
  plannedAmount: number;
  currencyCode: string;
  blocking: boolean;
  active: boolean;
  revisionApprovalRequired?: boolean;
  currentRevisionNumber?: number;
  createdAt: number;
  updatedAt: number;
}

export interface BudgetPayload {
  fiscalYear: number;
  periodType: BudgetPeriodType;
  periodMonth: number | null;
  departmentId: string;
  plannedAmount: number;
  currencyCode: string;
  blocking: boolean;
  active: boolean;
  revisionApprovalRequired?: boolean;
}

export type BudgetRevisionStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export interface BudgetRevision {
  id: string;
  budgetId: string;
  revisionNumber: number;
  previousAmount: number;
  newAmount: number;
  reason: string;
  requestedBy: string;
  status: BudgetRevisionStatus;
  approvedBy: string | null;
  decidedAt: number | null;
  createdAt: number;
}

export interface BudgetStatusResponse {
  budgetId: string;
  fiscalYear: number;
  periodType: BudgetPeriodType;
  periodMonth: number | null;
  departmentId: string;
  departmentName: string | null;
  plannedAmount: number;
  committedAmount: number;
  actualAmount: number;
  availableAmount: number;
  utilizationPercent: number;
  blocking: boolean;
  currencyCode: string;
}

export interface EncumbranceResponse {
  id: string;
  budgetId: string;
  purchaseOrderId: string;
  purchaseOrderNumber: string;
  documentType: string;
  status: EncumbranceStatus;
  committedAmount: number;
  liquidatedAmount: number;
  releasedAmount: number;
  currencyCode: string;
  committedAt: number;
  releasedAt: number | null;
}

export interface Department {
  id: string;
  companyId: string;
  code: string;
  name: string;
  managerId: string | null;
  active: boolean;
  createdAt: number;
  updatedAt: number;
}

export interface Currency {
  id: string;
  code: string;
  name: string;
  symbol: string;
  isBase: boolean;
  exchangeRate: number;
  active: boolean;
}
