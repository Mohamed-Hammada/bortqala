export interface BusinessParty {
  id: string;
  code: string;
  name: string;
  nameEn: string | null;
  partyType: string;
  contactPerson: string | null;
  phone: string | null;
  email: string | null;
  address: string | null;
  notes: string | null;
  managedType: string;
  responsiblePartyId: string | null;
  relationshipStartDate: string | null;
  relationshipEndDate: string | null;
  currencyCode: string;
  invoicePolicy: string;
  paymentTerms: string;
  taxId: string | null;
  bankAccount: string | null;
  onboardingStatus: string;
  supplierCategory: string | null;
  riskLevel: string | null;
  ownerUserId: string | null;
  approvalInstanceId: string | null;
  bankVerified: boolean;
  bankVerifiedAt: number | null;
  bankVerifiedBy: string | null;
  active: boolean;
  version: number;
  createdAt: number;
  updatedAt: number;
}

export interface BusinessPartyPayload {
  code: string;
  name: string;
  nameEn: string | null;
  partyType: string;
  contactPerson: string | null;
  phone: string | null;
  email: string | null;
  address: string | null;
  notes: string | null;
  managedType: string;
  responsiblePartyId: string | null;
  relationshipStartDate: string | null;
  relationshipEndDate: string | null;
  currencyCode: string;
  invoicePolicy: string;
  paymentTerms: string;
  taxId: string | null;
  bankAccount: string | null;
  supplierCategory: string | null;
  riskLevel: string | null;
  ownerUserId: string | null;
  active: boolean;
  version: number | null;
}

export interface SupplierDocument {
  id: string;
  documentType: string;
  documentNumber: string | null;
  fileName: string;
  contentType: string;
  fileSize: number;
  issueDate: string | null;
  expiryDate: string | null;
  mandatory: boolean;
  verified: boolean;
  verifiedBy: string | null;
  verifiedAt: number | null;
  createdAt: number;
  expired: boolean;
}

export interface SupplierBankAccount {
  id: string;
  accountName: string;
  iban: string;
  bankName: string;
  currencyCode: string;
  primary: boolean;
  verificationStatus: 'PENDING' | 'VERIFIED';
  verifiedBy: string | null;
  verifiedAt: number | null;
  createdAt: number;
}

export interface SupplierComplianceItem { code: string; passed: boolean; explanation: string }

export interface Supplier360 {
  supplier: BusinessParty;
  documents: SupplierDocument[];
  bankAccounts: SupplierBankAccount[];
  compliance: SupplierComplianceItem[];
  documentCount: number;
  expiredDocumentCount: number;
  verifiedBankCount: number;
  procurementAllowed: boolean;
  paymentAllowed: boolean;
}

export interface SupplierDuplicateResponse {
  taxIdMatches: { supplierId: string; code: string; name: string; reason: string }[];
  bankMatches: { supplierId: string; code: string; name: string; reason: string }[];
  duplicateFound: boolean;
}

export interface AgingBreakdown {
  currentNotDue: number;
  bucket1To30: number;
  bucket31To60: number;
  bucket61To90: number;
  bucket90Plus: number;
  totalOverdue: number;
}

export interface PartyFinancialPositionSummary {
  partyId: string;
  partyCode: string;
  partyName: string;
  partyType: string;
  currencyCode: string;
  openingBalance: number;
  totalDebits: number;
  totalCredits: number;
  netClosingBalance: number;
  overdueAmount: number;
  unappliedCashAmount: number;
  retentionHeldAmount: number;
  creditLimit: number;
  creditHold: boolean;
  creditStatus: string;
  aging: AgingBreakdown;
}

export interface PartyStatementLine {
  transactionDate: number;
  documentType: string;
  documentNumber: string;
  reference?: string;
  description: string;
  debit: number;
  credit: number;
  runningBalance: number;
  dueDate?: number;
  overdueDays: number;
  sourceEntityId: string;
}

export interface PartyStatementResponse {
  summary: PartyFinancialPositionSummary;
  fromDate?: number;
  toDate?: number;
  lines: PartyStatementLine[];
}

export interface AgingReportRow {
  partyId: string;
  partyCode: string;
  partyName: string;
  partyType: string;
  totalBalance: number;
  currentNotDue: number;
  bucket1To30: number;
  bucket31To60: number;
  bucket61To90: number;
  bucket90Plus: number;
}

export interface AgingReportResponse {
  asOfDate: number;
  partyTypeFilter?: string;
  totalOutstanding: number;
  rows: AgingReportRow[];
}

export interface ArApGlReconciliationResponse {
  partyType: string;
  subledgerTotal: number;
  glControlAccountBalance: number;
  variance: number;
  isReconciled: boolean;
}
