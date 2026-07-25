import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DashboardStore } from './dashboard.store';
import { TablePagination } from '../../shared/ui/table-pagination/pagination';
import { TablePaginationComponent } from '../../shared/ui/table-pagination/table-pagination.component';
import { I18nService } from '../../core/i18n.service';

@Component({
  selector: 'app-dashboard-page',
  imports: [RouterLink, TablePaginationComponent],
  providers: [DashboardStore],
  templateUrl: './dashboard.page.html',
  styleUrl: './dashboard.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardPage {
  readonly store = inject(DashboardStore);
  readonly i18n = inject(I18nService);
  readonly year = signal(new Date().getFullYear());
  readonly month = signal(new Date().getMonth() + 1);
  readonly pagination = new TablePagination();
  readonly pagedCategories = computed(() =>
    this.pagination.slice(this.store.data()?.categories ?? []),
  );
  readonly monthKeys = Array.from({ length: 12 }, (_, index) => `month.${index + 1}`);
  constructor() {
    void this.store.load(this.year(), this.month());
  }
  changePeriod(year: string, month: string): void {
    this.year.set(Number(year));
    this.month.set(Number(month));
    void this.store.load(this.year(), this.month());
  }
  formatTime(value: string | null): string {
    return value?.slice(0, 5) ?? '—';
  }
  statusLabel(value: string | null): string {
    const key = (
      {
        DRAFT: 'status.draft',
        IN_REVIEW: 'status.inReview',
        APPROVED: 'status.approved',
        EXPORTED: 'status.exported',
      } as Record<string, string>
    )[value ?? ''];
    return key ? this.i18n.t(key) : this.i18n.t('dashboard.noReport');
  }
}
