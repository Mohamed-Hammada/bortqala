import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  CreateCyclePayload,
  CreateKpiPayload,
  InitAppraisalPayload,
  PerformanceAppraisal,
  PerformanceCycle,
  PerformanceKpi,
  SubmitAppraisalPayload,
} from './performance.models';

@Injectable({
  providedIn: 'root',
})
export class PerformanceService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/performance';

  listCycles(): Observable<PerformanceCycle[]> {
    return this.http.get<PerformanceCycle[]>(`${this.baseUrl}/cycles`);
  }

  createCycle(payload: CreateCyclePayload): Observable<PerformanceCycle> {
    return this.http.post<PerformanceCycle>(`${this.baseUrl}/cycles`, payload);
  }

  lockCycle(id: string): Observable<PerformanceCycle> {
    return this.http.post<PerformanceCycle>(`${this.baseUrl}/cycles/${id}/lock`, {});
  }

  listKpis(cycleId?: string): Observable<PerformanceKpi[]> {
    let params = new HttpParams();
    if (cycleId) {
      params = params.set('cycleId', cycleId);
    }
    return this.http.get<PerformanceKpi[]>(`${this.baseUrl}/kpis`, { params });
  }

  createKpi(payload: CreateKpiPayload): Observable<PerformanceKpi> {
    return this.http.post<PerformanceKpi>(`${this.baseUrl}/kpis`, payload);
  }

  listAppraisals(cycleId?: string, employeeId?: string): Observable<PerformanceAppraisal[]> {
    let params = new HttpParams();
    if (cycleId) params = params.set('cycleId', cycleId);
    if (employeeId) params = params.set('employeeId', employeeId);
    return this.http.get<PerformanceAppraisal[]>(`${this.baseUrl}/appraisals`, { params });
  }

  initAppraisal(payload: InitAppraisalPayload): Observable<PerformanceAppraisal> {
    return this.http.post<PerformanceAppraisal>(`${this.baseUrl}/appraisals/init`, payload);
  }

  submitAppraisal(id: string, payload: SubmitAppraisalPayload): Observable<PerformanceAppraisal> {
    return this.http.post<PerformanceAppraisal>(`${this.baseUrl}/appraisals/${id}/submit`, payload);
  }

  finalizeAppraisal(id: string): Observable<PerformanceAppraisal> {
    return this.http.post<PerformanceAppraisal>(`${this.baseUrl}/appraisals/${id}/finalize`, {});
  }

  listEmployees(): Observable<Array<{ id: string; fullName: string; employeeCode: string }>> {
    return this.http.get<Array<{ id: string; fullName: string; employeeCode: string }>>('/api/v1/employees');
  }
}
