export interface PermissionDto {
  id: string;
  permissionKey: string;
  module: string;
  submodule?: string;
  descriptionKey?: string;
  action?: string;
  isSystem: boolean;
}

export interface ModulePermissionTreeDto {
  module: string;
  permissions: PermissionDto[];
}

export interface PolicyCatalogResponse {
  modules: ModulePermissionTreeDto[];
  totalPermissions: number;
}

export interface PolicyGroupSummaryDto {
  id: string;
  groupName: string;
  description?: string;
  isSystem: boolean;
  permissionsCount: number;
  assignedUsersCount: number;
  createdAt: number;
  updatedAt: number;
  version: number;
}

export interface PolicyGroupDetailDto {
  id: string;
  groupName: string;
  description?: string;
  isSystem: boolean;
  permissionKeys: string[];
  assignedUsersCount: number;
  createdAt: number;
  updatedAt: number;
  version: number;
}

export interface CreatePolicyGroupRequest {
  groupName: string;
  description?: string;
  permissionKeys: string[];
}

export interface UpdatePolicyGroupRequest {
  groupName: string;
  description?: string;
  permissionKeys: string[];
  version?: number;
}

export interface UserPolicyAssignmentItem {
  policyGroupId: string;
  scopeBranchId?: string;
  scopeCostCenterId?: string;
}

export interface AssignUserPoliciesRequest {
  assignments: UserPolicyAssignmentItem[];
}

export interface UserPolicyAssignmentDto {
  id: string;
  userId: string;
  policyGroupId: string;
  policyGroupName: string;
  scopeBranchId?: string;
  scopeCostCenterId?: string;
  assignedAt: number;
}

export interface UserEffectivePermissionsResponse {
  userId: string;
  username: string;
  isAdmin: boolean;
  permissions: string[];
  branchScopes: string[];
  costCenterScopes: string[];
}
