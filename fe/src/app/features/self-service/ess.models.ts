export interface EssProfileDto {
  employeeId: string;
  employeeCode: string;
  fullName: string;
  categoryId: string;
  categoryName: string;
  employmentType: string;
  activeFrom: string;
  baseSalary: number;
  annualLeaveRemainingDays: number;
  pendingLeavesCount: number;
  pendingAdvancesCount: number;
  currentMonthPunchesCount: number;
  lastPunchTime?: string;
  lastPunchType?: string;
}

export interface EssPayslipSummaryDto {
  paymentId: string;
  periodYear: number;
  periodMonth: number;
  periodKind: string;
  periodStart: string;
  periodEnd: string;
  grossTotal: number;
  totalDeductions: number;
  netPay: number;
  paymentStatus: string;
  paidAt?: number;
}

export interface EssExplanationItemDto {
  componentType: string;
  label: string;
  amount: number;
  calculationNote: string;
}

export interface EssPayslipDetailDto {
  paymentId: string;
  periodYear: number;
  periodMonth: number;
  periodKind: string;
  periodStart: string;
  periodEnd: string;
  baseSalary: number;
  grossTotal: number;
  totalDeductions: number;
  netPay: number;
  paymentStatus: string;
  paidAt?: number;
  scheduledDays: number;
  attendedDays: number;
  absentDays: number;
  overtimeHours: number;
  overtimeAmount: number;
  delayDeduction: number;
  absenceDeduction: number;
  advanceDeductions: number;
  bonusAmount: number;
  allowanceAmount: number;
  explanationItems: EssExplanationItemDto[];
}

export interface EssLeaveDto {
  id: string;
  requestNumber: string;
  leaveTypeId: string;
  leaveTypeName: string;
  startDate: string;
  endDate: string;
  totalDays: number;
  reason: string;
  status: string;
  createdAt: number;
}

export interface EssAdvanceDto {
  id: string;
  amount: number;
  totalInstallments: number;
  installmentAmount: number;
  remainingBalance: number;
  status: string;
  firstInstallmentDate: string;
  reason?: string;
  createdAt: number;
}

export interface EssAttendanceRecordDto {
  date: string;
  checkIn?: string;
  checkOut?: string;
  status: string;
  hoursWorked: number;
}
