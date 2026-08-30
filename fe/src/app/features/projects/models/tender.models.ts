export type TenderType = 'INTERNAL' | 'EXTERNAL';
export type TenderStatus = 'DRAFT' | 'PUBLISHED' | 'EVALUATION' | 'AWARDED' | 'CANCELLED';
export type BidderStatus = 'INVITED' | 'ACCEPTED' | 'DECLINED' | 'SUBMITTED' | 'DISQUALIFIED';

export interface ProjectTender {
  id: string;
  tenderNumber: string;
  title: string;
  titleEn?: string | null;
  tenderType: TenderType;
  projectId?: string | null;
  clientPartyId?: string | null;
  submissionDeadline: number;
  estimatedValue: number;
  currencyCode: string;
  technicalWeightPercent: number;
  financialWeightPercent: number;
  bidBondRequired: boolean;
  bidBondAmount?: number | null;
  bidBondValidityDays?: number | null;
  status: TenderStatus;
  awardedBidderId?: string | null;
  awardedBidderName?: string | null;
  awardedAmount?: number | null;
  awardedAt?: number | null;
  notes?: string | null;
  boqItemsCount: number;
  biddersCount: number;
  createdAt: number;
  updatedAt: number;
  version: number;
  boqItems?: TenderBoqItem[];
  bidders?: TenderBidder[];
  clarifications?: TenderClarification[];
}

export interface TenderBoqItem {
  id: string;
  tenderId: string;
  itemCode: string;
  description: string;
  descriptionEn?: string | null;
  unitOfMeasure: string;
  quantity: number;
  estimatedRate: number;
  estimatedAmount: number;
  sortOrder: number;
}

export interface TenderBidder {
  id: string;
  tenderId: string;
  partyId?: string | null;
  bidderName: string;
  contactEmail?: string | null;
  contactPhone?: string | null;
  status: BidderStatus;
  invitationDate?: number | null;
  submissionDate?: number | null;
  technicalScore?: number | null;
  financialScore?: number | null;
  combinedScore?: number | null;
  rankOrder?: number | null;
  totalBidAmount?: number | null;
  bidBondReceived: boolean;
  bidBondNumber?: string | null;
  bidBondExpiryDate?: number | null;
  notes?: string | null;
  submissionLines?: BidSubmissionLine[];
}

export interface BidSubmissionLine {
  id: string;
  bidderId: string;
  boqItemId: string;
  unitRate: number;
  totalAmount: number;
  technicalRemarks?: string | null;
  deviationsNotes?: string | null;
}

export interface TenderClarification {
  id: string;
  tenderId: string;
  question: string;
  askedByPartyId?: string | null;
  askedAt: number;
  answer?: string | null;
  answeredByUserId?: string | null;
  answeredAt?: number | null;
  isPublicAddendum: boolean;
  createdAt: number;
}

export interface TenderEvaluationSummary {
  tenderId: string;
  tenderNumber: string;
  lowestCompliantBidAmount: number;
  technicalWeightPercent: number;
  financialWeightPercent: number;
  evaluatedBidders: TenderBidder[];
}

export interface CreateTenderRequest {
  title: string;
  titleEn?: string | null;
  tenderType: TenderType;
  projectId?: string | null;
  clientPartyId?: string | null;
  submissionDeadline: number;
  estimatedValue?: number | null;
  currencyCode?: string | null;
  technicalWeightPercent?: number | null;
  financialWeightPercent?: number | null;
  bidBondRequired: boolean;
  bidBondAmount?: number | null;
  bidBondValidityDays?: number | null;
  notes?: string | null;
}

export interface UpdateTenderRequest {
  title: string;
  titleEn?: string | null;
  tenderType: TenderType;
  projectId?: string | null;
  clientPartyId?: string | null;
  submissionDeadline: number;
  estimatedValue?: number | null;
  currencyCode?: string | null;
  technicalWeightPercent?: number | null;
  financialWeightPercent?: number | null;
  bidBondRequired: boolean;
  bidBondAmount?: number | null;
  bidBondValidityDays?: number | null;
  notes?: string | null;
}

export interface CreateBoqItemRequest {
  itemCode: string;
  description: string;
  descriptionEn?: string | null;
  unitOfMeasure: string;
  quantity: number;
  estimatedRate: number;
  sortOrder: number;
}

export interface InviteBidderRequest {
  partyId?: string | null;
  bidderName: string;
  contactEmail?: string | null;
  contactPhone?: string | null;
  notes?: string | null;
}

export interface BidLineSubmission {
  boqItemId: string;
  unitRate: number;
  technicalRemarks?: string | null;
  deviationsNotes?: string | null;
}

export interface SubmitBidRequest {
  lines: BidLineSubmission[];
  notes?: string | null;
}

export interface TechnicalEvaluationRequest {
  technicalScore: number;
  remarks?: string | null;
}

export interface RecordBidBondRequest {
  received: boolean;
  bondNumber?: string | null;
  expiryDate?: number | null;
}

export interface CreateClarificationRequest {
  question: string;
  askedByPartyId?: string | null;
  isPublicAddendum: boolean;
}

export interface AnswerClarificationRequest {
  answer: string;
  isPublicAddendum: boolean;
}

export interface AwardTenderRequest {
  awardedBidderId: string;
  notes?: string | null;
  updateProjectContract: boolean;
}
