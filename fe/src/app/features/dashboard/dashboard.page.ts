import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DashboardStore } from './dashboard.store';
import { TablePagination } from '../../shared/ui/table-pagination/pagination';
import { TablePaginationComponent } from '../../shared/ui/table-pagination/table-pagination.component';

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
  readonly year = signal(new Date().getFullYear());
  readonly month = signal(new Date().getMonth() + 1);
  readonly pagination = new TablePagination();
  readonly pagedCategories = computed(() => this.pagination.slice(this.store.data()?.categories ?? []));
  readonly months = [
    'يناير',
    'فبراير',
    'مارس',
    'أبريل',
    'مايو',
    'يونيو',
    'يوليو',
    'أغسطس',
    'سبتمبر',
    'أكتوبر',
    'نوفمبر',
    'ديسمبر',
  ];
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
    return (
      (
        {
          DRAFT: 'مسودة',
          IN_REVIEW: 'تحت المراجعة',
          APPROVED: 'معتمد',
          EXPORTED: 'تم التصدير',
        } as Record<string, string>
      )[value ?? ''] ?? 'لا يوجد تقرير'
    );
  }
}
