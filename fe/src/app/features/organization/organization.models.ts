export interface Company {
  id: string;
  code: string;
  name: string;
  taxNumber?: string;
  commercialRegistry?: string;
  active: boolean;
  createdAt: number;
  updatedAt: number;
}

export interface Branch {
  id: string;
  companyId: string;
  code: string;
  name: string;
  location?: string;
  active: boolean;
  createdAt: number;
  updatedAt: number;
}

export interface Warehouse {
  id: string;
  branchId: string;
  code: string;
  name: string;
  location?: string;
  active: boolean;
  createdAt: number;
  updatedAt: number;
}

export interface Department {
  id: string;
  companyId: string;
  code: string;
  name: string;
  managerId?: string;
  active: boolean;
  createdAt: number;
  updatedAt: number;
}

export interface OrganizationHierarchy {
  companies: Company[];
  branches: Branch[];
  warehouses: Warehouse[];
  departments: Department[];
}

export type IntercompanyType = 'INVENTORY_TRANSFER' | 'EXPENSE_ALLOCATION' | 'MANAGEMENT_FEE' | 'LOAN_ADVANCE';
export type IntercompanyStatus = 'DRAFT' | 'PENDING_APPROVAL' | 'APPROVED' | 'SETTLED' | 'ELIMINATED';

export interface IntercompanyTransaction {
  id: string;
  transactionNumber: string;
  fromCompanyId: string;
  fromCompanyName: string;
  fromBranchId?: string;
  fromBranchName?: string;
  toCompanyId: string;
  toCompanyName: string;
  toBranchId?: string;
  toBranchName?: string;
  transactionType: IntercompanyType;
  amount: number;
  currency: string;
  description?: string;
  dueToAccountId?: string;
  dueFromAccountId?: string;
  status: IntercompanyStatus;
  eliminatedInPeriod?: string;
  journalEntryId?: string;
  createdAt: number;
  updatedAt: number;
}

export interface CreateIntercompanyPayload {
  fromCompanyId: string;
  fromBranchId?: string;
  toCompanyId: string;
  toBranchId?: string;
  transactionType: IntercompanyType;
  amount: number;
  currency?: string;
  description?: string;
  dueToAccountId?: string;
  dueFromAccountId?: string;
}

export interface RunEliminationPayload {
  period: string;
}

export interface EliminationResult {
  period: string;
  eliminatedCount: number;
  eliminatedTotalAmount: number;
}

export interface BranchPerformanceMetric {
  branchId: string;
  branchCode: string;
  branchName: string;
  companyId: string;
  companyName: string;
  revenue: number;
  expenses: number;
  netProfit: number;
  marginPercent: number;
  inventoryValue: number;
  headcount: number;
  activeProjects: number;
}

export interface ConsolidatedOrganizationSummary {
  totalRevenue: number;
  totalExpenses: number;
  eliminatedTransfers: number;
  consolidatedNetMargin: number;
  activeBranches: number;
  totalHeadcount: number;
  branchMetrics: BranchPerformanceMetric[];
}
