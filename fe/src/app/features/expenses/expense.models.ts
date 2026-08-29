export type ExpenseCategory = 'MEAL' | 'TRANSPORT' | 'LODGING' | 'SUPPLIES' | 'OTHER';
export type ExpenseStatus = 'DRAFT' | 'SUBMITTED' | 'APPROVED' | 'REJECTED' | 'REIMBURSED';

export interface ExpenseClaimResponse {
  id: string;
  employeeId: string;
  employeeName: string;
  category: ExpenseCategory;
  spentOn: string;
  amount: number;
  currency: string;
  description: string | null;
  receiptName: string | null;
  receiptContentType: string | null;
  receiptSize: number | null;
  status: ExpenseStatus;
  approverId: string | null;
  decidedAt: number | null;
  decisionNote: string | null;
  reimbursementReference: string | null;
  limitExceeded: boolean;
  createdAt: number;
  updatedAt: number;
}

export interface CreateClaimRequest {
  category: ExpenseCategory;
  spentOn: string;
  amount: number;
  currency?: string;
  description?: string;
  attachmentName?: string;
  attachmentContentType?: string;
  attachmentSize?: number;
}

export interface UpdateClaimRequest {
  category: ExpenseCategory;
  spentOn: string;
  amount: number;
  currency?: string;
  description?: string;
  attachmentName?: string;
  attachmentContentType?: string;
  attachmentSize?: number;
}

export interface DecisionRequest {
  note?: string;
}

export interface ReimburseRequest {
  reference: string;
}
