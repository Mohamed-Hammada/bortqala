import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { dateInputToEpoch, epochToDateInput, formatDate, formatDateTime } from '../../core/date';
import { PeriodOption, ReportPayCycle, ReportStatus } from './reports.models';
import { ReportsStore } from './reports.store';
import { I18nService } from '../../core/i18n.service';
import { TablePagination } from '../../shared/ui/table-pagination/pagination';
import { TablePaginationComponent } from '../../shared/ui/table-pagination/table-pagination.component';

@Component({
  selector: 'app-reports-page',
  imports: [RouterLink, ReactiveFormsModule, TablePaginationComponent],
  providers: [ReportsStore],
  templateUrl: './reports.page.html',
  styleUrl: './reports.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ReportsPage {
  readonly store = inject(ReportsStore);
  readonly i18n = inject(I18nService);
  readonly year = signal(new Date().getFullYear());
  readonly customError = signal<string | null>(null);
  readonly pagination = new TablePagination();
  readonly pagedReports = computed(() => this.pagination.slice(this.store.reports()));
  private readonly formBuilder = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly initialRange = this.currentMonthRange();
  readonly periodForm = this.formBuilder.nonNullable.group({
    periodStart: [this.initialRange.start, Validators.required],
    periodEnd: [this.initialRange.end, Validators.required],
    payCycle: ['MONTHLY' as ReportPayCycle, Validators.required],
  });

  constructor() {
    void this.store.list(this.year());
  }
  changeYear(value: string): void {
    this.year.set(Number(value));
    void this.store.list(this.year());
  }
  async create(period: PeriodOption): Promise<void> {
    const id = await this.store.create({
      periodStart: period.start,
      periodEnd: period.end,
      payCycle: period.kind === 'MONTHLY' ? 'MONTHLY' : 'HALF_MONTHLY',
    });
    if (id) await this.router.navigate(['/reports', id]);
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
    if (id) await this.router.navigate(['/reports', id]);
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
  date(value: number): string {
    return formatDate(value);
  }
  dateTime(value: number): string {
    return formatDateTime(value);
  }
  label(status: ReportStatus): string {
    return this.i18n.t(
      {
        DRAFT: 'status.draft',
        IN_REVIEW: 'status.inReview',
        APPROVED: 'status.approved',
        EXPORTED: 'status.exported',
      }[status],
    );
  }

  payCycleLabel(value: ReportPayCycle): string {
    return this.i18n.t(
      {
        MONTHLY: 'reports.monthlyShort',
        HALF_MONTHLY: 'reports.halfMonthly',
        THIRTY_DAYS: 'reports.thirtyDays',
      }[value],
    );
  }

  private currentMonthRange(): { start: string; end: string } {
    const today = epochToDateInput(Date.now());
    const [year, month] = today.split('-').map(Number);
    return {
      start: `${year}-${String(month).padStart(2, '0')}-01`,
      end: epochToDateInput(Date.UTC(year, month, 0)),
    };
  }
}
