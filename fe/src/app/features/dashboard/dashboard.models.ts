export type ReportStatus = 'DRAFT' | 'IN_REVIEW' | 'APPROVED' | 'EXPORTED';
export interface CategoryMetric {
  categoryId: string;
  categoryName: string;
  employeeDays: number;
  presentDays: number;
  exceptionDays: number;
  typicalArrival: string | null;
  overtimeMinutes: number;
}
export interface RecentImport {
  id: string;
  fileName: string;
  deviceName: string;
  importedRows: number;
  errorRows: number;
  importedAt: number;
}
export interface Dashboard {
  year: number;
  month: number;
  updatedAt: string | null;
  activeEmployees: number;
  activeCategories: number;
  reportStatus: ReportStatus | null;
  reportId: string | null;
  unresolvedCount: number;
  scheduledEmployeeDays: number;
  presentEmployeeDays: number;
  attendanceRate: number;
  lateEmployeeDays: number;
  singlePunchDays: number;
  overtimeMinutes: number;
  unmatchedIdentities: number;
  importedPunches: number;
  totalStockMovements: number;
  totalInventoryItems: number;
  lowStockCount: number;
  negativeStockCount: number;
  totalPartnerEntries: number;
  activePartiesCount: number;
  categories: CategoryMetric[];
  recentImports: RecentImport[];
}
