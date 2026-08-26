import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import {
  CreateClaimRequest,
  DecisionRequest,
  ExpenseClaimResponse,
  ReimburseRequest,
  UpdateClaimRequest,
} from './expense.models';

@Injectable({ providedIn: 'root' })
export class ExpenseService {
  private readonly http = inject(HttpClient);

  listMine(): Promise<ExpenseClaimResponse[]> {
    return firstValueFrom(this.http.get<ExpenseClaimResponse[]>('/api/v1/expenses'));
  }

  listPending(): Promise<ExpenseClaimResponse[]> {
    return firstValueFrom(this.http.get<ExpenseClaimResponse[]>('/api/v1/expenses/pending'));
  }

  getById(id: string): Promise<ExpenseClaimResponse> {
    return firstValueFrom(this.http.get<ExpenseClaimResponse>(`/api/v1/expenses/${id}`));
  }

  create(payload: CreateClaimRequest): Promise<ExpenseClaimResponse> {
    return firstValueFrom(this.http.post<ExpenseClaimResponse>('/api/v1/expenses', payload));
  }

  update(id: string, payload: UpdateClaimRequest): Promise<ExpenseClaimResponse> {
    return firstValueFrom(this.http.put<ExpenseClaimResponse>(`/api/v1/expenses/${id}`, payload));
  }

  submit(id: string): Promise<ExpenseClaimResponse> {
    return firstValueFrom(this.http.post<ExpenseClaimResponse>(`/api/v1/expenses/${id}/submit`, {}));
  }

  approve(id: string, note?: string): Promise<ExpenseClaimResponse> {
    return firstValueFrom(
      this.http.post<ExpenseClaimResponse>(`/api/v1/expenses/${id}/approve`, { note } as DecisionRequest),
    );
  }

  reject(id: string, note?: string): Promise<ExpenseClaimResponse> {
    return firstValueFrom(
      this.http.post<ExpenseClaimResponse>(`/api/v1/expenses/${id}/reject`, { note } as DecisionRequest),
    );
  }

  reimburse(id: string, reference: string): Promise<ExpenseClaimResponse> {
    return firstValueFrom(
      this.http.post<ExpenseClaimResponse>(`/api/v1/expenses/${id}/reimburse`, { reference } as ReimburseRequest),
    );
  }
}
