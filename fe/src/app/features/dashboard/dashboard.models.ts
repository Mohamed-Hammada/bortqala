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

export interface AttendanceChartPoint {
  label: string;
  present: number;
  absent: number;
  late: number;
  exception: number;
}

export interface PayrollSummary {
  totalEmployees: number;
  paidCount: number;
  pendingCount: number;
  totalGross: number;
  totalPaid: number;
  totalPending: number;
}

export interface DepartmentMetric {
  departmentId: string;
  departmentName: string;
  employeeCount: number;
  presentDays: number;
  scheduledDays: number;
  rate: number;
}

export interface TrendPoint {
  label: string;
  year: number;
  month: number;
  scheduledEmployeeDays: number;
  presentEmployeeDays: number;
  attendanceRate: number;
  exceptionDays: number;
  overtimeMinutes: number;
  paidCount: number;
  pendingCount: number;
  totalGross: number;
  totalPaid: number;
}
