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
