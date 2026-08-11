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
  decisionPolicy: 'ANY_N';
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
  dueAt?: number;
  overdue: boolean;
  escalationLevel: number;
  approvalsReceived: number;
  approvalsRequired: number;
  delegatedFrom?: string;
}

export interface DecisionRecord {
  id: string;
  instanceId: string;
  stepId: string;
  decision: 'APPROVED' | 'REJECTED' | 'DELEGATED';
  comment?: string;
  decidedBy: string;
  decidedAt: number;
  delegatedFrom?: string;
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
  workflowDefinitionVersion: number;
  documentSnapshotJson: string;
  stepDueAt?: number;
  overdue: boolean;
  escalationLevel: number;
  approvalsReceived: number;
  approvalsRequired: number;
  history: DecisionRecord[];
}

export interface ApprovalDelegation {
  id: string;
  delegatorUserId: string;
  delegateUserId: string;
  documentType?: string;
  startsAt: number;
  endsAt: number;
  reason: string;
  active: boolean;
  createdBy: string;
  createdAt: number;
  version: number;
}

export interface ApprovalDelegationPayload {
  delegatorUserId: string;
  delegateUserId: string;
  documentType?: string;
  startsAt: number;
  endsAt: number;
  reason: string;
}
