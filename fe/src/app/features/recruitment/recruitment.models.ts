export type OpeningStatus = 'DRAFT' | 'OPEN' | 'CLOSED';
export type ApplicationStage = 'NEW' | 'SCREENING' | 'INTERVIEW' | 'OFFER' | 'HIRED' | 'REJECTED';

export interface JobOpening {
  id: string;
  titleAr: string;
  titleEn: string;
  departmentId: string | null;
  headcount: number;
  status: OpeningStatus;
  description: string | null;
  published: boolean;
  applicationCount: number;
  createdAt: number;
  updatedAt: number;
  version: number;
}

export interface JobApplication {
  id: string;
  openingId: string;
  fullName: string;
  phone: string | null;
  email: string | null;
  source: string | null;
  cvAttachmentId: string | null;
  stage: ApplicationStage;
  rating: number | null;
  notes: string | null;
  convertedEmployeeId: string | null;
  createdAt: number;
  updatedAt: number;
  version: number;
}

export interface StageEvent {
  id: string;
  fromStage: ApplicationStage;
  toStage: ApplicationStage;
  actor: string;
  note: string | null;
  eventAt: number;
}

export interface DuplicateWarning {
  applicationId: string;
  fullName: string;
  matchedBy: 'phone' | 'email';
}

export interface CreateOpeningRequest {
  titleAr: string;
  titleEn: string;
  departmentId?: string;
  headcount: number;
  description?: string;
}

export interface UpdateOpeningRequest {
  titleAr: string;
  titleEn: string;
  departmentId?: string;
  headcount: number;
  description?: string;
  published: boolean;
}

export interface CreateApplicationRequest {
  openingId: string;
  fullName: string;
  phone?: string;
  email?: string;
  source?: string;
  cvAttachmentId?: string;
}

export const APPLICATION_STAGES: ApplicationStage[] = [
  'NEW',
  'SCREENING',
  'INTERVIEW',
  'OFFER',
  'HIRED',
  'REJECTED',
];

export const STAGE_TRANSITIONS: Record<ApplicationStage, ApplicationStage[]> = {
  NEW: ['SCREENING', 'REJECTED'],
  SCREENING: ['INTERVIEW', 'REJECTED'],
  INTERVIEW: ['OFFER', 'REJECTED'],
  OFFER: ['HIRED', 'REJECTED'],
  HIRED: [],
  REJECTED: [],
};