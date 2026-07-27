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
  changePeriod(yearStr: string, monthStr: string): void {
    const y = Number(yearStr);
    const m = Number(monthStr);
    if (!isNaN(y) && y >= 2000 && y <= 2100 && !isNaN(m) && m >= 1 && m <= 12) {
      this.year.set(y);
      this.month.set(m);
      void this.store.load(y, m);
    }
  }
  reload(): void {
    void this.store.load(this.year(), this.month());
  }

  formatLastUpdated(value: string | null): string {
    if (!value) return new Date().toLocaleTimeString('ar-EG', { hour: '2-digit', minute: '2-digit' });
    try {
      const d = new Date(value);
      return d.toLocaleTimeString('ar-EG', { hour: '2-digit', minute: '2-digit', second: '2-digit' });
    } catch {
      return value;
    }
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
