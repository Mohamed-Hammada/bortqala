import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, firstValueFrom } from 'rxjs';
import { apiErrorMessage } from '../../core/api-error';
import { downloadBlob, timestampedExcelFileName } from '../../core/download';
import { I18nService } from '../../core/i18n.service';
import {
  AttendanceDecision,
  HolidayProposalStatus,
  PeriodOption,
  ReportDetails,
  ReportPeriodSelection,
  ReportSummary,
} from './reports.models';

@Injectable()
export class ReportsStore {
  private readonly http = inject(HttpClient);
  private readonly i18n = inject(I18nService);
  readonly reports = signal<ReportSummary[]>([]);
  readonly periods = signal<PeriodOption[]>([]);
  readonly details = signal<ReportDetails | null>(null);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  async list(year: number): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      const [reports, periods] = await Promise.all([
        firstValueFrom(this.http.get<ReportSummary[]>('/api/v1/reports')),
        firstValueFrom(
          this.http.get<PeriodOption[]>('/api/v1/reports/available-periods', { params: { year } }),
        ),
      ]);
      this.reports.set(reports);
      this.periods.set(periods);
    } catch (error) {
      this.error.set(apiErrorMessage(error, this.i18n));
    } finally {
      this.loading.set(false);
    }
  }

  async create(period: ReportPeriodSelection): Promise<string | null> {
    this.loading.set(true);
    this.error.set(null);
    try {
      const result = await firstValueFrom(
        this.http.post<ReportDetails>('/api/v1/reports', {
          periodStart: period.periodStart,
          periodEnd: period.periodEnd,
          payCycle: period.payCycle,
        }),
      );
      this.details.set(result);
      return result.report.id;
    } catch (error) {
      this.error.set(apiErrorMessage(error, this.i18n));
      return null;
    } finally {
      this.loading.set(false);
    }
  }

  async load(id: string): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      this.details.set(await firstValueFrom(this.http.get<ReportDetails>(`/api/v1/reports/${id}`)));
    } catch (error) {
      this.error.set(apiErrorMessage(error, this.i18n));
    } finally {
      this.loading.set(false);
    }
  }

  async decide(
    id: string,
    rowId: string,
    decision: AttendanceDecision,
    workedMinutes: number | null,
    note: string | null,
  ): Promise<boolean> {
    return this.mutate(
      this.http.put<ReportDetails>(`/api/v1/reports/${id}/daily-results/${rowId}/decision`, {
        decision,
        workedMinutes,
        note,
      }),
    );
  }

  async decideHoliday(
    id: string,
    proposalId: string,
    status: HolidayProposalStatus,
    holidayName: string | null,
    note: string | null,
  ): Promise<boolean> {
    return this.mutate(
      this.http.put<ReportDetails>(
        `/api/v1/reports/${id}/holiday-proposals/${proposalId}/decision`,
        { status, holidayName, note },
      ),
    );
  }

  async approve(id: string): Promise<boolean> {
    return this.mutate(this.http.post<ReportDetails>(`/api/v1/reports/${id}/approve`, {}));
  }

  async reopen(id: string): Promise<boolean> {
    return this.mutate(this.http.post<ReportDetails>(`/api/v1/reports/${id}/reopen`, {}));
  }

  async export(id: string): Promise<void> {
    try {
      downloadBlob(
        await firstValueFrom(
          this.http.get(`/api/v1/reports/${id}/export`, { responseType: 'blob' }),
        ),
        timestampedExcelFileName('تقرير-الحضور', 'attendance-report', this.i18n.locale()),
      );
      await this.load(id);
    } catch (error) {
      this.error.set(apiErrorMessage(error, this.i18n));
    }
  }

  private async mutate(request: Observable<ReportDetails>): Promise<boolean> {
    this.loading.set(true);
    this.error.set(null);
    try {
      this.details.set(await firstValueFrom(request));
      return true;
    } catch (error) {
      this.error.set(apiErrorMessage(error, this.i18n));
      return false;
    } finally {
      this.loading.set(false);
    }
  }
}
