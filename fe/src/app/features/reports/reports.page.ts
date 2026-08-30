import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { dateInputToEpoch, epochToDateInput, formatDate, formatDateHijri, formatDateTime } from '../../core/date';
import { AuthService } from '../../core/auth/auth.service';
import { AppSettings } from '../../core/auth/auth.models';
import { GeneratedPeriod, PeriodOption, ReportPayCycle, ReportPreview, ReportStatus } from './reports.models';
import { ReportsStore } from './reports.store';
import { I18nService } from '../../core/i18n.service';
import { NotificationService } from '../../core/notification.service';
import { TablePagination } from '../../shared/ui/table-pagination/pagination';
import { TablePaginationComponent } from '../../shared/ui/table-pagination/table-pagination.component';
import { DataExchangeCenterComponent } from '../../shared/ui/data-exchange-center/data-exchange-center.component';
import { BusinessReportsCatalogComponent } from './business-reports-catalog.component';

export interface ReportSchedule {
  id: string;
  name: string;
  reportKind: string;
  params: string | null;
  channel: string;
  recipients: string | null;
  cadence: string;
  timeOfDay: string | null;
  active: boolean;
  lastRunAt: number | null;
  lastStatus: string | null;
  lastError: string | null;
  consecutiveFailures: number;
  version: number;
}

