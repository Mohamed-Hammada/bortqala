export type CycleStatus = 'DRAFT' | 'ACTIVE' | 'LOCKED' | 'CLOSED';
export type KpiCategory = 'OPERATIONAL' | 'STRATEGIC' | 'FINANCIAL' | 'BEHAVIORAL';
export type RatingBand =
  | 'OUTSTANDING'
  | 'EXCEEDS_EXPECTATIONS'
  | 'MEETS_EXPECTATIONS'
  | 'NEEDS_IMPROVEMENT'
  | 'UNSATISFACTORY';
export type AppraisalStatus = 'DRAFT' | 'SUBMITTED' | 'REVIEWED' | 'FINALIZED';

export interface PerformanceCycle {
  id: string;
  nameAr: string;
  nameEn: string;
  periodYear: number;
  startDate: string;
  endDate: string;
  status: CycleStatus;
  createdAt: number;
}

export interface PerformanceKpi {
  id: string;
  cycleId: string;
  code: string;
  titleAr: string;
  titleEn: string;
  category: KpiCategory;
  targetValue: number;
  weightPercentage: number;
  createdAt: number;
}

export interface AppraisalKpiScore {
  id: string;
  kpiId: string;
  kpiCode: string;
  kpiTitleAr: string;
  kpiTitleEn: string;
  category: KpiCategory;
  weightPercentage: number;
  selfRating?: number;
  managerRating?: number;
  weightedScore?: number;
  comments?: string;
}

export interface PerformanceAppraisal {
  id: string;
  cycleId: string;
  cycleNameAr: string;
  cycleNameEn: string;
  employeeId: string;
  employeeName: string;
  employeeCode: string;
  reviewerId?: string;
  reviewerName?: string;
  selfScore?: number;
  managerScore?: number;
  finalScore?: number;
  ratingBand?: RatingBand;
  status: AppraisalStatus;
  managerFeedback?: string;
  developmentPlan?: string;
  scores: AppraisalKpiScore[];
  createdAt: number;
  updatedAt: number;
  version: number;
}

export interface CreateCyclePayload {
  nameAr: string;
  nameEn: string;
  periodYear: number;
  startDate: string;
  endDate: string;
}

export interface CreateKpiPayload {
  cycleId: string;
  code: string;
  titleAr: string;
  titleEn: string;
  category: KpiCategory;
  targetValue: number;
  weightPercentage: number;
}

export interface InitAppraisalPayload {
  cycleId: string;
  employeeId: string;
  reviewerId?: string;
}

export interface KpiScoreInput {
  kpiId: string;
  selfRating?: number;
  managerRating?: number;
  comments?: string;
}

export interface SubmitAppraisalPayload {
  kpiScores: KpiScoreInput[];
  managerFeedback: string;
  developmentPlan: string;
}
