import { ReportStatus } from '../dashboard/dashboard.models';
export type DailyStatus =
  | 'PRESENT'
  | 'SINGLE_PUNCH'
  | 'NO_PUNCH'
  | 'MANUAL_ENTRY'
  | 'NON_WORKDAY'
  | 'HOLIDAY'
  | 'MISSING_SCHEDULE';
export type AttendanceDecision = 'DEDUCT' | 'NORMAL_DAY' | 'APPROVED_LEAVE' | 'ABSENCE' | 'OFFICIAL_HOLIDAY' | 'INDIVIDUAL_REVIEW';
export type HolidayProposalStatus = 'PENDING' | 'CONFIRMED' | 'REJECTED';
export type ReportPayCycle = 'MONTHLY' | 'HALF_MONTHLY' | 'THIRTY_DAYS';
export interface ReportSummary {
  id: string;
  periodStart: number;
  periodEnd: number;
  payCycle: ReportPayCycle;
  status: ReportStatus;
  unresolvedCount: number;
  createdBy: string;
  createdAt: number;
  approvedBy: string | null;
  approvedAt: number | null;
  exportedAt: number | null;
  version: number;
  generationHash: string | null;
}
export interface ReportPeriodSelection {
  periodStart: number;
  periodEnd: number;
  payCycle: ReportPayCycle;
}
export interface PeriodOption {
  year: number;
  month: number;
  kind: 'MONTHLY' | 'FIRST_HALF' | 'SECOND_HALF';
  start: number;
  end: number;
}
export interface GeneratedPeriod {
  from: number;
  to: number;
  type: string;
  reportId: string;
}
export interface PreviewCategory {
  categoryId: string;
  categoryName: string;
  employeeCount: number;
}
export interface ReportPreview {
  periodStart: number;
  periodEnd: number;
  payCycle: ReportPayCycle;
  categories: PreviewCategory[];
  employeeCount: number;
  workdays: number;
  scheduleCoverageCount: number;
  existingReportId: string | null;
  overlappingReportIds: string[];
}
export interface CategorySummary {
  categoryId: string;
  categoryName: string;
  employeeDays: number;
  presentDays: number;
  exceptionDays: number;
  typicalArrival: string | null;
  overtimeMinutes: number;
}
export interface DailyResult {
  id: string;
  employeeId: string;
  employeeCode: string;
  employeeName: string;
  categoryId: string;
  categoryName: string;
  workDate: number;
  firstPunch: number | null;
  lastPunch: number | null;
  punchCount: number;
  expectedMinutes: number;
  workedMinutes: number;
  manualWorkedMinutes: number | null;
  effectiveWorkedMinutes: number;
  lateMinutes: number;
  earlyLeaveMinutes: number;
  overtimeMinutes: number;
  status: DailyStatus;
  warning: string | null;
  decision: AttendanceDecision | null;
  decisionNote: string | null;
  decidedBy: string | null;
  decidedAt: number | null;
  ruleVersion: string;
  version: number;
}
export interface HolidayProposal {
  id: string;
  categoryId: string;
  categoryName: string;
  workDate: number;
  activeEmployeeCount: number;
  status: HolidayProposalStatus;
  note: string | null;
  decidedBy: string | null;
  decidedAt: number | null;
}
export interface ReportDetails {
  report: ReportSummary;
  categories: CategorySummary[];
  dailyResults: DailyResult[];
  holidayProposals: HolidayProposal[];
  dayAnomalies: DayAnomaly[];
  allowedActions: string[];
}
export interface BulkDecisionRequest {
  decision: AttendanceDecision;
  statusFilter: string;
  note?: string;
  operationId: string;
}

export interface BulkDecisionResponse {
  matchingCount: number;
  editableCount: number;
  excludedCount: number;
  successCount: number;
  excludedRecordIds: string[];
}

export interface DowntimeDecisionRequest {
  date: string;
  categoryId: string;
  location: string;
  decision: string;
  note?: string;
}

export type DayAnomalyStatus = 'OPEN' | 'DEFERRED' | 'RESOLVED' | 'REVERSED';
export type DayAnomalyDecision = 'DEVICE_OUTAGE' | 'OFFICIAL_HOLIDAY' | 'ABSENCE' | 'PRESENT' | 'DEFER';
export interface DayAnomaly {
  id: string;
  reportId: string;
  workDate: number;
  categoryId: string;
  categoryName: string;
  location: string | null;
  affectedCount: number;
  totalEmployeeCount: number;
  absencePercentage: number;
  thresholdPercentage: number;
  affectedExpectedMinutes: number;
  status: DayAnomalyStatus;
  decision: DayAnomalyDecision | null;
  reason: string | null;
  decidedBy: string | null;
  decidedAt: number | null;
  reversedBy: string | null;
  reversedAt: number | null;
  reopenedBy: string | null;
  reopenedAt: number | null;
  createdAt: number;
}
export interface DayAnomalyDecisionRequest {
  decision: DayAnomalyDecision;
  reason: string;
  operationId: string;
}
export interface DayAnomalyActionResponse {
  details: ReportDetails;
  affectedCount: number;
  appliedCount: number;
  skippedCount: number;
}

export type AttendanceExceptionType = 'NO_PUNCH' | 'SINGLE_PUNCH' | 'MISSING_SCHEDULE' | 'LATE' | 'EARLY_LEAVE' | 'EXCESS_SHIFT';
export type AttendanceExceptionResolution = 'ACCEPT' | 'MARK_PRESENT' | 'MARK_ABSENT' | 'IGNORE';
export interface AttendanceExceptionView {
  id: string; reportId: string; dailyResultId: string; employeeId: string; employeeName: string;
  categoryId: string; categoryName: string; workDate: number; exceptionType: AttendanceExceptionType;
  score: number; metricMinutes: number; explanationKey: string; policyId?: string; policyName: string; policyVersion: number; policySnapshotJson: string;
  policyScope: 'TENANT' | 'CATEGORY' | 'EMPLOYEE'; payrollBlocking: boolean;
  status: 'OPEN' | 'RESOLVED' | 'OVERRIDDEN' | 'IGNORED'; resolution?: AttendanceExceptionResolution;
  reason?: string; version: number;
}
export interface AttendanceExceptionWorkbench {
  summary: { total: number; open: number; critical: number; resolved: number; affectedEmployees: number };
  exceptions: AttendanceExceptionView[];
}
export interface AttendanceExceptionBulkRequest {
  exceptionIds: string[]; resolution: AttendanceExceptionResolution; reason: string; operationId: string;
}
export interface AttendanceExceptionBulkPreview {
  selected: number; editable: number; alreadyClosed: number; payrollBlockersCleared: number; excludedIds: string[];
}
export interface AttendanceExceptionBulkResult {
  workbench: AttendanceExceptionWorkbench; applied: number; replayed: number; skipped: number;
}

export type { ReportStatus };
