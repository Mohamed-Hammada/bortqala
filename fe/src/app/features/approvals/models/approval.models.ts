export interface ApprovalWorkflowStep {
  id?: string;
  stepOrder: number;
  stepCode: string;
  name: string;
  requiredRole?: string;
  requiredUserId?: string;
  amountFrom?: number;
  amountTo?: number;
  minimumApprovals: number;
  allowSelfApproval: boolean;
  escalationHours?: number;
}

export interface ApprovalWorkflowDefinition {
  id: string;
  documentType: string;
  name: string;
  active: boolean;
  version: number;
  steps: ApprovalWorkflowStep[];
  createdAt: number;
  updatedAt: number;
}

export interface ApprovalTask {
  instanceId: string;
  documentType: string;
  documentId: string;
  currentStepOrder: number;
  stepName: string;
  requiredRole?: string;
  status: string;
  submittedBy: string;
  submittedAt: number;
}

export interface DecisionRecord {
  id: string;
  instanceId: string;
  stepId: string;
  decision: 'APPROVED' | 'REJECTED' | 'DELEGATED';
  comment?: string;
  decidedBy: string;
  decidedAt: number;
}

export interface ApprovalInstanceDetail {
  instanceId: string;
  documentType: string;
  documentId: string;
  currentStepOrder: number;
  status: string;
  submittedBy: string;
  submittedAt: number;
  completedAt?: number;
  history: DecisionRecord[];
}
