import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import {
  Contractor,
  WorkerCategory,
  Worker,
  LaborRequest,
  SettlementPeriod,
  SettlementCalculationSummary,
  WorkforceAdvance,
  AttendanceCell
} from '../models/workforce.models';

@Injectable({ providedIn: 'root' })
export class WorkforceService {
  private http = inject(HttpClient);

  contractors = signal<Contractor[]>([]);
  categories = signal<WorkerCategory[]>([]);
  workers = signal<Worker[]>([]);
  laborRequests = signal<LaborRequest[]>([]);
  settlementPeriods = signal<SettlementPeriod[]>([]);
  advances = signal<WorkforceAdvance[]>([]);
  loading = signal<boolean>(false);

  // Contractors
  loadContractors(): Observable<Contractor[]> {
    this.loading.set(true);
    return this.http.get<Contractor[]>('/api/v1/workforce/contractors').pipe(
      tap(res => {
        this.contractors.set(res);
        this.loading.set(false);
      })
    );
  }

  createContractor(payload: Partial<Contractor>): Observable<Contractor> {
    return this.http.post<Contractor>('/api/v1/workforce/contractors', payload).pipe(
      tap(() => this.loadContractors().subscribe())
    );
  }

  updateContractor(id: string, payload: Partial<Contractor>): Observable<Contractor> {
    return this.http.put<Contractor>(`/api/v1/workforce/contractors/${id}`, payload).pipe(
      tap(() => this.loadContractors().subscribe())
    );
  }

  // Worker Categories
  loadCategories(): Observable<WorkerCategory[]> {
    return this.http.get<WorkerCategory[]>('/api/v1/workforce/categories').pipe(
      tap(res => this.categories.set(res))
    );
  }

  createCategory(payload: Partial<WorkerCategory>): Observable<WorkerCategory> {
    return this.http.post<WorkerCategory>('/api/v1/workforce/categories', payload).pipe(
      tap(() => this.loadCategories().subscribe())
    );
  }

  // Workers
  loadWorkers(): Observable<Worker[]> {
    return this.http.get<Worker[]>('/api/v1/workforce/workers').pipe(
      tap(res => this.workers.set(res))
    );
  }

  createWorker(payload: Partial<Worker>): Observable<Worker> {
    return this.http.post<Worker>('/api/v1/workforce/workers', payload).pipe(
      tap(() => this.loadWorkers().subscribe())
    );
  }

  updateWorker(id: string, payload: Partial<Worker>): Observable<Worker> {
    return this.http.put<Worker>(`/api/v1/workforce/workers/${id}`, payload).pipe(
      tap(() => this.loadWorkers().subscribe())
    );
  }

  // Labor Requests
  loadLaborRequests(): Observable<LaborRequest[]> {
    return this.http.get<LaborRequest[]>('/api/v1/workforce/labor-requests').pipe(
      tap(res => this.laborRequests.set(res))
    );
  }

  createLaborRequest(payload: any): Observable<LaborRequest> {
    return this.http.post<LaborRequest>('/api/v1/workforce/labor-requests', payload).pipe(
      tap(() => this.loadLaborRequests().subscribe())
    );
  }

  // Attendance Matrix
  saveAttendanceBatch(entries: AttendanceCell[]): Observable<any> {
    return this.http.post('/api/v1/workforce/attendance/batch', { entries });
  }

  // Settlement Periods
  loadSettlementPeriods(): Observable<SettlementPeriod[]> {
    return this.http.get<SettlementPeriod[]>('/api/v1/workforce/settlements/periods').pipe(
      tap(res => this.settlementPeriods.set(res))
    );
  }

  createSettlementPeriod(payload: any): Observable<SettlementPeriod> {
    return this.http.post<SettlementPeriod>('/api/v1/workforce/settlements/periods', payload).pipe(
      tap(() => this.loadSettlementPeriods().subscribe())
    );
  }

  calculatePeriod(id: string): Observable<SettlementCalculationSummary> {
    return this.http.post<SettlementCalculationSummary>(`/api/v1/workforce/settlements/periods/${id}/calculate`, {});
  }

  approvePeriod(id: string): Observable<SettlementPeriod> {
    return this.http.post<SettlementPeriod>(`/api/v1/workforce/settlements/periods/${id}/approve`, {}).pipe(
      tap(() => this.loadSettlementPeriods().subscribe())
    );
  }

  exportSettlementPeriodExcel(id: string): Observable<Blob> {
    return this.http.get(`/api/v1/workforce/settlements/periods/${id}/export-excel`, {
      responseType: 'blob'
    });
  }

  // Advances
  loadAdvances(): Observable<WorkforceAdvance[]> {
    return this.http.get<WorkforceAdvance[]>('/api/v1/workforce/advances').pipe(
      tap(res => this.advances.set(res))
    );
  }

  createAdvance(payload: any): Observable<WorkforceAdvance> {
    return this.http.post<WorkforceAdvance>('/api/v1/workforce/advances', payload).pipe(
      tap(() => this.loadAdvances().subscribe())
    );
  }

  pauseAdvance(id: string): Observable<WorkforceAdvance> {
    return this.http.post<WorkforceAdvance>(`/api/v1/workforce/advances/${id}/pause`, {});
  }

  resumeAdvance(id: string): Observable<WorkforceAdvance> {
    return this.http.post<WorkforceAdvance>(`/api/v1/workforce/advances/${id}/resume`, {});
  }

  repayAdvance(id: string, payload: any): Observable<WorkforceAdvance> {
    return this.http.post<WorkforceAdvance>(`/api/v1/workforce/advances/${id}/repay`, payload);
  }

  // Import Analysis
  analyzeImport(summaryDays?: number, settlementDays?: number): Observable<any> {
    return this.http.post('/api/v1/workforce/import/analyze', null, {
      params: { summaryDays: summaryDays || 1550, settlementDays: settlementDays || 1635 }
    });
  }
}
