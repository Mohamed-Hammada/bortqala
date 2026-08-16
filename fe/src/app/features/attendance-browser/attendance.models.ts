export interface AttendanceMonthSummary {
  month: string;
  punchCount: number;
  employeeCount: number;
  mappedEmployeeCount: number;
  unmatchedEmployeeCount: number;
  firstPunch: number;
  lastPunch: number;
}

export interface AttendanceEmployeeSummary {
  deviceUserId: string;
  observedName: string | null;
  employeeId: string | null;
  employeeCode: string | null;
  employeeName: string | null;
  mapped: boolean;
  punchCount: number;
  firstPunch: number;
  lastPunch: number;
}

export interface AttendanceDay {
  date: string;
  firstPunch: number;
  lastPunch: number | null;
  punchCount: number;
  workedMinutes: number;
  incomplete: boolean;
  punches: number[];
}

export interface EmployeeAttendanceDetails {
  deviceUserId: string;
  observedName: string | null;
  employeeId: string | null;
  employeeCode: string | null;
  employeeName: string | null;
  mapped: boolean;
  month: string;
  punchCount: number;
  firstPunch: number;
  lastPunch: number;
  workedMinutes: number;
  days: AttendanceDay[];
}
