export type KpiCategory =
  | 'FINANCIAL'
  | 'COMMERCIAL'
  | 'OPERATIONS'
  | 'PROJECTS'
  | 'WORKFORCE'
  | 'COMPLIANCE';

export type KpiGrain = 'DAILY' | 'MONTHLY' | 'QUARTERLY' | 'ANNUAL' | 'REAL_TIME';

export type TrendDirection = 'UP' | 'DOWN' | 'STABLE';

export type ReconciliationStatus = 'RECONCILED' | 'PENDING_REVIEW' | 'DISCREPANCY';

export type KpiUnit = 'CURRENCY_EGP' | 'PERCENT' | 'DAYS' | 'COUNT' | 'RATIO';

export interface KpiDefinition {
  key: string;
  nameEn: string;
  nameAr: string;
  category: KpiCategory;
  grain: KpiGrain;
  unit: KpiUnit;
  formulaEn: string;
  formulaAr: string;
  sourceModule: string;
  requiredPermission: string;
}

export interface ExecutiveKpiCard {
  key: string;
  nameEn: string;
  nameAr: string;
  category: KpiCategory;
  actualValue: number;
  targetValue: number;
  variancePercent: number;
  trendDirection: TrendDirection;
  unit: KpiUnit;
  reconciliationStatus: ReconciliationStatus;
  drilldownUrl: string;
}

export interface ModuleSummary {
  category: KpiCategory;
  moduleName: string;
  kpis: ExecutiveKpiCard[];
}

export interface ExecutiveOverview {
  period: string;
  timestamp: number;
  totalRevenue: number;
  totalOpex: number;
  grossProfit: number;
  netMarginPercent: number;
  operatingCashFlow: number;
  salesBookings: number;
  posGross: number;
  openReceivables: number;
  inventoryValuation: number;
  projectPortfolioValue: number;
  projectCostVariance: number;
  activeHeadcount: number;
  payrollDisbursed: number;
  attendanceRatePercent: number;
  etaTaxCompliancePercent: number;
  moduleSummaries: ModuleSummary[];
}

export interface TrendPeriodPoint {
  period: string;
  revenue: number;
  opex: number;
  netProfit: number;
  marginPercent: number;
  salesBookings: number;
  inventoryValue: number;
  payrollDisbursed: number;
  projectEarnedValue: number;
}

export interface ComparativeTrends {
  months: number;
  trendPoints: TrendPeriodPoint[];
}

export interface ExecutiveKpiSnapshot {
  id: string;
  snapshotDate: number;
  periodKey: string;
  category: KpiCategory;
  kpiKey: string;
  targetValue?: number;
  actualValue: number;
  varianceValue?: number;
  variancePercent?: number;
  trendDirection: TrendDirection;
  reconciliationStatus: ReconciliationStatus;
  drilldownUrl?: string;
  metadataJson?: string;
  createdAt: number;
}

export interface CreateSnapshotPayload {
  periodKey: string;
  category: KpiCategory;
  kpiKey: string;
  targetValue?: number;
  actualValue: number;
  varianceValue?: number;
  variancePercent?: number;
  trendDirection: TrendDirection;
  reconciliationStatus: ReconciliationStatus;
  drilldownUrl?: string;
  metadataJson?: string;
}

export interface AgingBucket {
  labelKey: string;
  amount: number;
  invoiceCount: number;
  percentOfTotal: number;
}

export interface ArApAgingSummary {
  current: AgingBucket;
  days30To60: AgingBucket;
  days60To90: AgingBucket;
  daysOver90: AgingBucket;
  total: number;
  totalOverdue: number;
}

export interface BranchPerformanceItem {
  branchId: string;
  branchCode: string;
  branchName: string;
  isMainBranch: boolean;
  revenue: number;
  cogs: number;
  grossProfit: number;
  grossMarginPercent: number;
  opex: number;
  netProfit: number;
  headcount: number;
  cashAndBank: number;
}

export interface TopCustomerItem {
  customerId: string;
  customerName: string;
  totalInvoiced: number;
  totalCollected: number;
  outstandingBalance: number;
  invoiceCount: number;
}

export interface TopProductItem {
  itemId: string;
  itemCode: string;
  itemName: string;
  quantitySold: number;
  revenue: number;
  cogs: number;
  marginPercent: number;
}

export interface ExpenseCategoryItem {
  category: string;
  nameKey: string;
  amount: number;
  percentOfTotal: number;
}

export interface StockAlertItem {
  itemId: string;
  itemCode: string;
  itemName: string;
  currentStock: number;
  reorderPoint: number;
  reorderQuantity: number;
  isDeadStock: boolean;
  estimatedValue: number;
}

export interface ManufacturingWipItem {
  orderId: string;
  orderNumber: string;
  itemName: string;
  targetQuantity: number;
  actualOutputQuantity: number;
  materialCost: number;
  startDate: string;
  status: string;
}

export interface ProjectBudgetVarianceItem {
  projectId: string;
  code: string;
  name: string;
  contractValue: number;
  budgetAmount: number;
  actualCost: number;
  costVariance: number;
  status: string;
}

export interface CockpitTargetResponse {
  id: string;
  periodKey: string;
  targetRevenue: number;
  targetGrossMarginPercent: number;
  targetMaxOpex: number;
  targetMinLiquidity: number;
  targetMaxOverdueAr: number;
  notes?: string;
  updatedAt: number;
}

export interface SaveCockpitTargetRequest {
  periodKey: string;
  targetRevenue: number;
  targetGrossMarginPercent: number;
  targetMaxOpex: number;
  targetMinLiquidity: number;
  targetMaxOverdueAr: number;
  notes?: string;
}

export interface OwnerCockpitKpiSummary {
  todaySales: number;
  todayCollections: number;
  netLiquidity: number;
  cashInHand: number;
  bankBalances: number;
  totalRevenue: number;
  totalCogs: number;
  grossMarginAmount: number;
  grossMarginPercent: number;
  totalOpex: number;
  operatingProfit: number;
  netProfit: number;
  netMarginPercent: number;
  payrollDisbursed: number;
  payrollPending: number;
  activeHeadcount: number;
  manufacturingWipCount: number;
  manufacturingWipValuation: number;
  projectBudgetTotal: number;
  projectActualCost: number;
  projectCostVariance: number;
  lowStockCount: number;
  deadStockCount: number;
  totalReceivables: number;
  overdueReceivables: number;
  totalPayables: number;
  overduePayables: number;
}

export interface OwnerCockpitResponse {
  period: string;
  companyId?: string;
  branchId?: string;
  timestamp: number;
  kpiSummary: OwnerCockpitKpiSummary;
  arAging: ArApAgingSummary;
  apAging: ArApAgingSummary;
  branchLeaderboard: BranchPerformanceItem[];
  topCustomers: TopCustomerItem[];
  topProducts: TopProductItem[];
  expenseBreakdown: ExpenseCategoryItem[];
  lowStockAlerts: StockAlertItem[];
  deadStockAlerts: StockAlertItem[];
  manufacturingWip: ManufacturingWipItem[];
  projectBudgetControl: ProjectBudgetVarianceItem[];
  targets: CockpitTargetResponse;
}


