import { DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { DashboardStore } from './dashboard.store';
import { TablePagination } from '../../shared/ui/table-pagination/pagination';
import { TablePaginationComponent } from '../../shared/ui/table-pagination/table-pagination.component';
import { I18nService } from '../../core/i18n.service';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-dashboard-page',
  imports: [RouterLink, TablePaginationComponent, FormsModule, DecimalPipe],
  providers: [DashboardStore],
  templateUrl: './dashboard.page.html',
  styleUrl: './dashboard.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardPage {
  readonly route = inject(ActivatedRoute);
  readonly router = inject(Router);
  readonly store = inject(DashboardStore);
  readonly i18n = inject(I18nService);
  readonly year = signal(new Date().getFullYear());
  readonly month = signal(new Date().getMonth() + 1);
  readonly pagination = new TablePagination();
  readonly pagedCategories = computed(() =>
    this.pagination.slice(this.store.data()?.categories ?? []),
  );
  readonly monthKeys = Array.from({ length: 12 }, (_, index) => `month.${index + 1}`);
  readonly activeKpiModal = signal<{ title: string; value: string | number; details: string[] } | null>(null);

  readonly selectedPeriod = signal<'WEEK' | 'MONTH'>('MONTH');
  readonly selectedDepartmentId = signal<string | null>(null);
  readonly departmentOptions = computed(() => {
    const depts = this.store.departmentMetrics();
    return [{ id: null as string | null, name: this.i18n.t('dashboard.allDepartments') }, ...depts.map(d => ({ id: d.departmentId, name: d.departmentName }))];
  });
  readonly chartMaxValue = computed(() => {
    const points = this.store.chartData();
    if (!points.length) return 100;
    return Math.max(...points.flatMap(p => [p.present, p.absent, p.late]), 10);
  });
  readonly totalEmployees = computed(() => this.store.data()?.activeEmployees ?? 0);
  readonly attendanceRate = computed(() => this.store.data()?.attendanceRate ?? 0);
  readonly pendingApprovals = computed(() => this.store.data()?.unresolvedCount ?? 0);

  constructor() {
    this.route.queryParams.subscribe((params: Record<string, string>) => {
      const y = Number(params['year']) || new Date().getFullYear();
      const m = Number(params['month']) || new Date().getMonth() + 1;
      if (y !== this.year() || m !== this.month()) {
        this.year.set(y);
        this.month.set(m);
      }
      void this.loadAll();
    });
  }

  private async loadAll(): Promise<void> {
    await this.store.loadAll(
      this.year(),
      this.month(),
      this.selectedPeriod(),
      this.selectedDepartmentId(),
    );
  }

  changePeriodFilter(): void {
    void this.store.loadChartData(this.selectedPeriod(), this.selectedDepartmentId(), this.year(), this.month());
  }

  onDepartmentChange(): void {
    void this.store.loadChartData(this.selectedPeriod(), this.selectedDepartmentId(), this.year(), this.month());
  }

  openKpiDetails(title: string, value: string | number, details: string[]) {
    this.activeKpiModal.set({ title, value, details });
  }

  closeKpiModal() {
    this.activeKpiModal.set(null);
  }

  changePeriod(yearStr: string, monthStr: string): void {
    const y = Number(yearStr);
    const m = Number(monthStr);
    if (!isNaN(y) && y >= 2020 && y <= 2035 && !isNaN(m) && m >= 1 && m <= 12) {
      this.year.set(y);
      this.month.set(m);
      void this.router.navigate([], {
        relativeTo: this.route,
        queryParams: { year: y, month: m },
        queryParamsHandling: 'merge',
      });
    }
  }
  reload(): void {
    void this.loadAll();
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