@Component({
  selector: 'app-reports-page',
  imports: [DataExchangeCenterComponent, RouterLink, ReactiveFormsModule, TablePaginationComponent, BusinessReportsCatalogComponent],
  providers: [ReportsStore],
  templateUrl: './reports.page.html',
  styleUrl: './reports.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ReportsPage {
  readonly store = inject(ReportsStore);
  readonly i18n = inject(I18nService);
  readonly authService = inject(AuthService);
  readonly year = signal(new Date().getFullYear());
  readonly customError = signal<string | null>(null);
  readonly previewResult = signal<ReportPreview | null>(null);
  readonly previewing = signal(false);
  readonly appSettings = signal<AppSettings | null>(null);
  readonly showReportPresets = computed(() => this.appSettings()?.showReportPresets ?? true);
  readonly pagination = new TablePagination();
  readonly pagedReports = computed(() => this.pagination.slice(this.store.reports()));
  private readonly formBuilder = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly initialRange = this.currentDayRange();
  readonly periodForm = this.formBuilder.nonNullable.group({
    periodStart: [this.initialRange.start, Validators.required],
    periodEnd: [this.initialRange.end, Validators.required],
    payCycle: ['MONTHLY' as ReportPayCycle, Validators.required],
  });

  constructor() {
    void this.store.list(this.year());
    this.authService.appSettings().subscribe({
      next: (settings) => this.appSettings.set(settings),
      error: () => {},
    });
    this.periodForm.valueChanges.subscribe(() => this.previewResult.set(null));
    void this.loadSchedules();
  }

  changeYear(value: string): void {
    this.year.set(Number(value));
    void this.store.list(this.year());
  }

  onPeriodStartChanged(): void {
    const start = this.periodForm.controls.periodStart.value;
    if (!start) return;
    // A newly selected start date begins as a one-day range.
    // The user can then deliberately extend the end date.
    this.periodForm.controls.periodEnd.setValue(start);
    this.previewResult.set(null);
    this.customError.set(null);
  }

  /**
   * Preset cards are shortcuts for configuring the form, not an immediate
   * create side effect. The user still has Preview and Create as explicit actions.
   */
  applyPreset(period: PeriodOption): void {
    this.customError.set(null);
    this.previewResult.set(null);
    this.periodForm.setValue({
      periodStart: epochToDateInput(period.start),
      periodEnd: epochToDateInput(period.end),
      payCycle: period.kind === 'MONTHLY' ? 'MONTHLY' : 'HALF_MONTHLY',
    });
    this.periodForm.markAsDirty();
    document.querySelector<HTMLFormElement>('form')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  async createCustom(): Promise<void> {
    this.customError.set(null);
    if (this.periodForm.invalid) {
      this.periodForm.markAllAsTouched();
      return;
    }
    const value = this.periodForm.getRawValue();
    const periodStart = dateInputToEpoch(value.periodStart);
    const periodEnd = dateInputToEpoch(value.periodEnd);
    if (periodEnd < periodStart) {
      this.customError.set(this.i18n.t('reports.invalidRange'));
      return;
    }
    const id = await this.store.create({ periodStart, periodEnd, payCycle: value.payCycle });
    if (id) {
      await this.router.navigate(['/reports', id]);
    } else if (this.store.error()) {
      this.customError.set(this.store.error());
    }
  }

  async previewCustom(): Promise<void> {
    this.customError.set(null);
    if (this.periodForm.invalid) {
      this.periodForm.markAllAsTouched();
      return;
    }
    const value = this.periodForm.getRawValue();
    this.previewing.set(true);
    try {
      const preview = await this.store.preview(value.periodStart, value.periodEnd, value.payCycle);
      this.previewResult.set(preview);
      if (!preview && this.store.error()) this.customError.set(this.store.error());
    } finally {
      this.previewing.set(false);
    }
  }

  periodName(period: PeriodOption): string {
    const suffixKey = {
      MONTHLY: 'reports.monthlyShort',
      FIRST_HALF: 'reports.firstHalf',
      SECOND_HALF: 'reports.secondHalf',
    }[period.kind];
    const month = new Intl.DateTimeFormat(this.i18n.locale(), {
      month: 'long',
      timeZone: 'Africa/Cairo',
    }).format(new Date(period.start));
    return `${month} — ${this.i18n.t(suffixKey)}`;
  }

  cycleLabel(period: PeriodOption): string {
    return this.i18n.t(period.kind === 'MONTHLY' ? 'reports.cycleMonthly' : 'reports.cycleHalf');
  }

  /**
   * Returns the finalized report overlapping this preset period, if any.
   * Finalized = APPROVED or EXPORTED; drafts never lock a month (WP-06 AC-2).
   */
  generatedFor(period: PeriodOption): GeneratedPeriod | null {
    return this.store.generated().find((generated) =>
      generated.from <= period.end && generated.to >= period.start) ?? null;
  }

  date(value: number): string { return formatDate(value); }
  readonly hijriEnabled = signal<boolean>(
    typeof localStorage === 'undefined' ? false : localStorage.getItem('calendar.hijriOverlay') === 'true',
  );
  readonly hijriDate = (value: number): string => formatDateHijri(value);
  dateTime(value: number): string { return formatDateTime(value); }
  label(status: ReportStatus): string {
    return this.i18n.t({
      DRAFT: 'status.draft', IN_REVIEW: 'status.inReview',
      APPROVED: 'status.approved', EXPORTED: 'status.exported',
    }[status]);
  }

  payCycleLabel(value: ReportPayCycle): string {
    return this.i18n.t({
      MONTHLY: 'reports.monthlyShort',
      HALF_MONTHLY: 'reports.halfMonthly',
      THIRTY_DAYS: 'reports.thirtyDays',
    }[value]);
  }

  private currentDayRange(): { start: string; end: string } {
    const now = new Date();
    const yyyy = now.getFullYear();
    const mm = String(now.getMonth() + 1).padStart(2, '0');
    const dd = String(now.getDate()).padStart(2, '0');
    const today = `${yyyy}-${mm}-${dd}`;
    return { start: today, end: today };
  }

  private readonly http = inject(HttpClient);
  private readonly notification = inject(NotificationService);

  readonly schedules = signal<ReportSchedule[]>([]);
  readonly showScheduleForm = signal(false);
  readonly savingSchedule = signal(false);
  readonly runningSchedule = signal<string | null>(null);
  readonly formatDateTime = formatDateTime;

  readonly scheduleForm = this.formBuilder.nonNullable.group({
    name: ['', Validators.required],
    reportKind: ['CASHFLOW', Validators.required],
    channel: ['EMAIL', Validators.required],
    cadence: ['DAILY', Validators.required],
    timeOfDay: ['08:00'],
    recipients: [''],
    params: ['{}'],
  });

  async loadSchedules(): Promise<void> {
    try {
      const list = await firstValueFrom(this.http.get<ReportSchedule[]>('/api/v1/report-schedules'));
      this.schedules.set(list ?? []);
    } catch { /* ignore - page still works without schedules */ }
  }

  async createSchedule(): Promise<void> {
    if (this.scheduleForm.invalid) return;
    this.savingSchedule.set(true);
    try {
      await firstValueFrom(this.http.post('/api/v1/report-schedules', this.scheduleForm.getRawValue()));
      this.showScheduleForm.set(false);
      this.scheduleForm.reset({ reportKind: 'CASHFLOW', channel: 'EMAIL', cadence: 'DAILY', timeOfDay: '08:00', params: '{}' });
      this.notification.success(this.i18n.t('reports.scheduleCreated'));
      await this.loadSchedules();
    } catch { /* error handled by notification */ }
    finally { this.savingSchedule.set(false); }
  }

  async runSchedule(id: string): Promise<void> {
    this.runningSchedule.set(id);
    try {
      await firstValueFrom(this.http.post(`/api/v1/report-schedules/${id}/run-now`, {}));
      this.notification.success(this.i18n.t('reports.runStarted'));
      await this.loadSchedules();
    } catch { /* error handled by notification */ }
    finally { this.runningSchedule.set(null); }
  }

  async deleteSchedule(id: string): Promise<void> {
    try {
      await firstValueFrom(this.http.delete(`/api/v1/report-schedules/${id}`));
      this.notification.success(this.i18n.t('reports.scheduleDeleted'));
      await this.loadSchedules();
    } catch { /* error handled by notification */ }
  }
}
