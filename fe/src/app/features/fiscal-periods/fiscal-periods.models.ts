export interface FiscalPeriod {
  id: string;
  fiscalYear: number;
  periodNumber: number;
  periodName: string;
  startDate: number;
  endDate: number;
  status: 'OPEN' | 'CLOSED' | 'LOCKED';
  closedBy?: string;
  closedAt?: number;
  createdAt: number;
  updatedAt: number;
  version: number;
}

export interface BalanceSheetReport {
  totalAssets: number;
  totalLiabilities: number;
  totalEquity: number;
  netIncome: number;
  balanced: boolean;
}

export interface IncomeStatementReport {
  totalRevenue: number;
  totalExpenses: number;
  netIncome: number;
}

export interface ReconciliationReport {
  id: string;
  subledgerType: string;
  glBalance: number;
  subledgerBalance: number;
  varianceAmount: number;
  reconciledAt: number;
  asOfDate: string;
  differenceDetails: string;
}

export interface CloseCheckItem {
  code: string;
  module: string;
  severity: 'PASS' | 'WARNING' | 'BLOCKER';
  count: number;
  amount: number;
  message: string;
}

export interface ClosePrecheck {
  periodId: string;
  periodName: string;
  canClose: boolean;
  checks: CloseCheckItem[];
}

export interface PeriodReadiness {
  periodId: string;
  allReady: boolean;
  modules: Array<{ moduleName: string; ready: boolean; blockerReason?: string }>;
}

export interface CloseExecutionRecord {
  id: string;
  moduleName: string;
  status: string;
  blockerReason?: string;
  closedAt?: number;
}
