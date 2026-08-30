export type EtaEnvironment = 'PRE_PRODUCTION' | 'PRODUCTION';
export type EtaDocumentType = 'INVOICE' | 'CREDIT_NOTE' | 'DEBIT_NOTE' | 'RECEIPT';
export type EtaSubmissionStatus = 'DRAFT' | 'VALIDATED' | 'SUBMITTED' | 'VALID' | 'INVALID' | 'CANCELLED';

export interface EtaConfig {
  id: string;
  clientId: string;
  maskedSecret: string;
  issuerTaxId: string;
  issuerName: string;
  environment: EtaEnvironment;
  tokenUrl: string;
  apiBaseUrl: string;
  active: boolean;
  updatedAt: number;
}

export interface SaveEtaConfigRequest {
  clientId: string;
  clientSecret?: string;
  issuerTaxId: string;
  issuerName: string;
  environment: EtaEnvironment;
  tokenUrl?: string;
  apiBaseUrl?: string;
  active: boolean;
}

export interface EtaSubmission {
  id: string;
  invoiceId: string;
  internalId: string;
  documentType: EtaDocumentType;
  etaUuid: string | null;
  submissionUuid: string | null;
  status: EtaSubmissionStatus;
  dateTimeIssued: number;
  totalSalesAmount: number;
  totalDiscountAmount: number;
  netAmount: number;
  taxAmount: number;
  totalAmount: number;
  canonicalJsonHash: string | null;
  rawResponseJson: string | null;
  validationErrorsJson: string | null;
  submissionAttempts: number;
  cancellationReason: string | null;
  createdAt: number;
  updatedAt: number;
  version: number;
}

export interface EtaSummary {
  totalSubmitted: number;
  validCount: number;
  invalidCount: number;
  pendingCount: number;
  totalTaxReported: number;
}

export interface EtaItemMapping {
  id: string;
  itemId: string;
  itemCode: string;
  codeType: string;
  itemCodeValue: string;
  descriptionAr: string | null;
  descriptionEn: string | null;
  active: boolean;
  createdAt: number;
}

export interface SaveEtaItemMappingRequest {
  itemId: string;
  itemCode: string;
  codeType: string;
  itemCodeValue: string;
  descriptionAr?: string;
  descriptionEn?: string;
  active: boolean;
}
