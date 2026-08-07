import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { BudgetPayload, BudgetResponse, BudgetStatusResponse, Department, EncumbranceResponse } from './budget.models';

@Injectable({ providedIn: 'root' })
export class BudgetService {
  private readonly http = inject(HttpClient);

  listBudgets(): Promise<BudgetResponse[]> {
    return firstValueFrom(this.http.get<BudgetResponse[]>('/api/v1/budget/budgets'));
  }

  createBudget(payload: BudgetPayload): Promise<BudgetResponse> {
    return firstValueFrom(this.http.post<BudgetResponse>('/api/v1/budget/budgets', payload));
  }

  updateBudget(id: string, payload: BudgetPayload): Promise<BudgetResponse> {
    return firstValueFrom(this.http.put<BudgetResponse>(`/api/v1/budget/budgets/${id}`, payload));
  }

  deleteBudget(id: string): Promise<void> {
    return firstValueFrom(this.http.delete<void>(`/api/v1/budget/budgets/${id}`));
  }

  budgetStatus(year?: number): Promise<BudgetStatusResponse[]> {
    const params = year ? `?year=${year}` : '';
    return firstValueFrom(this.http.get<BudgetStatusResponse[]>(`/api/v1/budget/status${params}`));
  }

  listEncumbrances(): Promise<EncumbranceResponse[]> {
    return firstValueFrom(this.http.get<EncumbranceResponse[]>('/api/v1/budget/encumbrances'));
  }

  listDepartments(): Promise<Department[]> {
    return firstValueFrom(this.http.get<Department[]>('/api/v1/organization/departments'));
  }
}
