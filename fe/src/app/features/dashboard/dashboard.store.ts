import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { apiErrorMessage } from '../../core/api-error';
import { Dashboard, AttendanceChartPoint, PayrollSummary, DepartmentMetric } from './dashboard.models';
import { I18nService } from '../../core/i18n.service';

@Injectable()
export class DashboardStore {
  private readonly httpClient = inject(HttpClient);
  private readonly i18n = inject(I18nService);
  readonly data = signal<Dashboard | null>(null);
  readonly chartData = signal<AttendanceChartPoint[]>([]);
  readonly payrollSummary = signal<PayrollSummary | null>(null);
  readonly departmentMetrics = signal<DepartmentMetric[]>([]);
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

  async loadAll(year: number, month: number, period = 'MONTH', departmentId: string | null = null): Promise<void> {
    await Promise.all([
      this.load(year, month),
      this.loadChartData(period, departmentId, year, month),
      this.loadPayrollSummary(year, month),
      this.loadDepartmentMetrics(year, month),
    ]);
  }
}
