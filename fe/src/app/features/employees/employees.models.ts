import { AttendanceCategory } from '../categories/categories.models';
export type EmploymentType = 'FIXED' | 'DAILY';
export type ContractType = 'PERMANENT' | 'FIXED_TERM' | 'PROBATIONARY' | 'PART_TIME' | 'CONSULTANT' | 'SEASONAL';
export type ContractStatus = 'DRAFT' | 'ACTIVE' | 'AMENDED' | 'EXPIRED' | 'TERMINATED';

export interface Employee {
  id: string;
  employeeCode: string;
  fullName: string;
  deviceUserId: string | null;
  categoryId: string;
  categoryName: string;
  employmentType: EmploymentType;
  baseSalary: number;
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
  baseSalary: number;
  activeFrom: number;
  activeTo: number | null;
  active: boolean;
  version: number | null;
}

export interface EmployeeContract {
  id: string;
  contractNumber: string;
  employeeId: string;
  contractType: ContractType;
  status: ContractStatus;
  startDate: string;
  endDate?: string;
  probationEndDate?: string;
  noticePeriodDays: number;
  basicSalary: number;
  housingAllowance: number;
  transportationAllowance: number;
  otherAllowances: number;
  grossSalary: number;
  jobTitle?: string;
  departmentId?: string;
  notes?: string;
  amendmentReason?: string;
  previousContractId?: string;
  createdAt: number;
  updatedAt: number;
  version: number;
}

export interface CreateContractPayload {
  contractNumber?: string;
  contractType: ContractType;
  startDate: string;
  endDate?: string;
  probationEndDate?: string;
  noticePeriodDays: number;
  basicSalary: number;
  housingAllowance: number;
  transportationAllowance: number;
  otherAllowances: number;
  jobTitle?: string;
  departmentId?: string;
  notes?: string;
}

export interface AmendContractPayload {
  newContractNumber?: string;
  basicSalary: number;
  housingAllowance: number;
  transportationAllowance: number;
  otherAllowances: number;
  jobTitle?: string;
  endDate?: string;
  amendmentReason: string;
}

export interface TerminateContractPayload {
  terminationDate: string;
  reason: string;
}

export type { AttendanceCategory };
