export type LeaveRequestStatus = 'DRAFT' | 'PENDING_APPROVAL' | 'APPROVED' | 'REJECTED' | 'CANCELLED';

export interface LeaveType {
  id: string;
  code: string;
  nameAr: string;
  nameEn: string;
  paid: boolean;
  requiresAttachment: boolean;
  maxConsecutiveDays: number;
  createdAt: number;
}

export interface CreateLeaveTypePayload {
  code: string;
  nameAr: string;
  nameEn: string;
  paid: boolean;
  requiresAttachment: boolean;
  maxConsecutiveDays: number;
}

export interface LeaveBalance {
  id: string;
  employeeId: string;
  employeeName: string;
  leaveTypeId: string;
  leaveTypeCode: string;
  leaveTypeName: string;
  year: number;
  entitledDays: number;
  carriedOverDays: number;
  usedDays: number;
  pendingDays: number;
  remainingDays: number;
}

export interface AdjustBalancePayload {
  employeeId: string;
  leaveTypeId: string;
  year: number;
  entitledDays: number;
  carriedOverDays: number;
}

export interface LeaveRequest {
  id: string;
  requestNumber: string;
  employeeId: string;
  employeeName: string;
  leaveTypeId: string;
  leaveTypeCode: string;
  leaveTypeName: string;
  startDate: string;
  endDate: string;
  totalDays: number;
  status: LeaveRequestStatus;
  reason?: string;
  rejectionReason?: string;
  approverUserId?: string;
  approvedAt?: number;
  createdAt: number;
  updatedAt: number;
  version: number;
}

export interface SubmitLeaveRequestPayload {
  employeeId: string;
  leaveTypeId: string;
  startDate: string;
  endDate: string;
  reason?: string;
}

export interface RejectLeaveRequestPayload {
  rejectionReason: string;
}
