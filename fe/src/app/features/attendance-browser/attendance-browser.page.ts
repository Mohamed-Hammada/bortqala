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
  readonly fromDate = signal('');
  readonly toDate = signal('');

  readonly monthDialogOpen = signal(false);
  readonly selectedYear = signal<number | null>(null);
  readonly years = computed(() => Array.from(new Set(
    this.months().map((item) => Number(item.month.slice(0, 4))).filter((year) => Number.isFinite(year)),
  )).sort((a, b) => b - a));
  readonly monthsForYear = computed(() => {
    const year = this.selectedYear();
    if (!year) return [];
    return this.months().filter((item) => Number(item.month.slice(0, 4)) === year);
  });
  readonly currentMonthSummary = computed(() => this.months().find((item) => item.month === this.selectedMonth()) ?? null);

  readonly filteredEmployees = computed(() => {
    const query = this.search().trim().toLowerCase();
    const from = this.startOfDay(this.fromDate());
    const to = this.endOfDay(this.toDate());
    return this.employees().filter((item) => {
      const textOk = !query || [item.deviceUserId, item.observedName, item.employeeCode, item.employeeName]
        .filter(Boolean)
        .some((value) => value!.toLowerCase().includes(query));
      const dateOk = (!from || item.lastPunch >= from) && (!to || item.firstPunch <= to);
      return textOk && dateOk;
    });
  });

  constructor() { void this.load(); }

  async load(): Promise<void> {
    this.loading.set(true); this.error.set(null);
    try {
      const months = await this.api.months();
      this.months.set(months);
      if (months.length > 0) {
        this.selectedYear.set(Number(months[0].month.slice(0, 4)));
        await this.selectMonth(months[0].month);
      }
    } catch (error) { this.error.set(apiErrorMessage(error, this.i18n)); }
    finally { this.loading.set(false); }
  }

  openMonthDialog(): void {
    const currentYear = Number(this.selectedMonth().slice(0, 4));
    this.selectedYear.set(Number.isFinite(currentYear) && currentYear > 0 ? currentYear : (this.years()[0] ?? null));
    this.monthDialogOpen.set(true);
  }

  closeMonthDialog(): void { this.monthDialogOpen.set(false); }
  chooseYear(event: Event): void { this.selectedYear.set(Number((event.target as HTMLSelectElement).value)); }

  async chooseMonth(month: string): Promise<void> {
    await this.selectMonth(month);
    this.closeMonthDialog();
  }

  async selectMonth(month: string): Promise<void> {
    if (!month) return;
    this.selectedMonth.set(month); this.loading.set(true); this.error.set(null);
    const [year, monthNumber] = month.split('-').map(Number);
    if (year && monthNumber) {
      const first = new Date(year, monthNumber - 1, 1);
      const last = new Date(year, monthNumber, 0);
      this.fromDate.set(this.dateInput(first));
      this.toDate.set(this.dateInput(last));
    }
    try { this.employees.set(await this.api.employees(month)); }
    catch (error) { this.error.set(apiErrorMessage(error, this.i18n)); }
    finally { this.loading.set(false); }
  }

  displayName(item: AttendanceEmployeeSummary): string { return item.employeeName || item.observedName || item.deviceUserId; }
  dateTime(value: number): string { return formatDateTime(value); }
  monthLabel(month: string): string {
    const [year, monthNumber] = month.split('-').map(Number);
    if (!year || !monthNumber) return month;
    return new Intl.DateTimeFormat(this.i18n.locale(), { month: 'long', year: 'numeric' }).format(new Date(year, monthNumber - 1, 1));
  }
  shortMonthLabel(month: string): string {
    const [year, monthNumber] = month.split('-').map(Number);
    if (!year || !monthNumber) return month;
    return new Intl.DateTimeFormat(this.i18n.locale(), { month: 'long' }).format(new Date(year, monthNumber - 1, 1));
  }
  setSearch(event: Event): void { this.search.set((event.target as HTMLInputElement).value); }
  setFromDate(event: Event): void { this.fromDate.set((event.target as HTMLInputElement).value); }
  setToDate(event: Event): void { this.toDate.set((event.target as HTMLInputElement).value); }
  resetFilters(): void {
    this.search.set('');
    const month = this.selectedMonth();
    if (month) {
      const [year, monthNumber] = month.split('-').map(Number);
      this.fromDate.set(this.dateInput(new Date(year, monthNumber - 1, 1)));
      this.toDate.set(this.dateInput(new Date(year, monthNumber, 0)));
    } else { this.fromDate.set(''); this.toDate.set(''); }
  }

  exportCsv(): void {
    const ar = this.i18n.locale().toLowerCase().startsWith('ar');
    const rows = [
      [ar ? 'الموظف' : 'Employee', ar ? 'كود الموظف' : 'Employee code', ar ? 'رقم الجهاز' : 'Device ID', ar ? 'الحالة' : 'Status', ar ? 'البصمات' : 'Punches', ar ? 'أول بصمة' : 'First punch', ar ? 'آخر بصمة' : 'Last punch'],
      ...this.filteredEmployees().map((item) => [
        this.displayName(item), item.employeeCode ?? '', item.deviceUserId,
        item.mapped ? (ar ? 'مربوط' : 'Mapped') : (ar ? 'غير مربوط' : 'Unmapped'),
        String(item.punchCount), this.dateTime(item.firstPunch), this.dateTime(item.lastPunch),
      ]),
    ];
    const csv = '\uFEFF' + rows.map((row) => row.map((value) => `"${String(value).replace(/"/g, '""')}"`).join(',')).join('\r\n');
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' });
    const url = URL.createObjectURL(blob); const anchor = document.createElement('a');
    anchor.href = url; anchor.download = `attendance-${this.selectedMonth() || 'export'}.csv`; anchor.click(); URL.revokeObjectURL(url);
  }

  private startOfDay(value: string): number | null { if (!value) return null; const d = new Date(`${value}T00:00:00`); return Number.isNaN(d.getTime()) ? null : d.getTime(); }
  private endOfDay(value: string): number | null { if (!value) return null; const d = new Date(`${value}T23:59:59.999`); return Number.isNaN(d.getTime()) ? null : d.getTime(); }
  private dateInput(date: Date): string { const y = date.getFullYear(); const m = `${date.getMonth()+1}`.padStart(2,'0'); const d = `${date.getDate()}`.padStart(2,'0'); return `${y}-${m}-${d}`; }
}
