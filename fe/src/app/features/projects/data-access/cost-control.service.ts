import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  CostControlSummary,
  ProjectBudgetVersion,
  ProjectCostLedgerEntry,
  ProjectForecastEac,
  CreateBudgetVersionRequest,
  UpdateForecastEacRequest,
  RecordCostLedgerEntryRequest
} from '../models/cost-control.models';

@Injectable({
  providedIn: 'root'
})
export class CostControlService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/projects';

  getSummary(projectId: string): Observable<CostControlSummary> {
    return this.http.get<CostControlSummary>(`${this.baseUrl}/${projectId}/cost-control/summary`);
  }

  listBudgetVersions(projectId: string): Observable<ProjectBudgetVersion[]> {
    return this.http.get<ProjectBudgetVersion[]>(`${this.baseUrl}/${projectId}/cost-control/budget-versions`);
  }

  getBudgetVersion(projectId: string, versionId: string): Observable<ProjectBudgetVersion> {
    return this.http.get<ProjectBudgetVersion>(`${this.baseUrl}/${projectId}/cost-control/budget-versions/${versionId}`);
  }

  createBudgetVersion(projectId: string, req: CreateBudgetVersionRequest): Observable<ProjectBudgetVersion> {
    return this.http.post<ProjectBudgetVersion>(`${this.baseUrl}/${projectId}/cost-control/budget-versions`, req);
  }

  approveBudgetVersion(projectId: string, versionId: string): Observable<ProjectBudgetVersion> {
    return this.http.post<ProjectBudgetVersion>(`${this.baseUrl}/${projectId}/cost-control/budget-versions/${versionId}/approve`, {});
  }

  listCostLedgerEntries(projectId: string): Observable<ProjectCostLedgerEntry[]> {
    return this.http.get<ProjectCostLedgerEntry[]>(`${this.baseUrl}/${projectId}/cost-control/ledger`);
  }

  recordCostLedgerEntry(projectId: string, req: RecordCostLedgerEntryRequest): Observable<ProjectCostLedgerEntry> {
    return this.http.post<ProjectCostLedgerEntry>(`${this.baseUrl}/${projectId}/cost-control/ledger`, req);
  }

  listForecastEac(projectId: string): Observable<ProjectForecastEac[]> {
    return this.http.get<ProjectForecastEac[]>(`${this.baseUrl}/${projectId}/cost-control/forecast-eac`);
  }

  updateForecastEac(projectId: string, req: UpdateForecastEacRequest): Observable<ProjectForecastEac> {
    return this.http.put<ProjectForecastEac>(`${this.baseUrl}/${projectId}/cost-control/forecast-eac`, req);
  }
}
