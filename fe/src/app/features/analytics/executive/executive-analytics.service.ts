import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ComparativeTrends,
  CreateSnapshotPayload,
  ExecutiveKpiSnapshot,
  ExecutiveOverview,
  KpiCategory,
  KpiDefinition,
  OwnerCockpitResponse,
  CockpitTargetResponse,
  SaveCockpitTargetRequest,
} from './executive-analytics.models';

@Injectable({
  providedIn: 'root',
})
export class ExecutiveAnalyticsService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/analytics/executive';

  getKpiRegistry(): Observable<KpiDefinition[]> {
    return this.http.get<KpiDefinition[]>(`${this.baseUrl}/kpi-registry`);
  }

  getOverview(
    period?: string,
    companyId?: string,
    branchId?: string,
    projectId?: string,
  ): Observable<ExecutiveOverview> {
    let params = new HttpParams();
    if (period) params = params.set('period', period);
    if (companyId) params = params.set('companyId', companyId);
    if (branchId) params = params.set('branchId', branchId);
    if (projectId) params = params.set('projectId', projectId);
    return this.http.get<ExecutiveOverview>(`${this.baseUrl}/overview`, { params });
  }

  getTrends(months = 6, category?: KpiCategory): Observable<ComparativeTrends> {
    let params = new HttpParams().set('months', months.toString());
    if (category) params = params.set('category', category);
    return this.http.get<ComparativeTrends>(`${this.baseUrl}/trends`, { params });
  }

  getSnapshots(periodKey?: string): Observable<ExecutiveKpiSnapshot[]> {
    let params = new HttpParams();
    if (periodKey) params = params.set('periodKey', periodKey);
    return this.http.get<ExecutiveKpiSnapshot[]>(`${this.baseUrl}/snapshots`, { params });
  }

  recordSnapshot(payload: CreateSnapshotPayload): Observable<ExecutiveKpiSnapshot> {
    return this.http.post<ExecutiveKpiSnapshot>(`${this.baseUrl}/snapshots`, payload);
  }

  getCockpit(
    period?: string,
    companyId?: string,
    branchId?: string,
  ): Observable<OwnerCockpitResponse> {
    let params = new HttpParams();
    if (period) params = params.set('period', period);
    if (companyId) params = params.set('companyId', companyId);
    if (branchId) params = params.set('branchId', branchId);
    return this.http.get<OwnerCockpitResponse>(`${this.baseUrl}/cockpit`, { params });
  }

  exportCockpitExcel(
    period?: string,
    companyId?: string,
    branchId?: string,
  ): Observable<Blob> {
    let params = new HttpParams();
    if (period) params = params.set('period', period);
    if (companyId) params = params.set('companyId', companyId);
    if (branchId) params = params.set('branchId', branchId);
    return this.http.get(`${this.baseUrl}/cockpit/export.xlsx`, {
      params,
      responseType: 'blob',
    });
  }

  getTargets(periodKey?: string): Observable<CockpitTargetResponse> {
    let params = new HttpParams();
    if (periodKey) params = params.set('periodKey', periodKey);
    return this.http.get<CockpitTargetResponse>(`${this.baseUrl}/targets`, { params });
  }

  saveTargets(payload: SaveCockpitTargetRequest): Observable<CockpitTargetResponse> {
    return this.http.post<CockpitTargetResponse>(`${this.baseUrl}/targets`, payload);
  }
}

