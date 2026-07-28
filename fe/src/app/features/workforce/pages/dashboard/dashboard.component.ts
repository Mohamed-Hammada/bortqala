import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { forkJoin } from 'rxjs';
import { WorkforceService } from '../../data-access/workforce.service';

@Component({
  selector: 'app-workforce-dashboard',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="workforce-container">
      <header class="page-header">
        <div>
          <span class="eyebrow">العمالة والمقاولون</span>
          <h1>لوحة متابعة العمالة والتشغيل اليومي</h1>
        </div>
      </header>

      @if (loading()) { <div class="loading-state">جاري تحميل البيانات...</div> }
      @else if (loadError()) { <div class="error-state">{{ loadError() }} <button (click)="ngOnInit()">إعادة المحاولة</button></div> }
      @else {
      <div class="kpi-grid">
        <div class="kpi-card">
          <span class="kpi-title">المقاولون النشطون</span>
          <span class="kpi-value">{{ workforceService.contractors().length }}</span>
        </div>
        <div class="kpi-card">
          <span class="kpi-title">إجمالي العمالة المسجلة</span>
          <span class="kpi-value">{{ workforceService.workers().length }}</span>
        </div>
        <div class="kpi-card">
          <span class="kpi-title">طلبات العمالة النشطة</span>
          <span class="kpi-value">{{ workforceService.laborRequests().length }}</span>
        </div>
        <div class="kpi-card">
          <span class="kpi-title">السلف القائمة</span>
          <span class="kpi-value">{{ workforceService.advances().length }}</span>
        </div>
      </div>

      <div class="dashboard-grid">
        <section class="card">
          <h3>المقاولون ونماذج الحساب</h3>
          <table class="data-table">
            <thead>
              <tr>
                <th>كود المقاول</th>
                <th>اسم المقاول</th>
                <th>نموذج المحاسبة</th>
                <th>دورة التسوية</th>
                <th>الحالة</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let c of workforceService.contractors()">
                <td><strong>{{ c.code }}</strong></td>
                <td>{{ c.name }}</td>
                <td><span class="badge model-badge">{{ getModelLabel(c.accountingModel) }}</span></td>
                <td>كل {{ c.settlementCycleDays }} يوم</td>
                <td><span class="badge active">{{ c.status }}</span></td>
              </tr>
              <tr *ngIf="workforceService.contractors().length === 0">
                <td colspan="5" class="empty-cell">لا يوجد مقاولون مسجلون حالياً</td>
              </tr>
            </tbody>
          </table>
        </section>
      </div>
      }
    </div>
  `,
  styles: [`
    .workforce-container { padding: 1.5rem; display: flex; flex-direction: column; gap: 1.5rem; }
    .eyebrow { font-size: 0.875rem; color: #d97706; font-weight: 600; }
    .page-header h1 { font-size: 1.75rem; font-weight: 800; color: #0f172a; margin: 0.25rem 0 0 0; }
    .kpi-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 1rem; }
    .kpi-card { background: #fff; padding: 1.25rem; border-radius: 12px; border: 1px solid #e2e8f0; display: flex; flex-direction: column; gap: 0.5rem; }
    .kpi-title { font-size: 0.875rem; color: #64748b; }
    .kpi-value { font-size: 1.875rem; font-weight: 800; color: #0f172a; }
    .card { background: #fff; border-radius: 12px; border: 1px solid #e2e8f0; padding: 1.25rem; }
    .data-table { width: 100%; border-collapse: collapse; margin-top: 1rem; text-align: right; }
    .data-table th, .data-table td { padding: 0.75rem 1rem; border-bottom: 1px solid #e2e8f0; }
    .badge { padding: 0.25rem 0.625rem; border-radius: 6px; font-size: 0.75rem; font-weight: 600; }
    .badge.active { background: #dcfce7; color: #166534; }
    .badge.model-badge { background: #fef3c7; color: #92400e; }
    .empty-cell { text-align: center; color: #94a3b8; padding: 2rem; }
    .loading-state, .error-state { padding: 2rem; text-align: center; color: #64748b; } .error-state { color: #dc2626; }
  `]
})
export class WorkforceDashboardComponent implements OnInit {
  workforceService = inject(WorkforceService);
  loading = signal(true);
  loadError = signal<string | null>(null);

  ngOnInit() {
    this.loading.set(true);
    this.loadError.set(null);
    forkJoin({
      contractors: this.workforceService.loadContractors(),
      workers: this.workforceService.loadWorkers(),
      requests: this.workforceService.loadLaborRequests(),
      advances: this.workforceService.loadAdvances()
    }).subscribe({
      next: () => this.loading.set(false),
      error: (e) => {
        this.loadError.set('تعذّر تحميل البيانات: ' + (e?.error?.detail ?? e?.message ?? 'خطأ غير متوقع'));
        this.loading.set(false);
      }
    });
  }

  getModelLabel(model: string): string {
    switch (model) {
      case 'worker_net_total': return 'مجموع صافي العمال';
      case 'contractor_daily_rate': return 'سعر تعاقد مستقل';
      case 'worker_cost_plus_fee': return 'تكلفة العمال + عمولة';
      case 'fixed_period_amount': return 'مبلغ ثابت للفترة';
      default: return model;
    }
  }
}
