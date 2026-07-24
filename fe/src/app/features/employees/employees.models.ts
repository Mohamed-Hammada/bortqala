import { AttendanceCategory } from '../categories/categories.models';
export type EmploymentType = 'FIXED' | 'DAILY';
export interface Employee {
  id: string;
  employeeCode: string;
  fullName: string;
  deviceUserId: string | null;
  categoryId: string;
  categoryName: string;
  employmentType: EmploymentType;
  activeFrom: number;
  activeTo: number | null;
  active: boolean;
  version: number;
}
export interface EmployeePayload {
  employeeCode: string;
  fullName: string;
  deviceUserId: string | null;
  categoryId: string;
  employmentType: EmploymentType;
  activeFrom: number;
  activeTo: number | null;
  active: boolean;
  version: number | null;
}
export type { AttendanceCategory };
