import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  AssignUserPoliciesRequest,
  CreatePolicyGroupRequest,
  PolicyCatalogResponse,
  PolicyGroupDetailDto,
  PolicyGroupSummaryDto,
  UpdatePolicyGroupRequest,
  UserEffectivePermissionsResponse,
  UserPolicyAssignmentDto,
} from './security-policy.models';

@Injectable({ providedIn: 'root' })
export class PolicyService {
  private readonly http = inject(HttpClient);

  getCatalog(): Observable<PolicyCatalogResponse> {
    return this.http.get<PolicyCatalogResponse>('/api/v1/access/catalog/permissions');
  }

  listPolicyGroups(): Observable<PolicyGroupSummaryDto[]> {
    return this.http.get<PolicyGroupSummaryDto[]>('/api/v1/access/policy-groups');
  }

  getPolicyGroup(groupId: string): Observable<PolicyGroupDetailDto> {
    return this.http.get<PolicyGroupDetailDto>(`/api/v1/access/policy-groups/${groupId}`);
  }

  createPolicyGroup(request: CreatePolicyGroupRequest): Observable<PolicyGroupDetailDto> {
    return this.http.post<PolicyGroupDetailDto>('/api/v1/access/policy-groups', request);
  }

  updatePolicyGroup(groupId: string, request: UpdatePolicyGroupRequest): Observable<PolicyGroupDetailDto> {
    return this.http.put<PolicyGroupDetailDto>(`/api/v1/access/policy-groups/${groupId}`, request);
  }

  deletePolicyGroup(groupId: string): Observable<void> {
    return this.http.delete<void>(`/api/v1/access/policy-groups/${groupId}`);
  }

  getUserPolicies(userId: string): Observable<UserPolicyAssignmentDto[]> {
    return this.http.get<UserPolicyAssignmentDto[]>(`/api/v1/access/users/${userId}/policies`);
  }

  assignUserPolicies(userId: string, request: AssignUserPoliciesRequest): Observable<UserPolicyAssignmentDto[]> {
    return this.http.post<UserPolicyAssignmentDto[]>(`/api/v1/access/users/${userId}/policies`, request);
  }

  getMyEffectivePermissions(): Observable<UserEffectivePermissionsResponse> {
    return this.http.get<UserEffectivePermissionsResponse>('/api/v1/access/me/permissions');
  }
}
