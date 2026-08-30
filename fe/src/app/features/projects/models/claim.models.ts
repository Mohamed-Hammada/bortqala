export type ClaimType = 'OWNER_IPC' | 'SUBCONTRACTOR_IPC';
export type ClaimKind = 'INTERIM' | 'FINAL_ACCOUNT';
export type ClaimStatus =
  | 'DRAFT'
  | 'SUBMITTED'
  | 'REVIEWED'
  | 'CERTIFIED'
  | 'POSTED_FINANCE'
  | 'PAID'
  | 'CANCELLED';

export type ClaimLineType = 'BOQ_ITEM' | 'VARIATION_ORDER' | 'MATERIAL_ON_SITE' | 'DAYWORK';

export type AdjustmentType =
  | 'RETENTION'
  | 'ADVANCE_RECOVERY'
  | 'VAT_TAX'
  | 'WITHHOLDING_TAX'
  | 'PENALTY_DEDUCTION'
  | 'OTHER_DEDUCTION';

export interface ProgressClaimLine {
  id: string;
  claimId: string;
  lineType: ClaimLineType;
  wbsNodeId?: string | null;
  itemCode: string;
  description: string;
  unitOfMeasure: string;
  contractQuantity: number;
  unitRate: number;
  previousQuantity: number;
  currentQuantity: number;
  cumulativeQuantity: number;
  previousAmount: number;
  currentAmount: number;
  cumulativeAmount: number;
  percentComplete: number;
  remarks?: string | null;
  sortOrder: number;
}

export interface ProgressClaimAdjustment {
  id: string;
  claimId: string;
  adjustmentType: AdjustmentType;
  description: string;
  percentageRate?: number | null;
  calculationBasisAmount: number;
  adjustmentAmount: number;
  isAddition: boolean;
  notes?: string | null;
}

export interface ProjectProgressClaim {
  id: string;
  claimNumber: string;
  claimType: ClaimType;
  claimKind: ClaimKind;
  claimSequenceNumber: number;
  projectId: string;
  partyId?: string | null;
  partyName?: string | null;
  periodStartDate: string; // ISO date YYYY-MM-DD
  periodEndDate: string;
  submissionDate?: number | null;
  currencyCode: string;
  previousGrossAmount: number;
  currentGrossAmount: number;
  cumulativeGrossAmount: number;
  previousRetentionAmount: number;
  currentRetentionAmount: number;
  cumulativeRetentionAmount: number;
  previousAdvanceRecoveryAmount: number;
  currentAdvanceRecoveryAmount: number;
  cumulativeAdvanceRecoveryAmount: number;
  currentTaxAmount: number;
  currentDeductionsAmount: number;
  currentNetPayableAmount: number;
  cumulativeNetPaidAmount: number;
  status: ClaimStatus;
  certifiedByUserId?: string | null;
  certifiedAt?: number | null;
  certificationNotes?: string | null;
  postedFinanceJournalId?: string | null;
  postedInvoiceId?: string | null;
  postedAt?: number | null;
  notes?: string | null;
  linesCount: number;
  createdAt: number;
  updatedAt: number;
  version: number;
  lines?: ProgressClaimLine[];
  adjustments?: ProgressClaimAdjustment[];
}

export interface CreateProgressClaimRequest {
  claimType: ClaimType;
  claimKind: ClaimKind;
  projectId: string;
  partyId?: string | null;
  periodStartDate: string;
  periodEndDate: string;
  currencyCode?: string | null;
  notes?: string | null;
  initFromWbs: boolean;
}

export interface UpdateProgressClaimRequest {
  claimKind: ClaimKind;
  partyId?: string | null;
  periodStartDate: string;
  periodEndDate: string;
  currencyCode?: string | null;
  notes?: string | null;
  lines?: SaveClaimLineRequest[];
  adjustments?: SaveClaimAdjustmentRequest[];
}

export interface SaveClaimLineRequest {
  id?: string | null;
  lineType?: ClaimLineType;
  wbsNodeId?: string | null;
  itemCode: string;
  description: string;
  unitOfMeasure?: string | null;
  contractQuantity?: number;
  unitRate: number;
  previousQuantity?: number;
  currentQuantity: number;
  remarks?: string | null;
  sortOrder?: number;
}

export interface SaveClaimAdjustmentRequest {
  id?: string | null;
  adjustmentType: AdjustmentType;
  description: string;
  percentageRate?: number | null;
  fixedAmount?: number | null;
  isAddition: boolean;
  notes?: string | null;
}

export interface CertifyClaimRequest {
  notes?: string | null;
}
