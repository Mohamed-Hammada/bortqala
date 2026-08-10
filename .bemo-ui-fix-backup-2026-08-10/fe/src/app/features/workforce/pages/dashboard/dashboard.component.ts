import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { AuthService } from '../../../../core/auth/auth.service';
import { apiErrorDetail } from '../../../../core/api-error';
import { WorkforceService } from '../../data-access/workforce.service';

@Component({
  selector: 'app-workforce-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="workforce-container" [class.motion-enabled]="animationsEnabled()" [class.motion-disabled]="!animationsEnabled()">
      <header class="page-header">
        <div><span class="eyebrow">العمالة والمقاولون</span><h1>لوحة متابعة العمالة والتشغيل اليومي</h1></div>
      </header>

      <section class="card shared-filters" aria-label="فلاتر لوحة القوى العاملة">
        <div><label>المقاول</label><select [ngModel]="selectedContractorId()" (ngModelChange)="selectedContractorId.set($event); persistFilters()"><option value="">الكل</option>@for (item of workforceService.contractors(); track item.id) {<option [value]="item.id">{{ item.name }}</option>}</select></div>
        <div><label>فئة العامل</label><select [ngModel]="selectedCategoryId()" (ngModelChange)="selectedCategoryId.set($event); persistFilters()"><option value="">الكل</option>@for (item of workforceService.categories(); track item.id) {<option [value]="item.id">{{ item.name }}</option>}</select></div>
        <div><label>الموقع</label><select [ngModel]="selectedLocationId()" (ngModelChange)="selectedLocationId.set($event); persistFilters()"><option value="">كل المواقع</option>@for (location of locationOptions(); track location) {<option [value]="location">{{ location }}</option>}</select></div>
        <div><label>حالة النشاط</label><select [ngModel]="selectedStatus()" (ngModelChange)="selectedStatus.set($event); persistFilters()"><option value="">الكل</option><option value="ACTIVE">نشط</option><option value="INACTIVE">غير نشط</option></select></div>
        <button type="button" (click)="clearFilters()">مسح الفلاتر</button>
        <small>الفلاتر مشتركة بين البطاقات والرسوم ومحفوظة داخل رابط الصفحة.</small>
      </section>

      @if (loading()) { <div class="loading-state">جاري تحميل البيانات...</div> }
      @else if (loadError()) { <div class="error-state">{{ loadError() }} <button (click)="ngOnInit()">إعادة المحاولة</button></div> }
      @else {
        <div class="kpi-grid">
          <a class="kpi-card" routerLink="/workforce/contractors"><span class="kpi-title">المقاولون النشطون</span><span class="kpi-value">{{ filteredContractorCount() }}</span><small>فتح التفاصيل ←</small></a>
          <a class="kpi-card" routerLink="/workforce/workers" [queryParams]="filterQueryParams()"><span class="kpi-title">إجمالي العمالة المطابقة</span><span class="kpi-value">{{ filteredWorkers().length }}</span><small>فتح العمال ←</small></a>
          <a class="kpi-card" routerLink="/workforce/labor-requests"><span class="kpi-title">طلبات العمالة النشطة</span><span class="kpi-value">{{ workforceService.laborRequests().length }}</span><small>فتح الطلبات ←</small></a>
          <a class="kpi-card" routerLink="/workforce/advances"><span class="kpi-title">السلف القائمة</span><span class="kpi-value">{{ workforceService.advances().length }}</span><small>فتح السلف ←</small></a>
        </div>

        <div class="dashboard-grid">
          <section class="card chart-card">
            <div class="chart-head"><div><span class="chart-eyebrow">توزيع القوى العاملة</span><h3>العمال حسب المقاول</h3></div><strong>{{ filteredWorkers().length }} عامل</strong></div>
            <div class="bar-chart" role="img" aria-label="رسم يوضح عدد العمال حسب المقاول">
              @for (item of contractorSeries(); track item.id) {
                <a class="bar-row" routerLink="/workforce/workers" [queryParams]="{ contractorId: item.id }"><span>{{ item.label }}</span><div class="bar-track"><i [style.width.%]="item.percent"></i></div><strong>{{ item.value }}</strong></a>
              } @empty { <p class="empty-cell">لا توجد بيانات كافية للرسم</p> }
            </div>
          </section>

          <section class="card chart-card">
            <div class="chart-head"><div><span class="chart-eyebrow">المزيج التشغيلي</span><h3>العمال حسب الفئة</h3></div></div>
            <div class="bar-chart category-bars" role="img" aria-label="رسم يوضح عدد العمال حسب الفئة">
              @for (item of categorySeries(); track item.id) {
                <a class="bar-row" routerLink="/workforce/workers" [queryParams]="{ categoryId: item.id }"><span>{{ item.label }}</span><div class="bar-track"><i [style.width.%]="item.percent"></i></div><strong>{{ item.value }}</strong></a>
              } @empty { <p class="empty-cell">لا توجد بيانات كافية للرسم</p> }
            </div>
          </section>

          <section class="card chart-card donut-card">
            <div><span class="chart-eyebrow">تغطية طلبات العمالة</span><h3>المقبول مقابل المطلوب</h3></div>
            <div class="donut-layout">
              <div class="donut" [style.background]="requestCoverageStyle()" role="img" [attr.aria-label]="'نسبة تغطية الطلبات ' + requestCoverage() + ' بالمائة'"><span>{{ requestCoverage() }}%</span></div>
              <div class="legend"><span><i class="accepted"></i> مقبول: {{ acceptedWorkers() }}</span><span><i class="remaining"></i> متبقٍ: {{ requestRemaining() }}</span></div>
            </div>
          </section>

          <section class="card chart-card">
            <div class="chart-head"><div><span class="chart-eyebrow">مخاطر السلف</span><h3>الرصيد المتبقي من السلف</h3></div><strong>{{ advanceRemaining() | number:'1.0-0' }} ج.م</strong></div>
            <div class="financial-meter" role="img" [attr.aria-label]="'الرصيد المتبقي من السلف ' + advanceRemaining()"><i [style.width.%]="advanceRemainingPercent()"></i></div>
            <div class="meter-labels"><span>تم سداده {{ advancePaid() | number:'1.0-0' }}</span><span>أصل السلف {{ advanceGranted() | number:'1.0-0' }}</span></div>
          </section>

          <section class="card contractor-table">
            <h3>المقاولون ونماذج الحساب</h3>
            <table class="data-table">
              <thead><tr><th>كود المقاول</th><th>اسم المقاول</th><th>نموذج المحاسبة</th><th>دورة التسوية</th><th>الحالة</th></tr></thead>
              <tbody>
                <tr *ngFor="let c of filteredContractors()"><td><strong>{{ c.code }}</strong></td><td>{{ c.name }}</td><td><span class="badge model-badge">{{ getModelLabel(c.accountingModel) }}</span></td><td>كل {{ c.settlementCycleDays }} يوم</td><td><span class="badge active">{{ c.status }}</span></td></tr>
                <tr *ngIf="workforceService.contractors().length === 0"><td colspan="5" class="empty-cell">لا يوجد مقاولون مسجلون حالياً</td></tr>
              </tbody>
            </table>
          </section>
        </div>
      }
    </div>
  `,
  styles: [`
    .workforce-container { padding: 1.5rem; display: flex; flex-direction: column; gap: 1.5rem; direction: rtl; }
    .eyebrow { font-size: .875rem; color: #d97706; font-weight: 600; }
    .page-header h1 { font-size: 1.75rem; font-weight: 800; color: var(--ink); margin: .25rem 0 0; }
    .kpi-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 1rem; }
    .kpi-card, .card { background: var(--surface); padding: 1.25rem; border-radius: 12px; border: 1px solid var(--line); }
    .kpi-card { display: flex; flex-direction: column; gap: .5rem; }
    a.kpi-card, a.bar-row { color: inherit; text-decoration: none; }
    a.kpi-card:hover, a.bar-row:hover { border-color: #f59e0b; transform: translateY(-1px); }
    .shared-filters { display: flex; flex-wrap: wrap; align-items: end; gap: .8rem; }
    .shared-filters div { display: flex; flex-direction: column; gap: .25rem; }
    .shared-filters label, .shared-filters small { font-size: .75rem; color: var(--muted); }
    .shared-filters select, .shared-filters button { border: 1px solid var(--line); border-radius: 8px; padding: .5rem .65rem; background: white; }
    .shared-filters small { flex-basis: 100%; }
    .kpi-title, .chart-eyebrow { font-size: .8rem; color: var(--muted); font-weight: 700; }
    .kpi-value { font-size: 1.875rem; font-weight: 800; color: var(--ink); }
    .dashboard-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 1rem; }
    .contractor-table { grid-column: 1 / -1; }
    .chart-card { min-height: 245px; }
    .chart-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 1rem; }
    .chart-head h3, .donut-card h3 { margin: .2rem 0 1rem; }
    .bar-chart { display: flex; flex-direction: column; gap: .85rem; margin-top: 1rem; }
    .bar-row { display: grid; grid-template-columns: minmax(90px, 1fr) 3fr 34px; align-items: center; gap: .75rem; font-size: .82rem; }
    .bar-track { height: 12px; border-radius: 99px; background: var(--surface-muted); overflow: hidden; }
    .bar-track i { display: block; height: 100%; border-radius: inherit; background: linear-gradient(90deg, #d97706, #fbbf24); }
    .category-bars .bar-track i { background: linear-gradient(90deg, #0369a1, #38bdf8); }
    .donut-layout { display: flex; align-items: center; justify-content: space-around; gap: 1.5rem; min-height: 160px; }
    .donut { width: 135px; aspect-ratio: 1; border-radius: 50%; display: grid; place-items: center; position: relative; }
    .donut::after { content: ''; position: absolute; inset: 18px; background: var(--surface); border-radius: 50%; }
    .donut span { position: relative; z-index: 1; font-size: 1.4rem; font-weight: 800; }
    .legend { display: flex; flex-direction: column; gap: .75rem; font-size: .85rem; }
    .legend i { display: inline-block; width: 10px; height: 10px; border-radius: 2px; margin-left: .4rem; }
    .legend .accepted { background: #16a34a; } .legend .remaining { background: var(--line); }
    .financial-meter { height: 34px; border-radius: 10px; background: #dcfce7; overflow: hidden; margin: 2.5rem 0 .75rem; }
    .financial-meter i { display: block; height: 100%; background: linear-gradient(90deg, #ef4444, #f97316); }
    .meter-labels { display: flex; justify-content: space-between; color: var(--muted); font-size: .78rem; }
    .data-table { width: 100%; border-collapse: collapse; margin-top: 1rem; text-align: right; }
    .data-table th, .data-table td { padding: .75rem 1rem; border-bottom: 1px solid var(--line); }
    .badge { padding: .25rem .625rem; border-radius: 6px; font-size: .75rem; font-weight: 600; }
    .badge.active { background: #dcfce7; color: var(--success); } .badge.model-badge { background: #fef3c7; color: #92400e; }
    .empty-cell { text-align: center; color: var(--muted); padding: 2rem; }
    .loading-state, .error-state { padding: 2rem; text-align: center; color: var(--muted); } .error-state { color: var(--danger); }
    .motion-enabled .bar-track i, .motion-enabled .financial-meter i { animation: grow-bar 500ms cubic-bezier(.2,.8,.2,1) both; transform-origin: right; }
    .motion-disabled .bar-track i, .motion-disabled .financial-meter i { animation: none; }
    @keyframes grow-bar { from { transform: scaleX(0); } to { transform: scaleX(1); } }
    @media (max-width: 900px) { .dashboard-grid { grid-template-columns: 1fr; } .contractor-table { grid-column: auto; overflow-x: auto; } }
    @media (prefers-reduced-motion: reduce) { .bar-track i, .financial-meter i { animation: none !important; } }
  `]
})
export class WorkforceDashboardComponent implements OnInit {
  workforceService = inject(WorkforceService);
  private auth = inject(AuthService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  loading = signal(true);
  loadError = signal<string | null>(null);
  animationsEnabled = computed(() => this.auth.preferences().dashboardAnimationsEnabled);
  selectedContractorId = signal('');
  selectedCategoryId = signal('');
  selectedLocationId = signal('');
  selectedStatus = signal('');
  filteredWorkers = computed(() => this.workforceService.workers().filter(worker =>
    (!this.selectedContractorId() || worker.contractorId === this.selectedContractorId())
    && (!this.selectedCategoryId() || worker.categoryId === this.selectedCategoryId())
    && (!this.selectedLocationId() || worker.branchId === this.selectedLocationId())
    && (!this.selectedStatus() || worker.status === this.selectedStatus())));
  filteredContractors = computed(() => this.workforceService.contractors().filter(item =>
    !this.selectedContractorId() || item.id === this.selectedContractorId()));
  filteredContractorCount = computed(() => new Set(this.filteredWorkers().map(worker => worker.contractorId)).size);
  locationOptions = computed(() => [...new Set(this.workforceService.workers().map(worker => worker.branchId).filter((value): value is string => !!value))]);
  filterQueryParams = computed(() => ({ contractorId: this.selectedContractorId() || null, categoryId: this.selectedCategoryId() || null, branchId: this.selectedLocationId() || null, status: this.selectedStatus() || null }));
  contractorSeries = computed(() => this.breakdown(this.workforceService.contractors().map(item => ({ id: item.id, label: item.name })), worker => worker.contractorId));
  categorySeries = computed(() => this.breakdown(this.workforceService.categories().map(item => ({ id: item.id, label: item.name })), worker => worker.categoryId));
  requestedWorkers = computed(() => this.workforceService.laborRequests().flatMap(request => request.items ?? []).reduce((sum, item) => sum + item.requestedCount, 0));
  acceptedWorkers = computed(() => this.workforceService.laborRequests().flatMap(request => request.items ?? []).reduce((sum, item) => sum + item.acceptedCount, 0));
  requestRemaining = computed(() => Math.max(0, this.requestedWorkers() - this.acceptedWorkers()));
  requestCoverage = computed(() => this.requestedWorkers() > 0 ? Math.min(100, Math.round(this.acceptedWorkers() * 100 / this.requestedWorkers())) : 0);
  requestCoverageStyle = computed(() => `conic-gradient(#16a34a 0 ${this.requestCoverage()}%, #e2e8f0 ${this.requestCoverage()}% 100%)`);
  advanceGranted = computed(() => this.workforceService.advances().reduce((sum, item) => sum + Number(item.amount || 0), 0));
  advanceRemaining = computed(() => this.workforceService.advances().reduce((sum, item) => sum + Number(item.remainingBalance || 0), 0));
  advancePaid = computed(() => Math.max(0, this.advanceGranted() - this.advanceRemaining()));
  advanceRemainingPercent = computed(() => this.advanceGranted() > 0 ? Math.min(100, this.advanceRemaining() * 100 / this.advanceGranted()) : 0);

  ngOnInit() {
    this.route.queryParams.subscribe(params => {
      this.selectedContractorId.set(params['contractorId'] ?? '');
      this.selectedCategoryId.set(params['categoryId'] ?? '');
      this.selectedLocationId.set(params['locationId'] ?? '');
      this.selectedStatus.set(params['status'] ?? '');
    });
    this.loading.set(true);
    this.loadError.set(null);
    forkJoin({
      contractors: this.workforceService.loadContractors(),
      workers: this.workforceService.loadWorkers(),
      categories: this.workforceService.loadCategories(),
      requests: this.workforceService.loadLaborRequests(),
      advances: this.workforceService.loadAdvances(),
    }).subscribe({
      next: () => this.loading.set(false),
      error: (error) => { this.loadError.set('تعذّر تحميل البيانات: ' + (apiErrorDetail(error, error?.message ?? 'خطأ غير متوقع'))); this.loading.set(false); },
    });
  }

  persistFilters(): void {
    void this.router.navigate([], { relativeTo: this.route, queryParams: {
      contractorId: this.selectedContractorId() || null, categoryId: this.selectedCategoryId() || null,
      locationId: this.selectedLocationId() || null, status: this.selectedStatus() || null,
    }, queryParamsHandling: 'merge' });
  }

  clearFilters(): void {
    this.selectedContractorId.set(''); this.selectedCategoryId.set(''); this.selectedLocationId.set(''); this.selectedStatus.set('');
    this.persistFilters();
  }

  getModelLabel(model: string): string {
    return ({ worker_net_total: 'مجموع صافي العمال', contractor_daily_rate: 'سعر تعاقد مستقل', worker_cost_plus_fee: 'تكلفة العمال + عمولة', fixed_period_amount: 'مبلغ ثابت للفترة' } as Record<string, string>)[model] ?? model;
  }

  private breakdown(labels: Array<{ id: string; label: string }>, selector: (worker: { contractorId: string; categoryId: string }) => string) {
    const counts = labels.map(label => ({ ...label, value: this.filteredWorkers().filter(worker => selector(worker) === label.id).length }));
    const max = Math.max(1, ...counts.map(item => item.value));
    return counts.filter(item => item.value > 0).sort((a, b) => b.value - a.value).slice(0, 6).map(item => ({ ...item, percent: item.value * 100 / max }));
  }
}
