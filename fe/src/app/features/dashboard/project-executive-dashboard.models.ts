export interface TreasurySummary {
  totalBankBalance: number;
  totalCashOnHand: number;
  totalUnclearedCheques: number;
  netLiquidCapital: number;
}

export interface ExecutionHealth {
  averageProgressPercent: number;
  delayedProjectsCount: number;
  activeWorkforceHeadcount: number;
  criticalTasksCount: number;
}

export interface ProjectMatrixRow {
  projectId: string;
  projectName: string;
  status: string;
  contractValue: number;
  budgetAmount: number;
  committedAmount: number;
  actualCost: number;
  recognizedRevenue: number;
  grossProfit: number;
  grossMarginPercent: number;
  progressPercent: number;
  delayed: boolean;
}

export interface ProjectExecutiveDashboardResponse {
  totalProjects: number;
  activeProjects: number;
  totalContractValue: number;
  totalBudget: number;
  totalCommitted: number;
  totalActualCost: number;
  totalRevenue: number;
  portfolioGrossProfit: number;
  portfolioGrossMarginPercent: number;
  totalReceivables: number;
  totalRetentionHeld: number;
  treasury: TreasurySummary;
  executionHealth: ExecutionHealth;
  projects: ProjectMatrixRow[];
  currencyCode: string;
  dataAsOf: number;
}
