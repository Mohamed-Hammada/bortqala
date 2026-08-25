import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, firstValueFrom } from 'rxjs';
import { apiErrorMessage } from '../../core/api-error';
import { downloadBlob, timestampedExcelFileName } from '../../core/download';
import { I18nService } from '../../core/i18n.service';
import { NotificationService } from '../../core/notification.service';
import { TransitionResponse } from '../../core/api.models';
import {
  AttendanceDecision, BulkDecisionRequest, BulkDecisionResponse, DayAnomalyActionResponse,
  DayAnomalyDecisionRequest, DowntimeDecisionRequest, GeneratedPeriod, HolidayProposalStatus, PeriodOption,
  ReportDetails, ReportPayCycle, ReportPeriodSelection, ReportPreview, ReportSummary,
  AttendanceExceptionWorkbench, AttendanceExceptionBulkRequest, AttendanceExceptionBulkPreview,
  AttendanceExceptionBulkResult,
} from './reports.models';

@Injectable()
export class ReportsStore {
  private readonly http = inject(HttpClient);
  private readonly i18n = inject(I18nService);
  private readonly notification = inject(NotificationService);
  readonly reports = signal<ReportSummary[]>([]);
  readonly periods = signal<PeriodOption[]>([]);
  readonly generated = signal<GeneratedPeriod[]>([]);
  readonly generatedLoading = signal(false);
  readonly details = signal<ReportDetails | null>(null);
  readonly exceptionWorkbench = signal<AttendanceExceptionWorkbench | null>(null);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  private generatedRequestToken = 0;

  async list(year: number): Promise<void> {
    this.loading.set(true); this.error.set(null);
    this.generated.set([]); this.generatedLoading.set(true);
    // Generated-periods fetch is deliberately independent: the picker renders
    // immediately and chips flip to their finalized state when this resolves.
    // The token discards stale responses when the year changes mid-flight.
    const requestId = ++this.generatedRequestToken;
    firstValueFrom(this.http.get<GeneratedPeriod[]>('/api/v1/reports/generated-periods', { params: { year } }))
      .then((periods) => {
        if (requestId !== this.generatedRequestToken || !this.generatedLoading()) return;
        this.generated.set(periods ?? []);
      })
      .catch(() => { if (requestId === this.generatedRequestToken) this.generated.set([]); })
      .finally(() => { if (requestId === this.generatedRequestToken) this.generatedLoading.set(false); });
    try {
      const [reports, periods] = await Promise.all([
        firstValueFrom(this.http.get<ReportSummary[]>('/api/v1/reports')),
        firstValueFrom(this.http.get<PeriodOption[]>('/api/v1/reports/available-periods', { params: { year } })),
      ]);
      this.reports.set(reports); this.periods.set(periods);
    } catch (error) { this.error.set(apiErrorMessage(error, this.i18n)); }
    finally { this.loading.set(false); }
  }

  async create(period: ReportPeriodSelection): Promise<string | null> {
    this.loading.set(true); this.error.set(null);
    try {
      const result = await firstValueFrom(this.http.post<ReportDetails>('/api/v1/reports', {
        periodStart: period.periodStart, periodEnd: period.periodEnd, payCycle: period.payCycle,
      }));
      this.details.set(result); return result.report.id;
    } catch (error) { this.error.set(apiErrorMessage(error, this.i18n)); return null; }
    finally { this.loading.set(false); }
  }

  async preview(periodStart: string, periodEnd: string, payCycle: ReportPayCycle): Promise<ReportPreview | null> {
    this.error.set(null);
    try {
      return await firstValueFrom(this.http.get<ReportPreview>('/api/v1/reports/preview', { params: { periodStart, periodEnd, payCycle } }));
    } catch (error) { this.error.set(apiErrorMessage(error, this.i18n)); return null; }
  }

  async load(id: string): Promise<void> {
    this.loading.set(true); this.error.set(null);
    try {
      const [details, exceptions] = await Promise.all([
        firstValueFrom(this.http.get<ReportDetails>(`/api/v1/reports/${id}`)),
        firstValueFrom(this.http.get<AttendanceExceptionWorkbench>(`/api/v1/reports/${id}/attendance-exceptions`)),
      ]);
      this.details.set(details); this.exceptionWorkbench.set(exceptions);
    }
    catch (error) { this.error.set(apiErrorMessage(error, this.i18n)); }
    finally { this.loading.set(false); }
  }

  async decide(id: string, rowId: string, decision: AttendanceDecision, workedMinutes: number | null,
               note: string | null, expectedVersion: number): Promise<boolean> {
    const saved = await this.mutate(this.http.put<ReportDetails>(`/api/v1/reports/${id}/daily-results/${rowId}/decision`, {
      decision, workedMinutes, note, expectedVersion,
    }));
    if (!saved) {
      this.notification.error(this.error() ?? this.i18n.t('api.unexpected'));
      return false;
    }

    // Persistence verification: force a fresh GET and verify the same row.
    await this.load(id);
    const persisted = this.details()?.dailyResults.find(row => row.id === rowId);
    if (!persisted || persisted.decision !== decision) {
      this.error.set(this.i18n.t('review.decisionPersistenceFailed'));
      this.notification.error(this.error()!);
      return false;
    }
    this.notification.success(this.i18n.t('review.decisionSaved'));
    return true;
  }

