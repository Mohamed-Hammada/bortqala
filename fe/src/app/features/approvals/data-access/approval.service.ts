import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, finalize, tap } from 'rxjs';
import {
  ApprovalWorkflowDefinition,
  ApprovalTask,
  ApprovalInstanceDetail,
  ApprovalDelegation,
  ApprovalDelegationPayload
} from '../models/approval.models';

@Injectable({ providedIn: 'root' })
export class ApprovalService {
  private http = inject(HttpClient);

  definitions = signal<ApprovalWorkflowDefinition[]>([]);
  myTasks = signal<ApprovalTask[]>([]);
  delegations = signal<ApprovalDelegation[]>([]);
  loading = signal<boolean>(false);

  loadWorkflowDefinitions(): Observable<ApprovalWorkflowDefinition[]> {
    this.loading.set(true);
    return this.http.get<ApprovalWorkflowDefinition[]>('/api/v1/approval-workflows').pipe(
      tap(res => this.definitions.set(res)),
      finalize(() => this.loading.set(false))
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
      tap(res => this.myTasks.set(res)),
      finalize(() => this.loading.set(false))
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

  loadDelegations(): Observable<ApprovalDelegation[]> {
    return this.http.get<ApprovalDelegation[]>('/api/v1/approvals/delegations').pipe(
      tap(res => this.delegations.set(res))
    );
  }

  createDelegation(payload: ApprovalDelegationPayload): Observable<ApprovalDelegation> {
    return this.http.post<ApprovalDelegation>('/api/v1/approvals/delegations', payload).pipe(
      tap(saved => this.delegations.update(items => [saved, ...items]))
    );
  }

  deactivateDelegation(id: string): Observable<void> {
    return this.http.delete<void>(`/api/v1/approvals/delegations/${id}`).pipe(
      tap(() => this.delegations.update(items => items.map(item => item.id === id ? { ...item, active: false } : item)))
    );
  }
}
