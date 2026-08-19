export type ReportShift = 'DAY' | 'NIGHT' | 'FULL_DAY';

export type WeatherCondition =
  | 'SUNNY'
  | 'CLEAR'
  | 'RAINY'
  | 'WINDY'
  | 'HOT'
  | 'DUSTY'
  | 'STORMY'
  | 'OTHER';

export type DailyReportStatus = 'DRAFT' | 'SUBMITTED' | 'APPROVED' | 'REOPENED';

export type LaborSourceType = 'DIRECT_EMPLOYEE' | 'CONTRACTOR_WORKER' | 'SUBCONTRACTOR_CREW';

export type EquipmentSiteStatus = 'WORKING' | 'STANDBY' | 'BREAKDOWN' | 'MAINTENANCE';

export interface WorkProgressLineResponse {
  id: string;
  dailyReportId: string;
  wbsNodeId: string;
  wbsCode: string;
  wbsName: string;
  unitOfMeasure?: string | null;
  previousQuantity: number;
  todayQuantity: number;
  cumulativeQuantity: number;
  percentComplete: number;
  plannedQuantity?: number | null;
  locationNotes?: string | null;
  remarks?: string | null;
}

export interface LaborSnapshotResponse {
  id: string;
  dailyReportId: string;
  wbsNodeId?: string | null;
  costCodeId?: string | null;
  tradeCategory: string;
  sourceType: LaborSourceType;
  partyId?: string | null;
  headcount: number;
  hoursWorked: number;
  totalManHours: number;
  activityDescription?: string | null;
}

export interface EquipmentLogResponse {
  id: string;
  dailyReportId: string;
  wbsNodeId?: string | null;
  equipmentType: string;
  equipmentCode?: string | null;
  status: EquipmentSiteStatus;
  hoursOperated: number;
  hoursIdle: number;
  fuelConsumedLiters?: number | null;
  operatorName?: string | null;
  notes?: string | null;
}

export interface MaterialConsumptionResponse {
  id: string;
  dailyReportId: string;
  wbsNodeId?: string | null;
  materialName: string;
  unitOfMeasure: string;
  quantityUsed: number;
  deliveryNoteNumber?: string | null;
  supplierPartyId?: string | null;
  notes?: string | null;
}

export interface DailyReportResponse {
  id: string;
  projectId: string;
  reportNumber: string;
  reportDate: number;
  shift: ReportShift;
  weatherCondition?: WeatherCondition | null;
  temperatureCelsius?: number | null;
  status: DailyReportStatus;
  siteEngineerUserId?: string | null;
  approverUserId?: string | null;
  approvedAt?: number | null;
  reopenedAt?: number | null;
  generalNotes?: string | null;
  blockersAndIssues?: string | null;
  safetyObservations?: string | null;
  totalWorkforceCount: number;
  totalEquipmentCount: number;
  totalManHours: number;
  createdAt: number;
  updatedAt: number;
  version: number;
  progressLines: WorkProgressLineResponse[];
  laborSnapshots: LaborSnapshotResponse[];
  equipmentLogs: EquipmentLogResponse[];
  materialConsumptions: MaterialConsumptionResponse[];
}

export interface CreateWorkProgressLineRequest {
  wbsNodeId: string;
  todayQuantity: number;
  locationNotes?: string | null;
  remarks?: string | null;
}

export interface CreateLaborSnapshotRequest {
  wbsNodeId?: string | null;
  costCodeId?: string | null;
  tradeCategory: string;
  sourceType: LaborSourceType;
  partyId?: string | null;
  headcount: number;
  hoursWorked: number;
  activityDescription?: string | null;
}

export interface CreateEquipmentLogRequest {
  wbsNodeId?: string | null;
  equipmentType: string;
  equipmentCode?: string | null;
  status: EquipmentSiteStatus;
  hoursOperated: number;
  hoursIdle: number;
  fuelConsumedLiters?: number | null;
  operatorName?: string | null;
  notes?: string | null;
}

export interface CreateMaterialConsumptionRequest {
  wbsNodeId?: string | null;
  materialName: string;
  unitOfMeasure: string;
  quantityUsed: number;
  deliveryNoteNumber?: string | null;
  supplierPartyId?: string | null;
  notes?: string | null;
}

export interface CreateDailyReportRequest {
  reportDate: number;
  shift?: ReportShift | null;
  weatherCondition?: WeatherCondition | null;
  temperatureCelsius?: number | null;
  generalNotes?: string | null;
  blockersAndIssues?: string | null;
  safetyObservations?: string | null;
  progressLines?: CreateWorkProgressLineRequest[];
  laborSnapshots?: CreateLaborSnapshotRequest[];
  equipmentLogs?: CreateEquipmentLogRequest[];
  materialConsumptions?: CreateMaterialConsumptionRequest[];
}

export interface UpdateDailyReportRequest {
  shift?: ReportShift | null;
  weatherCondition?: WeatherCondition | null;
  temperatureCelsius?: number | null;
  generalNotes?: string | null;
  blockersAndIssues?: string | null;
  safetyObservations?: string | null;
  progressLines?: CreateWorkProgressLineRequest[];
  laborSnapshots?: CreateLaborSnapshotRequest[];
  equipmentLogs?: CreateEquipmentLogRequest[];
  materialConsumptions?: CreateMaterialConsumptionRequest[];
}

export interface DprWbsProgressSummary {
  wbsNodeId: string;
  wbsCode: string;
  wbsName: string;
  unitOfMeasure?: string | null;
  plannedQuantity: number;
  totalQuantityExecuted: number;
  percentComplete: number;
}

export interface DprLaborTradeSummary {
  tradeCategory: string;
  sourceType: LaborSourceType;
  totalHeadcount: number;
  totalManHours: number;
}

export interface DprMaterialUsageSummary {
  materialName: string;
  unitOfMeasure: string;
  totalQuantityUsed: number;
}

export interface DprPeriodSummaryResponse {
  projectId: string;
  startDate: number;
  endDate: number;
  totalReportsCount: number;
  approvedReportsCount: number;
  totalManDays: number;
  totalManHours: number;
  totalEquipmentOperatingHours: number;
  totalFuelLiters: number;
  wbsProgress: DprWbsProgressSummary[];
  laborBreakdown: DprLaborTradeSummary[];
  materialUsage: DprMaterialUsageSummary[];
}
