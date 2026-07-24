import { ReportStatus } from '../dashboard/dashboard.models';
export type DailyStatus =
  | 'PRESENT'
  | 'SINGLE_PUNCH'
  | 'NO_PUNCH'
  | 'MANUAL_ENTRY'
  | 'NON_WORKDAY'
  | 'HOLIDAY'
  | 'MISSING_SCHEDULE';
export type AttendanceDecision = 'DEDUCT' | 'NORMAL_DAY' | 'APPROVED_LEAVE';
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
}
export type { ReportStatus };
