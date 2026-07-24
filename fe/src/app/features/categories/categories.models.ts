export type DayOfWeek =
  'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY';
export type PayCycle = 'MONTHLY' | 'HALF_MONTHLY' | 'THIRTY_DAYS';
export type AttendanceMode = 'BIOMETRIC' | 'MANUAL' | 'HYBRID';
export interface ScheduleRule {
  id: string;
  name: string;
  effectiveFrom: number;
  effectiveTo: number | null;
  startTime: string;
  expectedMinutesOverride: number | null;
  graceMinutes: number;
}
export interface AttendanceCategory {
  id: string;
  code: string;
  name: string;
  expectedDailyMinutes: number;
  payCycle: PayCycle;
  attendanceMode: AttendanceMode;
  singlePunchCounts: boolean;
  workDays: DayOfWeek[];
  active: boolean;
  version: number;
  createdAt: number;
  updatedAt: number;
  schedules: ScheduleRule[];
}
export interface CategoryPayload {
  code: string;
  name: string;
  expectedDailyMinutes: number;
  payCycle: PayCycle;
  attendanceMode: AttendanceMode;
  singlePunchCounts: boolean;
  workDays: DayOfWeek[];
  active: boolean;
  version: number | null;
  schedules: Array<Omit<ScheduleRule, 'id'>>;
}
