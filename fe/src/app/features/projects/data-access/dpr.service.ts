import { Injectable, inject, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import {
  DailyReportResponse,
  CreateDailyReportRequest,
  UpdateDailyReportRequest,
  DprPeriodSummaryResponse,
} from '../models/dpr.models';

@Injectable({
  providedIn: 'root',
})
export class DprService {
  private readonly http = inject(HttpClient);

  readonly reports = signal<DailyReportResponse[]>([]);
  readonly currentReport = signal<DailyReportResponse | null>(null);
  readonly periodSummary = signal<DprPeriodSummaryResponse | null>(null);
  readonly loading = signal<boolean>(false);

  private getUrl(projectId: string): string {
    return `/api/v1/projects/${projectId}/daily-reports`;
  }

  loadReports(projectId: string): Observable<DailyReportResponse[]> {
    this.loading.set(true);
    return this.http.get<DailyReportResponse[]>(this.getUrl(projectId)).pipe(
      tap((data) => {
        this.reports.set(data);
        this.loading.set(false);
      })
    );
  }

  getReport(projectId: string, reportId: string): Observable<DailyReportResponse> {
    this.loading.set(true);
    return this.http.get<DailyReportResponse>(`${this.getUrl(projectId)}/${reportId}`).pipe(
      tap((data) => {
        this.currentReport.set(data);
        this.loading.set(false);
      })
    );
  }

  createReport(projectId: string, req: CreateDailyReportRequest): Observable<DailyReportResponse> {
    return this.http.post<DailyReportResponse>(this.getUrl(projectId), req).pipe(
      tap((created) => {
        this.reports.update((list) => [created, ...list]);
      })
    );
  }

  updateReport(projectId: string, reportId: string, req: UpdateDailyReportRequest): Observable<DailyReportResponse> {
    return this.http.put<DailyReportResponse>(`${this.getUrl(projectId)}/${reportId}`, req).pipe(
      tap((updated) => {
        this.currentReport.set(updated);
        this.reports.update((list) => list.map((r) => (r.id === reportId ? updated : r)));
      })
    );
  }

  deleteReport(projectId: string, reportId: string): Observable<void> {
    return this.http.delete<void>(`${this.getUrl(projectId)}/${reportId}`).pipe(
      tap(() => {
        this.reports.update((list) => list.filter((r) => r.id !== reportId));
      })
    );
  }

  submitReport(projectId: string, reportId: string): Observable<DailyReportResponse> {
    return this.http.post<DailyReportResponse>(`${this.getUrl(projectId)}/${reportId}/submit`, {}).pipe(
      tap((updated) => {
        this.currentReport.set(updated);
        this.reports.update((list) => list.map((r) => (r.id === reportId ? updated : r)));
      })
    );
  }

  approveReport(projectId: string, reportId: string): Observable<DailyReportResponse> {
    return this.http.post<DailyReportResponse>(`${this.getUrl(projectId)}/${reportId}/approve`, {}).pipe(
      tap((updated) => {
        this.currentReport.set(updated);
        this.reports.update((list) => list.map((r) => (r.id === reportId ? updated : r)));
      })
    );
  }

  reopenReport(projectId: string, reportId: string, reason?: string): Observable<DailyReportResponse> {
    return this.http.post<DailyReportResponse>(`${this.getUrl(projectId)}/${reportId}/reopen`, { reason }).pipe(
      tap((updated) => {
        this.currentReport.set(updated);
        this.reports.update((list) => list.map((r) => (r.id === reportId ? updated : r)));
      })
    );
  }

  copyPreviousDay(projectId: string, targetDateEpoch: number): Observable<DailyReportResponse> {
    const params = new HttpParams().set('targetDate', targetDateEpoch.toString());
    return this.http.post<DailyReportResponse>(`${this.getUrl(projectId)}/copy-previous`, {}, { params }).pipe(
      tap((created) => {
        this.reports.update((list) => [created, ...list]);
      })
    );
  }

  loadPeriodSummary(projectId: string, startDateEpoch: number, endDateEpoch: number): Observable<DprPeriodSummaryResponse> {
    const params = new HttpParams()
      .set('startDate', startDateEpoch.toString())
      .set('endDate', endDateEpoch.toString());
    return this.http.get<DprPeriodSummaryResponse>(`${this.getUrl(projectId)}/summary`, { params }).pipe(
      tap((summary) => this.periodSummary.set(summary))
    );
  }
}
