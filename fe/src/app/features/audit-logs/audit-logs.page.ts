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

  constructor() {
    void this.load(1);
  }

  async load(pageNumber: number = 1) {
    this.loading.set(true);
    this.error.set(null);
    this.pagination.changePage(pageNumber, this.totalElements());
    try {
      const params = {
        page: pageNumber - 1,
        size: this.pagination.pageSize(),
      };
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

  dateTime(ms: number) {
    return formatDateTime(ms);
  }
}
