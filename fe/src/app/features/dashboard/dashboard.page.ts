import { DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { DashboardStore } from './dashboard.store';
import { TablePagination } from '../../shared/ui/table-pagination/pagination';
import { TablePaginationComponent } from '../../shared/ui/table-pagination/table-pagination.component';
import { I18nService } from '../../core/i18n.service';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { DashboardWidgetId } from '../../core/auth/auth.models';
import { NotificationService } from '../../core/notification.service';
import { AppTooltipDirective } from '../../shared/ui/app-tooltip/app-tooltip.directive';

@Component({
  selector: 'app-dashboard-page',
  imports: [RouterLink, TablePaginationComponent, FormsModule, DecimalPipe, AppTooltipDirective],
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
  readonly auth = inject(AuthService);
  private readonly notification = inject(NotificationService);
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
  readonly dashboardEditorOpen = signal(false);
  readonly dashboardSaving = signal(false);
  readonly draftWidgetIds = signal<DashboardWidgetId[]>([]);
  readonly animationsEnabled = signal(true);
  readonly widgetOptions: ReadonlyArray<{ id: DashboardWidgetId; labelKey: string }> = [
    { id: 'summary', labelKey: 'dashboard.widgetSummary' },
    { id: 'report', labelKey: 'dashboard.widgetReport' },
    { id: 'attendance-chart', labelKey: 'dashboard.widgetAttendanceChart' },
    { id: 'insights', labelKey: 'dashboard.widgetInsights' },
    { id: 'units', labelKey: 'dashboard.widgetUnits' },
    { id: 'departments', labelKey: 'dashboard.widgetDepartments' },
    { id: 'categories', labelKey: 'dashboard.widgetCategories' },
    { id: 'imports', labelKey: 'dashboard.widgetImports' },
  ];
  readonly payrollPaidRate = computed(() => {
    const summary = this.store.payrollSummary();
    return summary?.totalEmployees ? Math.round((summary.paidCount / summary.totalEmployees) * 100) : 0;
  });
  readonly departmentInsights = computed(() => this.store.departmentMetrics().slice(0, 5));
  readonly attendanceDonutStyle = computed(() => {
    const rate = Math.max(0, Math.min(100, this.attendanceRate()));
    return `conic-gradient(var(--success) 0 ${rate}%, var(--surface-hover) ${rate}% 100%)`;
  });

  constructor() {
    void firstValueFrom(this.auth.refreshPreferences()).catch(() => undefined);
    effect(() => {
      const preferences = this.auth.preferences();
      this.draftWidgetIds.set([...preferences.dashboardWidgetIds]);
      this.animationsEnabled.set(preferences.dashboardAnimationsEnabled);
    }, { allowSignalWrites: true });
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

  widgetVisible(id: DashboardWidgetId): boolean {
    return this.draftWidgetIds().includes(id);
  }

  widgetOrder(id: DashboardWidgetId): number {
    const index = this.draftWidgetIds().indexOf(id);
    return index < 0 ? 100 : (index + 1) * 10;
  }

  openDashboardEditor(): void {
    this.draftWidgetIds.set([...this.auth.preferences().dashboardWidgetIds]);
    this.dashboardEditorOpen.set(true);
  }

  closeDashboardEditor(): void {
    this.draftWidgetIds.set([...this.auth.preferences().dashboardWidgetIds]);
    this.dashboardEditorOpen.set(false);
  }

  toggleWidget(id: DashboardWidgetId, checked: boolean): void {
    const current = this.draftWidgetIds();
    if (checked && !current.includes(id)) this.draftWidgetIds.set([...current, id]);
    if (!checked && current.includes(id) && current.length > 1) {
      this.draftWidgetIds.set(current.filter((item) => item !== id));
    }
  }

  moveWidget(id: DashboardWidgetId, direction: -1 | 1): void {
    const current = [...this.draftWidgetIds()];
    const index = current.indexOf(id);
    const next = index + direction;
    if (index < 0 || next < 0 || next >= current.length) return;
    [current[index], current[next]] = [current[next], current[index]];
    this.draftWidgetIds.set(current);
  }

  resetDashboard(): void {
    this.draftWidgetIds.set(this.widgetOptions.map((widget) => widget.id));
  }

  async saveDashboard(): Promise<void> {
    this.dashboardSaving.set(true);
    try {
      await firstValueFrom(this.auth.updateDashboardPreferences({
        widgetIds: this.draftWidgetIds(),
        animationsEnabled: this.animationsEnabled(),
      }));
      this.dashboardEditorOpen.set(false);
      this.notification.success(this.i18n.t('dashboard.customizationSaved'));
    } finally {
      this.dashboardSaving.set(false);
    }
  }

  async toggleAnimations(): Promise<void> {
    const enabled = !this.animationsEnabled();
    this.animationsEnabled.set(enabled);
    try {
      await firstValueFrom(this.auth.updateDashboardPreferences({
        widgetIds: this.auth.preferences().dashboardWidgetIds,
        animationsEnabled: enabled,
      }));
    } catch {
      this.animationsEnabled.set(!enabled);
    }
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
