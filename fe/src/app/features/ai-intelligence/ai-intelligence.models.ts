export interface CashFlowPointDto {
  year: number;
  month: number;
  periodLabel: string;
  projectedInflow: number;
  projectedOutflow: number;
  projectedNet: number;
  lowerBand: number;
  upperBand: number;
  historical: boolean;
}

export interface CashFlowForecastDto {
  forecastMonths: number;
  points: CashFlowPointDto[];
  totalProjectedNet: number;
  confidenceNote: string;
}

export interface ExpenseAnomalyDto {
  vendorId: string;
  vendorName: string;
  expenseCategory: string;
  currentAmount: number;
  sixMonthMean: number;
  standardDeviation: number;
  zScore: number;
  flaggedReason: string;
  transactionTimestamp: number;
}

export interface DemandForecastDto {
  itemId: string;
  itemCode: string;
  itemName: string;
  currentStock: number;
  monthlyAvgConsumption: number;
  leadTimeDays: number;
  safetyStock: number;
  suggestedReorderQty: number;
  urgencyLevel: string;
}

export interface CollectionsRiskDto {
  customerId: string;
  customerName: string;
  outstandingBalance: number;
  totalInvoices: number;
  overdueInvoices: number;
  avgDaysOverdue: number;
  riskBand: string;
  scoringFactors: string[];
}

export interface NlQueryResponseDto {
  question: string;
  targetDataset: string;
  interpretedIntent: string;
  appliedFilters: string[];
  records: Record<string, unknown>[];
  totalMatchingRows: number;
  summaryAnswer: string;
  success: boolean;
}
