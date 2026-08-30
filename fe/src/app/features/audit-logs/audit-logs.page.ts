import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { I18nService } from '../../core/i18n.service';
import { apiErrorMessage } from '../../core/api-error';
import { formatDateTime } from '../../core/date';
import { AuditLog, AuditLogPage } from './audit-logs.models';
import { TablePagination } from '../../shared/ui/table-pagination/pagination';
import { TablePaginationComponent } from '../../shared/ui/table-pagination/table-pagination.component';

@Component({
  selector: 'app-audit-logs-page',
  imports: [TablePaginationComponent],
  templateUrl: './audit-logs.page.html',
  styleUrl: './audit-logs.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AuditLogsPage {
  readonly http = inject(HttpClient);
  readonly i18n = inject(I18nService);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly logs = signal<AuditLog[]>([]);
  readonly totalElements = signal<number>(0);
  readonly pagination = new TablePagination();

  readonly entityTypeFilter = signal('');
  readonly actionFilter = signal('');
  readonly usernameFilter = signal('');
  readonly searchFilter = signal('');
  readonly breakGlassFilter = signal(false);
  readonly fromFilter = signal(this.relativeDate(-1));
  readonly toFilter = signal(this.relativeDate(0));

  constructor() {
    void this.load(1);
  }

  applyFilters(): void {
    this.load(1);
  }

  retry(): void {
    this.load(1);
  }

  changePageSize(size: number): void {
    this.pagination.changePageSize(size);
    this.load(1);
  }

  resetFilters(): void {
    this.entityTypeFilter.set('');
    this.actionFilter.set('');
    this.usernameFilter.set('');
    this.searchFilter.set('');
    this.breakGlassFilter.set(false);
    this.fromFilter.set(this.relativeDate(-1));
    this.toFilter.set(this.relativeDate(0));
    this.load(1);
  }

  async load(pageNumber: number = 1) {
    this.loading.set(true);
    this.error.set(null);
    this.pagination.changePage(pageNumber, this.totalElements());
    try {
      const params: Record<string, string> = {
        page: String(pageNumber - 1),
        size: String(this.pagination.pageSize()),
      };
      const entityType = this.entityTypeFilter().trim();
      const action = this.actionFilter().trim();
      const username = this.usernameFilter().trim();
      const search = this.searchFilter().trim();
      if (entityType) params['entityType'] = entityType;
      if (action) params['action'] = action;
      if (username) params['username'] = username;
      if (search) params['search'] = search;
      if (this.breakGlassFilter()) params['isBreakGlass'] = 'true';
      const from = this.dateToEpochMillis(this.fromFilter(), true);
      const to = this.dateToEpochMillis(this.toFilter(), false);
      if (from !== null) params['from'] = String(from);
      if (to !== null) params['to'] = String(to);
      const res = await firstValueFrom(
        this.http.get<AuditLogPage>('/api/v1/audit-logs', { params }),
      );
      this.logs.set(res.content);
      this.totalElements.set(res.totalElements);
      this.pagination.changePage(pageNumber, res.totalElements);
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    } finally {
      this.loading.set(false);
    }
  }

  private dateToEpochMillis(value: string, startOfDay: boolean): number | null {
    if (!value) return null;
    const date = new Date(value + 'T00:00:00Z');
    if (Number.isNaN(date.getTime())) return null;
    if (!startOfDay) {
      date.setUTCHours(23, 59, 59, 999);
    }
    return date.getTime();
  }

  dateTime(ms: number) {
    return formatDateTime(ms);
  }

  // BORTQALA_FEEDBACK_20260816_AUDIT_DEFAULT_DATES
  private relativeDate(offsetDays: number): string {
    const date = new Date();
    date.setDate(date.getDate() + offsetDays);
    const y = date.getFullYear();
    const m = `${date.getMonth() + 1}`.padStart(2, '0');
    const d = `${date.getDate()}`.padStart(2, '0');
    return `${y}-${m}-${d}`;
  }

}
