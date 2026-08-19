export type ProjectStatus = 'DRAFT' | 'ACTIVE' | 'ON_HOLD' | 'COMPLETED' | 'CLOSED';
export type WbsNodeType = 'PHASE' | 'SUB_PHASE' | 'WORK_PACKAGE' | 'BOQ_ITEM' | 'MILESTONE';
export type WbsNodeStatus = 'PLANNED' | 'IN_PROGRESS' | 'COMPLETED' | 'ON_HOLD' | 'CANCELLED';
export type CostCodeCategory = 'LABOR' | 'MATERIAL' | 'EQUIPMENT' | 'SUBCONTRACTOR' | 'OVERHEAD';
export type ProjectPartyRoleType = 'CLIENT_OWNER' | 'MAIN_CONTRACTOR' | 'SUBCONTRACTOR' | 'CONSULTANT' | 'SUPPLIER';

export interface ProjectResponse {
  id: string;
  code: string;
  name: string;
  nameEn?: string | null;
  description?: string | null;
  companyId?: string | null;
  branchId?: string | null;
  ownerPartyId?: string | null;
  projectManagerId?: string | null;
  siteAddress?: string | null;
  contractNumber?: string | null;
  contractValue: number;
  currencyCode: string;
  startDate?: number | null;
  endDate?: number | null;
  status: ProjectStatus;
  budgetBlocking: boolean;
  active: boolean;
  createdAt: number;
  updatedAt: number;
  version: number;
  totalPlannedAmount: number;
  wbsCount: number;
}

export interface ProjectSummaryResponse {
  totalProjects: number;
  activeProjects: number;
  onHoldProjects: number;
  completedProjects: number;
  closedProjects: number;
  totalContractValue: number;
  totalPlannedAmount: number;
}

export interface CreateProjectRequest {
  code: string;
  name: string;
  nameEn?: string | null;
  description?: string | null;
  companyId?: string | null;
  branchId?: string | null;
  ownerPartyId?: string | null;
  projectManagerId?: string | null;
  siteAddress?: string | null;
  contractNumber?: string | null;
  contractValue?: number | null;
  currencyCode?: string | null;
  startDate?: number | null;
  endDate?: number | null;
  budgetBlocking?: boolean | null;
}

export interface UpdateProjectRequest {
  name: string;
  nameEn?: string | null;
  description?: string | null;
  companyId?: string | null;
  branchId?: string | null;
  ownerPartyId?: string | null;
  projectManagerId?: string | null;
  siteAddress?: string | null;
  contractNumber?: string | null;
  contractValue?: number | null;
  currencyCode?: string | null;
  startDate?: number | null;
  endDate?: number | null;
  budgetBlocking?: boolean | null;
}

export interface WbsNodeResponse {
  id: string;
  projectId: string;
  parentId?: string | null;
  wbsCode: string;
  wbsPath: string;
  name: string;
  nameEn?: string | null;
  description?: string | null;
  nodeType: WbsNodeType;
  level: number;
  sortOrder: number;
  unitOfMeasure?: string | null;
  plannedQuantity: number;
  unitRate: number;
  plannedAmount: number;
  costCodeId?: string | null;
  startDate?: number | null;
  endDate?: number | null;
  status: WbsNodeStatus;
  createdAt: number;
  updatedAt: number;
  version: number;
  children: WbsNodeResponse[];
}

export interface CreateWbsNodeRequest {
  parentId?: string | null;
  wbsCode: string;
  name: string;
  nameEn?: string | null;
  description?: string | null;
  nodeType?: WbsNodeType | null;
  sortOrder?: number | null;
  unitOfMeasure?: string | null;
  plannedQuantity?: number | null;
  unitRate?: number | null;
  costCodeId?: string | null;
  startDate?: number | null;
  endDate?: number | null;
  status?: WbsNodeStatus | null;
}

export interface UpdateWbsNodeRequest {
  name: string;
  nameEn?: string | null;
  description?: string | null;
  nodeType?: WbsNodeType | null;
  unitOfMeasure?: string | null;
  plannedQuantity?: number | null;
  unitRate?: number | null;
  costCodeId?: string | null;
  startDate?: number | null;
  endDate?: number | null;
  status?: WbsNodeStatus | null;
}

export interface RepositionWbsNodeRequest {
  parentId?: string | null;
  sortOrder?: number | null;
}

export interface ProjectCostCodeResponse {
  id: string;
  code: string;
  name: string;
  nameEn?: string | null;
  category: CostCodeCategory;
  description?: string | null;
  active: boolean;
  createdAt: number;
  updatedAt: number;
  version: number;
}

export interface CreateCostCodeRequest {
  code: string;
  name: string;
  nameEn?: string | null;
  category: CostCodeCategory;
  description?: string | null;
}

export interface UpdateCostCodeRequest {
  name: string;
  nameEn?: string | null;
  category: CostCodeCategory;
  description?: string | null;
  active?: boolean | null;
}

export interface ProjectPartyRoleResponse {
  id: string;
  projectId: string;
  partyId: string;
  roleType: ProjectPartyRoleType;
  notes?: string | null;
  createdAt: number;
}

export interface AssignPartyRoleRequest {
  partyId: string;
  roleType: ProjectPartyRoleType;
  notes?: string | null;
}