  async decideHoliday(id: string, proposalId: string, status: HolidayProposalStatus, holidayName: string | null,
                      note: string | null): Promise<boolean> {
    return this.mutate(this.http.put<ReportDetails>(`/api/v1/reports/${id}/holiday-proposals/${proposalId}/decision`,
      { status, holidayName, note }));
  }

  async approve(id: string): Promise<boolean> {
    try { await firstValueFrom(this.http.post<TransitionResponse>(`/api/v1/reports/${id}/approve`, {})); await this.load(id); return true; }
    catch (error) { this.error.set(apiErrorMessage(error, this.i18n)); return false; }
  }
  async reopen(id: string): Promise<boolean> {
    try { await firstValueFrom(this.http.post<TransitionResponse>(`/api/v1/reports/${id}/reopen`, {})); await this.load(id); return true; }
    catch (error) { this.error.set(apiErrorMessage(error, this.i18n)); return false; }
  }
  async bulkDecision(reportId: string, request: BulkDecisionRequest): Promise<BulkDecisionResponse | null> {
    try { return await firstValueFrom(this.http.post<BulkDecisionResponse>(`/api/v1/reports/${reportId}/bulk-decision`, request)); }
    catch (error) { this.error.set(apiErrorMessage(error, this.i18n)); return null; }
  }
  async saveDowntimeDecision(reportId: string, request: DowntimeDecisionRequest): Promise<boolean> {
    return this.mutate(this.http.put<ReportDetails>(`/api/v1/reports/${reportId}/downtime-decision`, request));
  }
  async detectDayAnomalies(reportId: string): Promise<boolean> {
    return this.mutate(this.http.post<ReportDetails>(`/api/v1/reports/${reportId}/day-anomalies/detect`, {}));
  }
  async decideDayAnomaly(reportId: string, anomalyId: string,
                         request: DayAnomalyDecisionRequest): Promise<DayAnomalyActionResponse | null> {
    return this.anomalyAction(this.http.post<DayAnomalyActionResponse>(
      `/api/v1/reports/${reportId}/day-anomalies/${anomalyId}/decision`, request));
  }
  async reverseDayAnomaly(reportId: string, anomalyId: string): Promise<DayAnomalyActionResponse | null> {
    return this.anomalyAction(this.http.post<DayAnomalyActionResponse>(
      `/api/v1/reports/${reportId}/day-anomalies/${anomalyId}/reverse`, {}));
  }
  async reopenDayAnomaly(reportId: string, anomalyId: string): Promise<boolean> {
    return this.mutate(this.http.post<ReportDetails>(`/api/v1/reports/${reportId}/day-anomalies/${anomalyId}/reopen`, {}));
  }
  async detectAttendanceExceptions(reportId: string): Promise<boolean> {
    try { await firstValueFrom(this.http.post<number>(`/api/v1/reports/${reportId}/attendance-exceptions/detect`, {})); await this.load(reportId); return true; }
    catch (error) { this.error.set(apiErrorMessage(error, this.i18n)); return false; }
  }
  async previewAttendanceExceptions(reportId: string, request: AttendanceExceptionBulkRequest): Promise<AttendanceExceptionBulkPreview | null> {
    try { return await firstValueFrom(this.http.post<AttendanceExceptionBulkPreview>(`/api/v1/reports/${reportId}/attendance-exceptions/bulk-preview`, request)); }
    catch (error) { this.error.set(apiErrorMessage(error, this.i18n)); return null; }
  }
  async resolveAttendanceExceptions(reportId: string, request: AttendanceExceptionBulkRequest): Promise<AttendanceExceptionBulkResult | null> {
    try {
      const response = await firstValueFrom(this.http.post<AttendanceExceptionBulkResult>(`/api/v1/reports/${reportId}/attendance-exceptions/bulk-resolve`, request));
      this.exceptionWorkbench.set(response.workbench); await this.load(reportId); return response;
    } catch (error) { this.error.set(apiErrorMessage(error, this.i18n)); return null; }
  }
  async export(id: string): Promise<void> {
    try {
      downloadBlob(await firstValueFrom(this.http.get(`/api/v1/reports/${id}/export`, { responseType: 'blob' })),
        timestampedExcelFileName('تقرير-الحضور', 'attendance-report', this.i18n.locale()));
      await this.load(id);
    } catch (error) { this.error.set(apiErrorMessage(error, this.i18n)); }
  }

  private async mutate(request: Observable<ReportDetails>): Promise<boolean> {
    this.loading.set(true); this.error.set(null);
    try { this.details.set(await firstValueFrom(request)); return true; }
    catch (error) { this.error.set(apiErrorMessage(error, this.i18n)); return false; }
    finally { this.loading.set(false); }
  }

  private async anomalyAction(request: Observable<DayAnomalyActionResponse>): Promise<DayAnomalyActionResponse | null> {
    this.loading.set(true); this.error.set(null);
    try { const response = await firstValueFrom(request); this.details.set(response.details); return response; }
    catch (error) { this.error.set(apiErrorMessage(error, this.i18n)); return null; }
    finally { this.loading.set(false); }
  }
}
