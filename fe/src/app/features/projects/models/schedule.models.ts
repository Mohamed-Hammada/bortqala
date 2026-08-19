export type TaskDependencyType = 'FS' | 'SS' | 'FF' | 'SF';
export type TaskConstraintType = 'ASAP' | 'ALAP' | 'MUST_START_ON' | 'MUST_FINISH_ON' | 'START_NO_EARLIER_THAN' | 'FINISH_NO_LATER_THAN';
export type ScheduleStatus = 'DRAFT' | 'ACTIVE' | 'BASELINE_LOCKED' | 'COMPLETED';
export type TaskResourceType = 'LABOR' | 'EQUIPMENT' | 'SUBCONTRACTOR' | 'MATERIAL';

export interface TaskResourceAssignment {
  id: string;
  taskId: string;
  resourceType: TaskResourceType;
  resourceName: string;
  partyId?: string | null;
  employeeId?: string | null;
  quantityAllocated: number;
  startDate?: number | null;
  endDate?: number | null;
  notes?: string | null;
}

export interface ProjectScheduleTask {
  id: string;
  scheduleId: string;
  wbsNodeId?: string | null;
  parentTaskId?: string | null;
  taskCode: string;
  name: string;
  nameEn?: string | null;
  durationDays: number;
  plannedStartDate?: number | null;
  plannedEndDate?: number | null;
  earlyStartDate?: number | null;
  earlyEndDate?: number | null;
  lateStartDate?: number | null;
  lateEndDate?: number | null;
  freeFloatDays: number;
  totalFloatDays: number;
  isCritical: boolean;
  percentComplete: number;
  isMilestone: boolean;
  constraintType: TaskConstraintType;
  constraintDate?: number | null;
  sortOrder: number;
  resourceAssignments: TaskResourceAssignment[];
}

export interface TaskDependency {
  id: string;
  scheduleId: string;
  predecessorTaskId: string;
  successorTaskId: string;
  dependencyType: TaskDependencyType;
  lagDays: number;
  createdAt: number;
}

export interface ScheduleBaseline {
  id: string;
  scheduleId: string;
  versionNumber: number;
  name: string;
  approvedBy?: string | null;
  approvedAt?: number | null;
  notes?: string | null;
  taskCount: number;
}

export interface ScheduleBaselineComparison {
  taskId: string;
  taskCode: string;
  taskName: string;
  baselineStartDate?: number | null;
  baselineEndDate?: number | null;
  baselineDurationDays: number;
  currentStartDate?: number | null;
  currentEndDate?: number | null;
  currentDurationDays: number;
  varianceDays: number;
  isCritical: boolean;
}

export interface ResourceOverAllocation {
  resourceType: TaskResourceType;
  resourceName: string;
  date: number;
  allocatedQuantity: number;
  capacityLimit: number;
  overAllocatedAmount: number;
  affectedTaskCodes: string[];
}

export interface ProjectScheduleResponse {
  id: string;
  projectId: string;
  name: string;
  calendarCode: string;
  startDate?: number | null;
  endDate?: number | null;
  status: ScheduleStatus;
  currentBaselineVersion: number;
  totalTasksCount: number;
  criticalTasksCount: number;
  overallProgress: number;
  createdAt: number;
  updatedAt: number;
  version: number;
  tasks: ProjectScheduleTask[];
  dependencies: TaskDependency[];
  baselines: ScheduleBaseline[];
}

export interface CreateScheduleTaskRequest {
  wbsNodeId?: string | null;
  parentTaskId?: string | null;
  taskCode: string;
  name: string;
  nameEn?: string | null;
  durationDays: number;
  plannedStartDate?: number | null;
  plannedEndDate?: number | null;
  isMilestone: boolean;
  constraintType: TaskConstraintType;
  constraintDate?: number | null;
  sortOrder: number;
}

export interface UpdateScheduleTaskRequest {
  wbsNodeId?: string | null;
  parentTaskId?: string | null;
  taskCode: string;
  name: string;
  nameEn?: string | null;
  durationDays: number;
  plannedStartDate?: number | null;
  plannedEndDate?: number | null;
  isMilestone: boolean;
  constraintType: TaskConstraintType;
  constraintDate?: number | null;
  sortOrder: number;
}

export interface CreateDependencyRequest {
  predecessorTaskId: string;
  successorTaskId: string;
  dependencyType: TaskDependencyType;
  lagDays: number;
}

export interface CreateBaselineRequest {
  name: string;
  notes?: string | null;
}

export interface AssignResourceRequest {
  resourceType: TaskResourceType;
  resourceName: string;
  partyId?: string | null;
  employeeId?: string | null;
  quantityAllocated: number;
  startDate?: number | null;
  endDate?: number | null;
  notes?: string | null;
}
