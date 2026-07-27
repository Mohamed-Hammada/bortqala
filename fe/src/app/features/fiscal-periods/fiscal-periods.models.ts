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
}
