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
  isMainBranch?: boolean;
  phone?: string;
  email?: string;
  taxNumber?: string;
  commercialRegistry?: string;
  defaultWarehouseId?: string;
  defaultCashboxId?: string;
  defaultBankAccountId?: string;
  defaultPosTerminalId?: string;
  documentCodePrefix?: string;
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

export interface BranchControlSummary {
  branchId: string;
  branchCode: string;
  branchName: string;
  companyId: string;
  companyName: string;
  isMainBranch: boolean;
  warehousesCount: number;
  cashboxesCount: number;
  bankAccountsCount: number;
  posTerminalsCount: number;
  employeesCount: number;
  inventoryValuation: number;
  activeUsersCount: number;
  documentCodePrefix?: string;
}

export interface GroupPlLine {
  accountCode: string;
  accountName: string;
  category: string;
  amount: number;
  eliminationAmount: number;
  consolidatedAmount: number;
}

export interface GroupBalanceSheetLine {
  accountCode: string;
  accountName: string;
  category: string;
  amount: number;
  eliminationAmount: number;
  consolidatedAmount: number;
}

export interface BranchComparisonItem {
  branchId: string;
  branchCode: string;
  branchName: string;
  companyName: string;
  revenue: number;
  expenses: number;
  netProfit: number;
  inventoryValuation: number;
  headcount: number;
}

export interface ConsolidatedGroupReport {
  period: string;
  companyId?: string;
  companyName?: string;
  branchId?: string;
  branchName?: string;
  totalRevenue: number;
  totalExpenses: number;
  netIncome: number;
  totalAssets: number;
  totalLiabilities: number;
  totalEquity: number;
  intercompanyEliminationsCount: number;
  intercompanyEliminationsTotal: number;
  plLines: GroupPlLine[];
  balanceSheetLines: GroupBalanceSheetLine[];
  branchComparison: BranchComparisonItem[];
}

export interface StockTransferLineItem {
  id: string;
  itemId: string;
  itemCode: string;
  itemName: string;
  quantity: number;
  shippedQuantity?: number;
  receivedQuantity?: number;
  damagedQuantity?: number;
  lostQuantity?: number;
  discrepancyReason?: string;
  discrepancyNotes?: string;
}

export interface StockTransferItem {
  id: string;
  transferNumber: string;
  sourceWarehouseId: string;
  sourceWarehouseName?: string;
  targetWarehouseId: string;
  targetWarehouseName?: string;
  sourceBranchId?: string;
  sourceBranchName?: string;
  targetBranchId?: string;
  targetBranchName?: string;
  transferDate: string;
  status: 'DRAFT' | 'SHIPPED' | 'RECEIVED' | 'CANCELLED';
  carrierName?: string;
  driverName?: string;
  driverPhone?: string;
  vehiclePlate?: string;
  waybillNumber?: string;
  dispatchedAt?: number;
  dispatchedBy?: string;
  receivedAt?: number;
  receivedBy?: string;
  hasDiscrepancy?: boolean;
  notes?: string;
  intercompanyTransactionId?: string;
  version: number;
  lines: StockTransferLineItem[];
}

export type DiscrepancyResolutionStatus = 'PENDING' | 'RESOLVED' | 'CLAIMED' | 'WRITTEN_OFF' | 'RETURNED_TO_SENDER';

export interface StockTransferDiscrepancyItem {
  id: string;
  transferId: string;
  transferLineId: string;
  itemId: string;
  itemCode?: string;
  itemName?: string;
  shippedQuantity: number;
  receivedQuantity: number;
  damagedQuantity: number;
  lostQuantity: number;
  discrepancyType: 'DAMAGED' | 'LOST' | 'OVER_DELIVERY' | 'SHORT_DELIVERY' | 'WRONG_ITEM';
  notes?: string;
  reportedBy: string;
  reportedAt: number;
  resolutionStatus: DiscrepancyResolutionStatus;
  resolutionNotes?: string;
  resolvedBy?: string;
  resolvedAt?: number;
}

export interface DispatchTransferPayload {
  carrierName: string;
  driverName: string;
  driverPhone: string;
  vehiclePlate: string;
  waybillNumber: string;
  notes?: string;
}

export interface InspectionLineInput {
  lineId: string;
  receivedQuantity: number;
  damagedQuantity: number;
  lostQuantity: number;
  discrepancyReason?: string;
  discrepancyNotes?: string;
}

export interface ReceiveTransferPayload {
  inspectionLines: InspectionLineInput[];
  notes?: string;
}

export interface ResolveDiscrepancyPayload {
  resolutionStatus: DiscrepancyResolutionStatus;
  resolutionNotes: string;
}

