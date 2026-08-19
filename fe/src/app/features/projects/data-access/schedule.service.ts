import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import {
  ProjectScheduleResponse,
  ProjectScheduleTask,
  CreateScheduleTaskRequest,
  UpdateScheduleTaskRequest,
  CreateDependencyRequest,
  TaskDependency,
  CreateBaselineRequest,
  ScheduleBaseline,
  ScheduleBaselineComparison,
  AssignResourceRequest,
  TaskResourceAssignment,
  ResourceOverAllocation,
} from '../models/schedule.models';

@Injectable({
  providedIn: 'root',
})
export class ScheduleService {
  private readonly http = inject(HttpClient);

  readonly schedule = signal<ProjectScheduleResponse | null>(null);
  readonly baselineComparison = signal<ScheduleBaselineComparison[]>([]);
  readonly overAllocations = signal<ResourceOverAllocation[]>([]);
  readonly loading = signal<boolean>(false);

  private getUrl(projectId: string): string {
    return `/api/v1/projects/${projectId}/schedule`;
  }

  loadSchedule(projectId: string): Observable<ProjectScheduleResponse> {
    this.loading.set(true);
    return this.http.get<ProjectScheduleResponse>(this.getUrl(projectId)).pipe(
      tap((data) => {
        this.schedule.set(data);
        this.loading.set(false);
      })
    );
  }

  createTask(projectId: string, req: CreateScheduleTaskRequest): Observable<ProjectScheduleTask> {
    return this.http.post<ProjectScheduleTask>(`${this.getUrl(projectId)}/tasks`, req).pipe(
      tap(() => {
        this.loadSchedule(projectId).subscribe();
      })
    );
  }

  updateTask(projectId: string, taskId: string, req: UpdateScheduleTaskRequest): Observable<ProjectScheduleTask> {
    return this.http.put<ProjectScheduleTask>(`${this.getUrl(projectId)}/tasks/${taskId}`, req).pipe(
      tap(() => {
        this.loadSchedule(projectId).subscribe();
      })
    );
  }

  deleteTask(projectId: string, taskId: string): Observable<void> {
    return this.http.delete<void>(`${this.getUrl(projectId)}/tasks/${taskId}`).pipe(
      tap(() => {
        this.loadSchedule(projectId).subscribe();
      })
    );
  }

  addDependency(projectId: string, req: CreateDependencyRequest): Observable<TaskDependency> {
    return this.http.post<TaskDependency>(`${this.getUrl(projectId)}/dependencies`, req).pipe(
      tap(() => {
        this.loadSchedule(projectId).subscribe();
      })
    );
  }

  removeDependency(projectId: string, dependencyId: string): Observable<void> {
    return this.http.delete<void>(`${this.getUrl(projectId)}/dependencies/${dependencyId}`).pipe(
      tap(() => {
        this.loadSchedule(projectId).subscribe();
      })
    );
  }

  recalculateCpm(projectId: string): Observable<ProjectScheduleResponse> {
    this.loading.set(true);
    return this.http.post<ProjectScheduleResponse>(`${this.getUrl(projectId)}/recalculate-cpm`, {}).pipe(
      tap((data) => {
        this.schedule.set(data);
        this.loading.set(false);
      })
    );
  }

  createBaseline(projectId: string, req: CreateBaselineRequest): Observable<ScheduleBaseline> {
    return this.http.post<ScheduleBaseline>(`${this.getUrl(projectId)}/baselines`, req).pipe(
      tap(() => {
        this.loadSchedule(projectId).subscribe();
      })
    );
  }

  loadBaselineComparison(projectId: string, baselineId: string): Observable<ScheduleBaselineComparison[]> {
    return this.http
      .get<ScheduleBaselineComparison[]>(`${this.getUrl(projectId)}/baselines/${baselineId}/comparison`)
      .pipe(
        tap((comparisons) => {
          this.baselineComparison.set(comparisons);
        })
      );
  }

  assignResource(projectId: string, taskId: string, req: AssignResourceRequest): Observable<TaskResourceAssignment> {
    return this.http.post<TaskResourceAssignment>(`${this.getUrl(projectId)}/tasks/${taskId}/resources`, req).pipe(
      tap(() => {
        this.loadSchedule(projectId).subscribe();
      })
    );
  }

  removeResourceAssignment(projectId: string, assignmentId: string): Observable<void> {
    return this.http.delete<void>(`${this.getUrl(projectId)}/resources/${assignmentId}`).pipe(
      tap(() => {
        this.loadSchedule(projectId).subscribe();
      })
    );
  }

  loadOverAllocations(projectId: string): Observable<ResourceOverAllocation[]> {
    return this.http.get<ResourceOverAllocation[]>(`${this.getUrl(projectId)}/resources/over-allocations`).pipe(
      tap((data) => {
        this.overAllocations.set(data);
      })
    );
  }

  importFromWbs(projectId: string): Observable<ProjectScheduleResponse> {
    this.loading.set(true);
    return this.http.post<ProjectScheduleResponse>(`${this.getUrl(projectId)}/import-wbs`, {}).pipe(
      tap((data) => {
        this.schedule.set(data);
        this.loading.set(false);
      })
    );
  }

  syncFromDpr(projectId: string): Observable<ProjectScheduleResponse> {
    this.loading.set(true);
    return this.http.post<ProjectScheduleResponse>(`${this.getUrl(projectId)}/sync-dpr`, {}).pipe(
      tap((data) => {
        this.schedule.set(data);
        this.loading.set(false);
      })
    );
  }
}
