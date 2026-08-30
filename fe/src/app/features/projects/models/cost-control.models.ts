export type CostCategory =
  | 'LABOR'
  | 'EQUIPMENT'
  | 'MATERIAL'
  | 'SUBCONTRACTOR'
  | 'OVERHEAD'
  | 'CONTINGENCY';

export type CostLedgerEntryType =
  | 'BUDGET'
  | 'COMMITTED'
  | 'ACTUAL'
  | 'REVENUE'
  | 'FORECAST_ADJUSTMENT';

export type BudgetVersionStatus =
  | 'DRAFT'
  | 'APPROVED'
  | 'SUPERSEDED';

export interface CostCategoryBreakdown {
  category: CostCategory;
  budgetAmount: number;
  committedAmount: number;
  actualAmount: number;
  varianceAmount: number;
}

export interface CostControlSummary {
  projectId: string;
  projectName: string;
  contractValue: number;
  currencyCode: string;
  totalBudget: number;
  totalCommitted: number;
  totalActualCost: number;
  totalRecognizedRevenue: number;
  currentGrossProfit: number;
  currentGrossMarginPercent: number;
  forecastEac: number;
  forecastVac: number;
  forecastProfit: number;
  forecastMarginPercent: number;
  categoryBreakdowns: CostCategoryBreakdown[];
}

export interface ProjectBudgetLine {
  id: string;
  budgetVersionId: string;
  projectId: string;
  wbsNodeId?: string;
  costCodeId?: string;
  costCategory: CostCategory;
  description: string;
  budgetQuantity: number;
  unitOfMeasure: string;
  budgetUnitRate: number;
  budgetAmount: number;
  sortOrder: number;
}

export interface ProjectBudgetVersion {
  id: string;
  projectId: string;
  versionNumber: number;
  versionName: string;
  status: BudgetVersionStatus;
  approvedByUserId?: string;
  approvedAt?: number;
  totalBudgetAmount: number;
  notes?: string;
  linesCount: number;
  lines?: ProjectBudgetLine[];
}

export interface ProjectCostLedgerEntry {
  id: string;
  projectId: string;
  wbsNodeId?: string;
  wbsCode: string;
  costCodeId?: string;
  costCategory: CostCategory;
  entryType: CostLedgerEntryType;
  sourceModule: string;
  sourceDocumentId?: string;
  sourceDocumentNumber?: string;
  entryDate: string;
  description: string;
  quantity?: number;
  unitRate?: number;
  amount: number;
  currencyCode: string;
  postedAt?: number;
}

export interface ProjectForecastEac {
  id: string;
  projectId: string;
  wbsNodeId?: string;
  wbsCode: string;
  wbsName: string;
  costCodeId?: string;
  costCategory: CostCategory;
  budgetAmount: number;
  actualCostToDate: number;
  committedCost: number;
  estimateToComplete: number;
  estimateAtCompletion: number;
  varianceAtCompletion: number;
  forecastProfitMarginPercent?: number;
  notes?: string;
}

export interface CreateBudgetVersionRequest {
  versionName: string;
  notes?: string;
  initFromWbs: boolean;
  lines?: SaveBudgetLineRequest[];
}

export interface SaveBudgetLineRequest {
  id?: string;
  wbsNodeId?: string;
  costCodeId?: string;
  costCategory: CostCategory;
  description: string;
  budgetQuantity?: number;
  unitOfMeasure?: string;
  budgetUnitRate: number;
  sortOrder?: number;
}

export interface UpdateForecastEacRequest {
  wbsNodeId: string;
  estimateToComplete: number;
  notes?: string;
}

export interface RecordCostLedgerEntryRequest {
  wbsNodeId?: string;
  costCodeId?: string;
  costCategory: CostCategory;
  entryType: CostLedgerEntryType;
  sourceModule: string;
  sourceDocumentId?: string;
  sourceDocumentNumber?: string;
  entryDate: string;
  description: string;
  quantity?: number;
  unitRate?: number;
  amount: number;
  currencyCode?: string;
}
