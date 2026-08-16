import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { formatDateTime } from '../../core/date';
import { I18nService } from '../../core/i18n.service';
import { apiErrorMessage } from '../../core/api-error';
import { AttendanceApiService } from './attendance-api.service';
import { AttendanceEmployeeSummary, AttendanceMonthSummary } from './attendance.models';

@Component({
  selector: 'app-attendance-browser-page',
  imports: [RouterLink],
  templateUrl: './attendance-browser.page.html',
  styleUrl: './attendance-browser.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AttendanceBrowserPage {
  readonly api = inject(AttendanceApiService);
  readonly i18n = inject(I18nService);
  readonly months = signal<AttendanceMonthSummary[]>([]);
  readonly employees = signal<AttendanceEmployeeSummary[]>([]);
  readonly selectedMonth = signal('');
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly search = signal('');

  readonly filteredEmployees = computed(() => {
    const query = this.search().trim().toLowerCase();
    if (!query) return this.employees();
    return this.employees().filter((item) =>
      [item.deviceUserId, item.observedName, item.employeeCode, item.employeeName]
        .filter(Boolean)
        .some((value) => value!.toLowerCase().includes(query)),
    );
  });

  constructor() {
    void this.load();
  }

  async load(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      const months = await this.api.months();
      this.months.set(months);
      if (months.length > 0) {
        await this.selectMonth(months[0].month);
      }
    } catch (error) {
      this.error.set(apiErrorMessage(error, this.i18n));
    } finally {
      this.loading.set(false);
    }
  }

  async selectMonth(month: string): Promise<void> {
    this.selectedMonth.set(month);
    this.loading.set(true);
    this.error.set(null);
    try {
      this.employees.set(await this.api.employees(month));
    } catch (error) {
      this.error.set(apiErrorMessage(error, this.i18n));
    } finally {
      this.loading.set(false);
    }
  }

  displayName(item: AttendanceEmployeeSummary): string {
    return item.employeeName || item.observedName || item.deviceUserId;
  }

  dateTime(value: number): string {
    return formatDateTime(value);
  }

  monthLabel(month: string): string {
    const [year, monthNumber] = month.split('-').map(Number);
    if (!year || !monthNumber) return month;
    return new Intl.DateTimeFormat(this.i18n.locale(), { month: 'long', year: 'numeric' })
      .format(new Date(year, monthNumber - 1, 1));
  }

  setSearch(event: Event): void {
    this.search.set((event.target as HTMLInputElement).value);
  }
}
