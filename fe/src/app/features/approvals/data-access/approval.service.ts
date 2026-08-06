import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import {
  ApprovalWorkflowDefinition,
  ApprovalTask,
  ApprovalInstanceDetail
} from '../models/approval.models';

@Injectable({ providedIn: 'root' })
export class ApprovalService {
  private http = inject(HttpClient);

  definitions = signal<ApprovalWorkflowDefinition[]>([]);
  myTasks = signal<ApprovalTask[]>([]);
  loading = signal<boolean>(false);

  loadWorkflowDefinitions(): Observable<ApprovalWorkflowDefinition[]> {
    this.loading.set(true);
    return this.http.get<ApprovalWorkflowDefinition[]>('/api/v1/approval-workflows').pipe(
      tap(res => {
        this.definitions.set(res);
        this.loading.set(false);
      })
    );
  }

  createWorkflowDefinition(payload: any): Observable<ApprovalWorkflowDefinition> {
    return this.http.post<ApprovalWorkflowDefinition>('/api/v1/approval-workflows', payload).pipe(
      tap(() => this.loadWorkflowDefinitions().subscribe())
    );
  }

  updateWorkflowDefinition(id: string, payload: any): Observable<ApprovalWorkflowDefinition> {
    return this.http.put<ApprovalWorkflowDefinition>(`/api/v1/approval-workflows/${id}`, payload).pipe(
      tap(() => this.loadWorkflowDefinitions().subscribe())
    );
  }

  loadMyTasks(): Observable<ApprovalTask[]> {
    this.loading.set(true);
    return this.http.get<ApprovalTask[]>('/api/v1/approvals/my-tasks').pipe(
      tap(res => {
        this.myTasks.set(res);
        this.loading.set(false);
      })
    );
  }

  approveStep(instanceId: string, comment?: string): Observable<ApprovalInstanceDetail> {
    return this.http.post<ApprovalInstanceDetail>('/api/v1/approvals/approve', { instanceId, comment }).pipe(
      tap(() => this.loadMyTasks().subscribe())
    );
  }

  rejectStep(instanceId: string, comment: string): Observable<ApprovalInstanceDetail> {
    return this.http.post<ApprovalInstanceDetail>('/api/v1/approvals/reject', { instanceId, comment }).pipe(
      tap(() => this.loadMyTasks().subscribe())
    );
  }

  getApprovalHistory(documentType: string, documentId: string): Observable<ApprovalInstanceDetail> {
    return this.http.get<ApprovalInstanceDetail>(`/api/v1/approvals/history/${documentType}/${documentId}`);
  }
}
