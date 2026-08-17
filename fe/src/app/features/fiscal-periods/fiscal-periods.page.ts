import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { I18nService } from '../../core/i18n.service';
import { NotificationService } from '../../core/notification.service';
import { apiErrorMessage } from '../../core/api-error';
import { formatDate } from '../../core/date';
import { AuthService } from '../../core/auth/auth.service';
import {
  BalanceSheetReport,
  CloseExecutionRecord,
  ClosePrecheck,
  FiscalPeriod,
  IncomeStatementReport,
  PeriodReadiness,
  ReconciliationReport,
} from './fiscal-periods.models';

@Component({
  selector: 'app-fiscal-periods-page',
  imports: [],
  templateUrl: './fiscal-periods.page.html',
  styleUrl: './fiscal-periods.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FiscalPeriodsPage {
  readonly http = inject(HttpClient);
  readonly i18n = inject(I18nService);
  readonly notification = inject(NotificationService);
  readonly auth = inject(AuthService);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly periods = signal<FiscalPeriod[]>([]);
  readonly year = signal<number>(new Date().getFullYear());
  readonly selectedPeriod = signal<FiscalPeriod | null>(null);
  readonly balanceSheet = signal<BalanceSheetReport | null>(null);
  readonly incomeStatement = signal<IncomeStatementReport | null>(null);
  readonly reconciliations = signal<ReconciliationReport[]>([]);
  readonly precheck = signal<ClosePrecheck | null>(null);
  readonly readiness = signal<PeriodReadiness | null>(null);
  readonly closeEvidence = signal<CloseExecutionRecord[]>([]);
  readonly workbenchLoading = signal(false);
  readonly reconciliationType = signal('AR');

  constructor() {
    void this.load();
  }

  canGenerateReconciliation(): boolean {
    return this.auth.hasAnyRole(['FINANCE_MANAGER', 'ACCOUNTANT']);
  }

  canClose(): boolean {
    return this.auth.hasAnyRole(['FINANCE_MANAGER']);
  }

  async load() {
    this.loading.set(true);
    this.error.set(null);
    this.periods.set([]);
    try {
      const data = await firstValueFrom(
        this.http.get<FiscalPeriod[]>('/api/v1/fiscal-periods', { params: { year: this.year() } }),
      );
      this.periods.set(data);
    } catch (e) {
      if (e instanceof HttpErrorResponse && (e.status === 404 || e.status === 204)) {
        this.periods.set([]);
        return;
      }
      this.error.set(apiErrorMessage(e, this.i18n));
    } finally {
      this.loading.set(false);
    }
  }

  async generateYear() {
    this.loading.set(true);
    try {
      const data = await firstValueFrom(
        this.http.post<FiscalPeriod[]>(`/api/v1/fiscal-periods/generate-year?year=${this.year()}`, {}),
      );
      this.periods.set(data);
      this.notification.success(this.i18n.t('fiscalPeriods.generatedSuccess'));
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    } finally {
      this.loading.set(false);
    }
  }

  async toggleStatus(period: FiscalPeriod, targetStatus: 'OPEN' | 'LOCKED') {
    try {
      await firstValueFrom(this.http.put(`/api/v1/fiscal-periods/${period.id}/status`, {
        status: targetStatus,
        expectedVersion: period.version,
      }));
      this.notification.success(this.i18n.t('financeWorkbench.statusChanged', { status: targetStatus }));
      await this.load();
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    }
  }

  async openWorkbench(period: FiscalPeriod) {
    this.selectedPeriod.set(period);
    this.workbenchLoading.set(true);
    this.error.set(null);
    this.closeEvidence.set([]);
    try {
      const startDate = this.isoDate(period.startDate);
      const endDate = this.isoDate(period.endDate);
      const [balanceSheet, incomeStatement, reconciliations, precheck, readiness] = await Promise.all([
        firstValueFrom(this.http.get<BalanceSheetReport>('/api/v1/finance/reports/balance-sheet', { params: { asOfDate: endDate } })),
        firstValueFrom(this.http.get<IncomeStatementReport>('/api/v1/finance/reports/income-statement', { params: { startDate, endDate } })),
        firstValueFrom(this.http.get<ReconciliationReport[]>(`/api/v1/finance/reconciliation/subledger/periods/${period.id}`)),
        firstValueFrom(this.http.get<ClosePrecheck>(`/api/v1/fiscal-periods/${period.id}/precheck`)),
        firstValueFrom(this.http.get<PeriodReadiness>(`/api/v1/finance/period-close/readiness/${period.id}`)),
      ]);
      this.balanceSheet.set(balanceSheet);
      this.incomeStatement.set(incomeStatement);
      this.reconciliations.set(reconciliations);
      this.precheck.set(precheck);
      this.readiness.set(readiness);
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    } finally {
      this.workbenchLoading.set(false);
    }
  }

  async generateReconciliation() {
    const period = this.selectedPeriod();
    if (!period) return;
    this.workbenchLoading.set(true);
    try {
      await firstValueFrom(this.http.post('/api/v1/finance/reconciliation/subledger/generate', {
        periodId: period.id,
        subledgerType: this.reconciliationType(),
      }));
      this.notification.success(this.i18n.t('financeWorkbench.reconciliationGenerated'));
      await this.openWorkbench(period);
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
      this.workbenchLoading.set(false);
    }
  }

  async executeClose() {
    const period = this.selectedPeriod();
    if (!period) return;
    this.workbenchLoading.set(true);
    try {
      const evidence = await firstValueFrom(this.http.post<CloseExecutionRecord[]>(
        `/api/v1/finance/period-close/execute/${period.id}`,
        {},
        { params: { expectedVersion: period.version } },
      ));
      this.closeEvidence.set(evidence);
      this.notification.success(this.i18n.t('financeWorkbench.closeCompleted'));
      await this.load();
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    } finally {
      this.workbenchLoading.set(false);
    }
  }

  differenceDetails(report: ReconciliationReport): string {
    try {
      const parsed = JSON.parse(report.differenceDetails) as unknown;
      return Array.isArray(parsed) && parsed.length === 0
        ? this.i18n.t('financeWorkbench.noDifferences')
        : report.differenceDetails;
    } catch {
      return report.differenceDetails;
    }
  }

  date(ms: number) {
    return formatDate(ms);
  }

  private isoDate(ms: number): string {
    return new Date(ms).toISOString().slice(0, 10);
  }
}
