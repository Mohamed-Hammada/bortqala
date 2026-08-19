import { Injectable, inject, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import {
  ProjectResponse,
  ProjectSummaryResponse,
  CreateProjectRequest,
  UpdateProjectRequest,
  ProjectStatus,
  WbsNodeResponse,
  CreateWbsNodeRequest,
  UpdateWbsNodeRequest,
  RepositionWbsNodeRequest,
  ProjectCostCodeResponse,
  CreateCostCodeRequest,
  UpdateCostCodeRequest,
  ProjectPartyRoleResponse,
  AssignPartyRoleRequest,
} from '../models/project.models';

@Injectable({
  providedIn: 'root',
})
export class ProjectService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/projects';

  readonly projects = signal<ProjectResponse[]>([]);
  readonly summary = signal<ProjectSummaryResponse | null>(null);
  readonly currentProject = signal<ProjectResponse | null>(null);
  readonly wbsTree = signal<WbsNodeResponse[]>([]);
  readonly flatWbs = signal<WbsNodeResponse[]>([]);
  readonly costCodes = signal<ProjectCostCodeResponse[]>([]);
  readonly projectRoles = signal<ProjectPartyRoleResponse[]>([]);
  readonly loading = signal<boolean>(false);

  loadProjects(companyId?: string, status?: ProjectStatus): Observable<ProjectResponse[]> {
    this.loading.set(true);
    let params = new HttpParams();
    if (companyId) {
      params = params.set('companyId', companyId);
    }
    if (status) {
      params = params.set('status', status);
    }

    return this.http.get<ProjectResponse[]>(this.baseUrl, { params }).pipe(
      tap((data) => {
        this.projects.set(data);
        this.loading.set(false);
      })
    );
  }

  loadSummary(): Observable<ProjectSummaryResponse> {
    return this.http.get<ProjectSummaryResponse>(`${this.baseUrl}/summary`).pipe(
      tap((data) => this.summary.set(data))
    );
  }

  getProject(id: string): Observable<ProjectResponse> {
    this.loading.set(true);
    return this.http.get<ProjectResponse>(`${this.baseUrl}/${id}`).pipe(
      tap((data) => {
        this.currentProject.set(data);
        this.loading.set(false);
      })
    );
  }

  createProject(req: CreateProjectRequest): Observable<ProjectResponse> {
    return this.http.post<ProjectResponse>(this.baseUrl, req).pipe(
      tap((created) => {
        this.projects.update((list) => [created, ...list]);
      })
    );
  }

  updateProject(id: string, req: UpdateProjectRequest): Observable<ProjectResponse> {
    return this.http.put<ProjectResponse>(`${this.baseUrl}/${id}`, req).pipe(
      tap((updated) => {
        this.currentProject.set(updated);
        this.projects.update((list) => list.map((p) => (p.id === id ? updated : p)));
      })
    );
  }

  activateProject(id: string): Observable<ProjectResponse> {
    return this.http.post<ProjectResponse>(`${this.baseUrl}/${id}/activate`, {}).pipe(
      tap((updated) => {
        this.currentProject.set(updated);
        this.projects.update((list) => list.map((p) => (p.id === id ? updated : p)));
      })
    );
  }

  holdProject(id: string): Observable<ProjectResponse> {
    return this.http.post<ProjectResponse>(`${this.baseUrl}/${id}/hold`, {}).pipe(
      tap((updated) => {
        this.currentProject.set(updated);
        this.projects.update((list) => list.map((p) => (p.id === id ? updated : p)));
      })
    );
  }

  completeProject(id: string): Observable<ProjectResponse> {
    return this.http.post<ProjectResponse>(`${this.baseUrl}/${id}/complete`, {}).pipe(
      tap((updated) => {
        this.currentProject.set(updated);
        this.projects.update((list) => list.map((p) => (p.id === id ? updated : p)));
      })
    );
  }

  closeProject(id: string): Observable<ProjectResponse> {
    return this.http.post<ProjectResponse>(`${this.baseUrl}/${id}/close`, {}).pipe(
      tap((updated) => {
        this.currentProject.set(updated);
        this.projects.update((list) => list.map((p) => (p.id === id ? updated : p)));
      })
    );
  }

  reopenProject(id: string): Observable<ProjectResponse> {
    return this.http.post<ProjectResponse>(`${this.baseUrl}/${id}/reopen`, {}).pipe(
      tap((updated) => {
        this.currentProject.set(updated);
        this.projects.update((list) => list.map((p) => (p.id === id ? updated : p)));
      })
    );
  }

  deleteProject(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`).pipe(
      tap(() => {
        this.projects.update((list) => list.filter((p) => p.id !== id));
      })
    );
  }

  // ─── WBS Operations ──────────────────────────────────────────────

  loadWbsTree(projectId: string): Observable<WbsNodeResponse[]> {
    return this.http.get<WbsNodeResponse[]>(`${this.baseUrl}/${projectId}/wbs`).pipe(
      tap((tree) => this.wbsTree.set(tree))
    );
  }

  loadFlatWbs(projectId: string): Observable<WbsNodeResponse[]> {
    return this.http.get<WbsNodeResponse[]>(`${this.baseUrl}/${projectId}/wbs/flat`).pipe(
      tap((list) => this.flatWbs.set(list))
    );
  }

  createWbsNode(projectId: string, req: CreateWbsNodeRequest): Observable<WbsNodeResponse> {
    return this.http.post<WbsNodeResponse>(`${this.baseUrl}/${projectId}/wbs`, req);
  }

  updateWbsNode(projectId: string, id: string, req: UpdateWbsNodeRequest): Observable<WbsNodeResponse> {
    return this.http.put<WbsNodeResponse>(`${this.baseUrl}/${projectId}/wbs/${id}`, req);
  }

  repositionWbsNode(projectId: string, id: string, req: RepositionWbsNodeRequest): Observable<WbsNodeResponse> {
    return this.http.post<WbsNodeResponse>(`${this.baseUrl}/${projectId}/wbs/${id}/reposition`, req);
  }

  deleteWbsNode(projectId: string, id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${projectId}/wbs/${id}`);
  }

  // ─── Cost Codes ──────────────────────────────────────────────────

  loadCostCodes(activeOnly?: boolean): Observable<ProjectCostCodeResponse[]> {
    let params = new HttpParams();
    if (activeOnly !== undefined) {
      params = params.set('activeOnly', activeOnly.toString());
    }
    return this.http.get<ProjectCostCodeResponse[]>(`${this.baseUrl}/cost-codes`, { params }).pipe(
      tap((data) => this.costCodes.set(data))
    );
  }

  createCostCode(req: CreateCostCodeRequest): Observable<ProjectCostCodeResponse> {
    return this.http.post<ProjectCostCodeResponse>(`${this.baseUrl}/cost-codes`, req).pipe(
      tap((created) => {
        this.costCodes.update((list) => [...list, created]);
      })
    );
  }

  updateCostCode(id: string, req: UpdateCostCodeRequest): Observable<ProjectCostCodeResponse> {
    return this.http.put<ProjectCostCodeResponse>(`${this.baseUrl}/cost-codes/${id}`, req).pipe(
      tap((updated) => {
        this.costCodes.update((list) => list.map((c) => (c.id === id ? updated : c)));
      })
    );
  }

  deleteCostCode(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/cost-codes/${id}`).pipe(
      tap(() => {
        this.costCodes.update((list) => list.filter((c) => c.id !== id));
      })
    );
  }

  // ─── Project Stakeholder Roles ───────────────────────────────────

  loadProjectRoles(projectId: string): Observable<ProjectPartyRoleResponse[]> {
    return this.http.get<ProjectPartyRoleResponse[]>(`${this.baseUrl}/${projectId}/roles`).pipe(
      tap((data) => this.projectRoles.set(data))
    );
  }

  assignProjectRole(projectId: string, req: AssignPartyRoleRequest): Observable<ProjectPartyRoleResponse> {
    return this.http.post<ProjectPartyRoleResponse>(`${this.baseUrl}/${projectId}/roles`, req).pipe(
      tap((assigned) => {
        this.projectRoles.update((list) => [...list, assigned]);
      })
    );
  }

  removeProjectRole(projectId: string, roleId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${projectId}/roles/${roleId}`).pipe(
      tap(() => {
        this.projectRoles.update((list) => list.filter((r) => r.id !== roleId));
      })
    );
  }
}
