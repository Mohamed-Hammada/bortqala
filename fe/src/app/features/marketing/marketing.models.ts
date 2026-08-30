export interface Campaign {
  id: string;
  name: string;
  channel: string;
  subject: string;
  bodyAr: string;
  bodyEn: string;
  segmentSnapshot: string;
  status: string;
  scheduledAtEpochMs: number | null;
  totalRecipients: number;
  sentCount: number;
  failedCount: number;
  errorMessage: string | null;
  createdAtEpochMs: number | null;
  version: number;
}

export interface CampaignRecipient {
  id: string;
  campaignId: string;
  targetRef: string | null;
  email: string | null;
  phone: string | null;
  locale: string | null;
  status: string;
  errorMessage: string | null;
  sentAtEpochMs: number | null;
}

export interface Survey {
  id: string;
  title: string;
  description: string;
  active: boolean;
  createdAtEpochMs: number | null;
  version: number;
}

export interface SurveyQuestion {
  id: string;
  surveyId: string;
  questionText: string;
  questionType: string;
  options: string | null;
  sortOrder: number;
  required: boolean;
}

export interface ReportDataset {
  code: string;
  labelAr: string;
  labelEn: string;
  version: number;
}

export interface ReportColumn {
  name: string;
  label: string;
  labelAr: string;
  type: string;
  role: string;
  aggregate: string;
}

export interface SavedReport {
  id: string;
  name: string;
  datasetCode: string;
  datasetVersion: number;
  definition: string;
  ownerUserId: string;
  createdAtEpochMs: number | null;
  version: number;
}
