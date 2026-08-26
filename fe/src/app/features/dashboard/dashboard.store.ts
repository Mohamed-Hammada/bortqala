import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { apiErrorMessage } from '../../core/api-error';
import { Dashboard, AttendanceChartPoint, PayrollSummary, DepartmentMetric, TrendPoint, ClockInBucket } from './dashboard.models';
import { I18nService } from '../../core/i18n.service';

@Injectable()
export class DashboardStore {
  private readonly httpClient = inject(HttpClient);
  private readonly i18n = inject(I18nService);
  readonly data = signal<Dashboard | null>(null);
  readonly chartData = signal<AttendanceChartPoint[]>([]);
  readonly payrollSummary = signal<PayrollSummary | null>(null);
  readonly departmentMetrics = signal<DepartmentMetric[]>([]);
  readonly trends = signal<TrendPoint[]>([]);
  readonly trendsLoading = signal(false);
  readonly clockInBuckets = signal<ClockInBucket[]>([]);
  readonly clockInLoading = signal(false);
  readonly loading = signal(true);
  readonly chartLoading = signal(false);
  readonly error = signal<string | null>(null);
  private currentRequestId = 0;

  async load(year: number, month: number): Promise<void> {
    const reqId = ++this.currentRequestId;
    this.loading.set(true);
    this.error.set(null);
    try {
      const res = await firstValueFrom(
        this.httpClient.get<Dashboard>('/api/v1/dashboard', { params: { year, month } }),
      );
      if (reqId === this.currentRequestId) {
        this.data.set(res);
      }
    } catch (error) {
      if (reqId === this.currentRequestId) {
        this.error.set(apiErrorMessage(error, this.i18n));
      }
    } finally {
      if (reqId === this.currentRequestId) {
        this.loading.set(false);
      }
    }
  }

  async loadChartData(period: string, departmentId: string | null, year: number, month: number): Promise<void> {
    this.chartLoading.set(true);
    try {
      const params: Record<string, string | number> = { period, year, month };
      if (departmentId) params['departmentId'] = departmentId;
      const res = await firstValueFrom(
        this.httpClient.get<AttendanceChartPoint[]>('/api/v1/dashboard/attendance-chart', { params }),
      );
      this.chartData.set(res);
    } catch {
      this.chartData.set([]);
    } finally {
      this.chartLoading.set(false);
    }
  }

  async loadPayrollSummary(year: number, month: number): Promise<void> {
    try {
      const res = await firstValueFrom(
        this.httpClient.get<PayrollSummary>('/api/v1/dashboard/payroll-summary', { params: { year, month } }),
      );
      this.payrollSummary.set(res);
    } catch {
      this.payrollSummary.set(null);
    }
  }

  async loadDepartmentMetrics(year: number, month: number): Promise<void> {
    try {
      const res = await firstValueFrom(
        this.httpClient.get<DepartmentMetric[]>('/api/v1/dashboard/department-metrics', { params: { year, month } }),
      );
      this.departmentMetrics.set(res);
    } catch {
      this.departmentMetrics.set([]);
    }
  }

  async loadTrends(months: number, year: number, month: number): Promise<void> {
    this.trendsLoading.set(true);
    try {
      const res = await firstValueFrom(
        this.httpClient.get<TrendPoint[]>('/api/v1/dashboard/trends', { params: { months, year, month } }),
      );
      this.trends.set(res);
    } catch {
      this.trends.set([]);
    } finally {
      this.trendsLoading.set(false);
    }
  }

  /** WP-08: peak clock-in histogram from the dashboard API. */
  async loadClockInHistogram(months: number, categoryId?: string): Promise<void> {
    this.clockInLoading.set(true);
    try {
      const params: Record<string, string> = { months: String(months) };
      if (categoryId) params['categoryId'] = categoryId;
      const res = await firstValueFrom(
        this.httpClient.get<ClockInBucket[]>('/api/v1/dashboard/clock-in-histogram', { params }),
      );
      this.clockInBuckets.set(res ?? []);
    } catch {
      this.clockInBuckets.set([]);
    } finally {
      this.clockInLoading.set(false);
    }
  }

  downloadTrends(months: number, year: number, month: number): Promise<Blob> {
    return firstValueFrom(
      this.httpClient.get('/api/v1/exports/trends.xlsx', { params: { months, year, month }, responseType: 'blob' }),
    );
  }

  /** WP-08: Excel export of the peak clock-in histogram (honors the category filter). */
  downloadClockInHistogram(months: number, categoryId?: string): Promise<Blob> {
    const params: Record<string, string | number> = { months };
    if (categoryId) params['categoryId'] = categoryId;
    return firstValueFrom(
      this.httpClient.get('/api/v1/exports/clock-in-histogram.xlsx', { params, responseType: 'blob' }),
    );
  }

  async loadAll(year: number, month: number, period = 'MONTH', departmentId: string | null = null, trendMonths = 6): Promise<void> {
    await Promise.all([
      this.load(year, month),
      this.loadChartData(period, departmentId, year, month),
      this.loadPayrollSummary(year, month),
      this.loadDepartmentMetrics(year, month),
      this.loadTrends(trendMonths, year, month),
    ]);
  }
}

// BORTQALA_ATTENDANCE_PIPELINE_20260816_V1_TREND_FE_STORE
