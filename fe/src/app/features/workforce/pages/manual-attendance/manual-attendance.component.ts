import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { WorkforceService } from '../../data-access/workforce.service';
import { NotificationService } from '../../../../core/notification.service';
import { Worker, AttendanceCell } from '../../models/workforce.models';

interface DayCell {
  attendanceValue: number; // 1, 0.5, 0
  overtimeHours: number;
  notes: string;
}

type AttendanceMatrix = { [workerId: string]: { [date: string]: DayCell } };

@Component({
  selector: 'app-manual-attendance',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="workforce-container">
      <header class="page-header">
        <div>
          <span class="eyebrow">مصفوفة الإدخال اليدوي</span>
          <h1>جدول تسجيل حضور العمالة — مصفوفة متعددة الأيام</h1>
        </div>
        <div class="header-actions">
          <button type="button" class="btn btn-secondary" (click)="applyFullDayAll()">
            تعيين يوم كامل للكل
          </button>
          <button type="button" class="btn btn-primary" [disabled]="saving()" (click)="saveAttendance()">
            {{ saving() ? 'جارٍ الحفظ...' : 'حفظ جميع التسجيلات' }}
          </button>
        </div>
      </header>

      <!-- Period Controls -->
      <div class="card controls-card">
        <div class="controls-row">
          <div class="control-group">
            <label>تاريخ البداية</label>
            <input type="date" [(ngModel)]="startDate" class="form-input" (change)="onPeriodChange()" />
          </div>
          <div class="control-group">
            <label>تاريخ النهاية</label>
            <input type="date" [(ngModel)]="endDate" class="form-input" (change)="onPeriodChange()" />
          </div>
          <div class="control-group presets">
            <label>اختصارات الفترة</label>
            <div class="preset-btns">
              <button type="button" class="btn btn-preset" (click)="setCurrentHalfMonth()">النصف الحالي</button>
              <button type="button" class="btn btn-preset" (click)="setLastHalfMonth()">النصف السابق</button>
              <button type="button" class="btn btn-preset" (click)="setCurrentMonth()">الشهر الكامل</button>
            </div>
          </div>
          <div class="control-group">
            <label>&nbsp;</label>
            <button type="button" class="btn btn-outline" (click)="loadData()">
              🔄 تحديث البيانات
            </button>
          </div>
        </div>
        <div class="period-summary" *ngIf="dates().length > 0">
          <span class="badge-info">
            📅 {{ dates().length }} يوم | من {{ startDate }} إلى {{ endDate }}
            | {{ workers().length }} عامل | إجمالي الخلايا: {{ dates().length * workers().length }}
          </span>
        </div>
      </div>

      <!-- Loading Skeleton -->
      @if (loading()) {
        <div class="card skeleton-card">
          <div class="skeleton-title"></div>
          @for (_ of [1,2,3,4,5]; track $index) {
            <div class="skeleton-row"></div>
          }
        </div>
      }

      <!-- Error State -->
      @else if (loadError()) {
        <div class="card error-card">
          <span>⚠️ {{ loadError() }}</span>
          <button type="button" class="btn btn-outline" (click)="loadData()">إعادة المحاولة</button>
        </div>
      }

      <!-- Empty Workers -->
      @else if (workers().length === 0) {
        <div class="card empty-card">
          <div class="empty-icon">👷</div>
          <p>لا يوجد عمال مسجلون. أضف عمالاً من قسم العمال أولاً.</p>
        </div>
      }

      <!-- Matrix Table -->
      @else {
        <div class="card matrix-card">
          <!-- Legend -->
          <div class="legend-row">
            <span class="legend-item full">■ يوم كامل (1)</span>
            <span class="legend-item half">■ نصف يوم (0.5)</span>
            <span class="legend-item absent">■ غياب (0)</span>
          </div>

          <div class="table-scroll-wrapper">
            <table class="matrix-table">
              <thead>
                <tr>
                  <th class="sticky-col worker-col">كود العامل</th>
                  <th class="sticky-col name-col">اسم العامل</th>
                  <th class="sticky-col rate-col">اليومية</th>
                  @for (date of dates(); track date) {
                    <th class="date-col" [class.weekend-col]="isWeekend(date)">
                      <div class="date-header">
                        <span class="day-name">{{ getDayName(date) }}</span>
                        <span class="day-num">{{ date.slice(-2) }}</span>
                      </div>
                    </th>
                  }
                  <th class="total-col">إجمالي أيام</th>
                  <th class="total-col">الإجمالي (ج.م)</th>
                </tr>
              </thead>
              <tbody>
                @for (w of workers(); track w.id) {
                  <tr>
                    <td class="sticky-col worker-col"><strong>{{ w.code }}</strong></td>
                    <td class="sticky-col name-col">{{ w.fullName }}<br><small class="contractor-name">{{ w.contractorName }}</small></td>
                    <td class="sticky-col rate-col">{{ w.defaultDailyRate | number:'1.0-0' }} ج.م</td>
                    @for (date of dates(); track date) {
                      <td class="cell-td" [class.weekend-cell]="isWeekend(date)">
                        <select
                          [(ngModel)]="matrix[w.id][date].attendanceValue"
                          [name]="'att_' + w.id + '_' + date"
                          class="cell-select"
                          [class.cell-full]="matrix[w.id][date].attendanceValue === 1"
                          [class.cell-half]="matrix[w.id][date].attendanceValue === 0.5"
                          [class.cell-absent]="matrix[w.id][date].attendanceValue === 0"
                        >
                          <option [ngValue]="1">1</option>
                          <option [ngValue]="0.5">½</option>
                          <option [ngValue]="0">—</option>
                        </select>
                      </td>
                    }
                    <td class="total-col total-days">
                      <strong>{{ getWorkerTotalDays(w.id) | number:'1.1-1' }}</strong>
                    </td>
                    <td class="total-col total-amount">
                      <strong>{{ getWorkerTotalAmount(w) | number:'1.0-0' }} ج.م</strong>
                    </td>
                  </tr>
                }
              </tbody>
              <tfoot>
                <tr class="totals-row">
                  <td class="sticky-col" colspan="3"><strong>الإجمالي اليومي</strong></td>
                  @for (date of dates(); track date) {
                    <td class="total-col">
                      <strong>{{ getDayTotal(date) | number:'1.1-1' }}</strong>
                    </td>
                  }
                  <td colspan="2" class="grand-total">
                    <strong>{{ grandTotal() | number:'1.0-0' }} ج.م</strong>
                  </td>
                </tr>
              </tfoot>
            </table>
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    .workforce-container { padding: 1.5rem; display: flex; flex-direction: column; gap: 1.5rem; direction: rtl; }
    .eyebrow { font-size: 0.875rem; color: #d97706; font-weight: 600; }
    .page-header { display: flex; justify-content: space-between; align-items: center; }
    .page-header h1 { font-size: 1.5rem; font-weight: 800; color: #0f172a; margin: 0.25rem 0 0 0; }
    .header-actions { display: flex; gap: 0.75rem; }
    .card { background: #fff; border-radius: 12px; border: 1px solid #e2e8f0; padding: 1.25rem; }
    .controls-card { display: flex; flex-direction: column; gap: 1rem; }
    .controls-row { display: flex; align-items: flex-end; gap: 1.25rem; flex-wrap: wrap; }
    .control-group { display: flex; flex-direction: column; gap: 0.375rem; }
    .control-group label { font-size: 0.8125rem; font-weight: 600; color: #64748b; }
    .form-input { padding: 0.5rem 0.75rem; border: 1px solid #cbd5e1; border-radius: 8px; font-size: 0.875rem; }
    .preset-btns { display: flex; gap: 0.5rem; }
    .btn { padding: 0.5rem 1rem; border-radius: 8px; font-weight: 600; cursor: pointer; border: none; font-size: 0.875rem; }
    .btn:disabled { opacity: 0.6; cursor: not-allowed; }
    .btn-primary { background: #d97706; color: #fff; }
    .btn-secondary { background: #e2e8f0; color: #334155; }
    .btn-preset { background: #f1f5f9; color: #475569; border: 1px solid #cbd5e1; padding: 0.375rem 0.75rem; }
    .btn-preset:hover { background: #e2e8f0; }
    .btn-outline { background: transparent; border: 1px solid #cbd5e1; color: #475569; }
    .period-summary { padding: 0.5rem 0 0 0; }
    .badge-info { background: #eff6ff; color: #1e40af; padding: 0.375rem 0.75rem; border-radius: 6px; font-size: 0.8125rem; font-weight: 500; }
    /* Skeleton */
    .skeleton-card { display: flex; flex-direction: column; gap: 0.75rem; }
    .skeleton-title { height: 28px; width: 40%; background: #f1f5f9; border-radius: 4px; }
    .skeleton-row { height: 48px; background: linear-gradient(90deg, #f1f5f9 25%, #e2e8f0 50%, #f1f5f9 75%); background-size: 200% 100%; border-radius: 6px; animation: shimmer 1.5s infinite; }
    @keyframes shimmer { 0% { background-position: 200% 0; } 100% { background-position: -200% 0; } }
    /* Error / Empty */
    .error-card { background: #fef2f2; border-color: #fecaca; color: #dc2626; display: flex; align-items: center; gap: 1rem; }
    .empty-card { text-align: center; padding: 3rem; color: #64748b; }
    .empty-icon { font-size: 3rem; margin-bottom: 0.75rem; }
    /* Legend */
    .legend-row { display: flex; gap: 1.25rem; margin-bottom: 0.75rem; }
    .legend-item { font-size: 0.8125rem; font-weight: 500; display: flex; align-items: center; gap: 0.375rem; }
    .legend-item.full { color: #15803d; }
    .legend-item.half { color: #b45309; }
    .legend-item.absent { color: #64748b; }
    /* Matrix Table */
    .matrix-card { padding: 0.75rem; }
    .table-scroll-wrapper { overflow-x: auto; max-height: 75vh; overflow-y: auto; }
    .matrix-table { width: 100%; border-collapse: collapse; text-align: center; font-size: 0.8125rem; }
    .matrix-table th { background: #f8fafc; padding: 0.5rem 0.375rem; border: 1px solid #e2e8f0; position: sticky; top: 0; z-index: 10; white-space: nowrap; }
    .matrix-table td { padding: 0.25rem 0.25rem; border: 1px solid #e2e8f0; }
    .sticky-col { position: sticky; background: #fff; z-index: 8; }
    .worker-col { right: 0; min-width: 80px; text-align: right; padding-right: 0.5rem; }
    .name-col { right: 80px; min-width: 150px; text-align: right; padding-right: 0.5rem; }
    .rate-col { right: 230px; min-width: 80px; }
    .matrix-table th.sticky-col { z-index: 12; }
    .date-col { min-width: 52px; }
    .date-header { display: flex; flex-direction: column; align-items: center; gap: 0.125rem; }
    .day-name { font-size: 0.6875rem; color: #94a3b8; }
    .day-num { font-size: 0.875rem; font-weight: 700; color: #0f172a; }
    .weekend-col { background: #fef9ec !important; }
    .weekend-cell { background: #fefce8; }
    .cell-td { padding: 0.125rem; }
    .cell-select { width: 48px; padding: 0.25rem; border-radius: 4px; border: 1px solid #e2e8f0; font-weight: 700; font-size: 0.8125rem; text-align: center; cursor: pointer; }
    .cell-full { background: #dcfce7; color: #15803d; border-color: #86efac; }
    .cell-half { background: #fef3c7; color: #b45309; border-color: #fde68a; }
    .cell-absent { background: #f1f5f9; color: #94a3b8; border-color: #e2e8f0; }
    .total-col { min-width: 80px; background: #f8fafc; font-size: 0.8125rem; }
    .total-days { color: #0f172a; }
    .total-amount { color: #1e40af; }
    .contractor-name { font-size: 0.75rem; color: #94a3b8; }
    .totals-row { background: #f1f5f9; font-weight: 700; }
    .grand-total { text-align: center; color: #1e40af; font-size: 0.9375rem; }
  `]
})
export class ManualAttendanceComponent implements OnInit {
  private workforceService = inject(WorkforceService);
  private notificationService = inject(NotificationService);

  workers = this.workforceService.workers;
  loading = signal(false);
  saving = signal(false);
  loadError = signal<string | null>(null);
  dates = signal<string[]>([]);

  matrix: AttendanceMatrix = {};

  startDate = '';
  endDate = '';

  grandTotal = computed(() => {
    let total = 0;
    for (const w of this.workers()) {
      total += this.getWorkerTotalAmount(w);
    }
    return total;
  });

  ngOnInit() {
    this.setCurrentHalfMonth();
    this.loadData();
  }

  setCurrentHalfMonth() {
    const today = new Date();
    const year = today.getFullYear();
    const month = today.getMonth();
    if (today.getDate() <= 15) {
      this.startDate = this.toIso(new Date(year, month, 1));
      this.endDate = this.toIso(new Date(year, month, 15));
    } else {
      const lastDay = new Date(year, month + 1, 0).getDate();
      this.startDate = this.toIso(new Date(year, month, 16));
      this.endDate = this.toIso(new Date(year, month, lastDay));
    }
    this.onPeriodChange();
  }

  setLastHalfMonth() {
    const today = new Date();
    const year = today.getFullYear();
    const month = today.getMonth();
    if (today.getDate() <= 15) {
      const lastDay = new Date(year, month, 0).getDate();
      this.startDate = this.toIso(new Date(year, month - 1, 16));
      this.endDate = this.toIso(new Date(year, month - 1, lastDay));
    } else {
      this.startDate = this.toIso(new Date(year, month, 1));
      this.endDate = this.toIso(new Date(year, month, 15));
    }
    this.onPeriodChange();
  }

  setCurrentMonth() {
    const today = new Date();
    const year = today.getFullYear();
    const month = today.getMonth();
    const lastDay = new Date(year, month + 1, 0).getDate();
    this.startDate = this.toIso(new Date(year, month, 1));
    this.endDate = this.toIso(new Date(year, month, lastDay));
    this.onPeriodChange();
  }

  onPeriodChange() {
    const newDates = this.generateDateRange(this.startDate, this.endDate);
    this.dates.set(newDates);
    // Re-initialize matrix for new dates while keeping existing values
    for (const w of this.workers()) {
      if (!this.matrix[w.id]) this.matrix[w.id] = {};
      for (const date of newDates) {
        if (!this.matrix[w.id][date]) {
          this.matrix[w.id][date] = { attendanceValue: 1, overtimeHours: 0, notes: '' };
        }
      }
    }
  }

  loadData() {
    if (!this.startDate || !this.endDate) return;
    this.loading.set(true);
    this.loadError.set(null);

    forkJoin({
      workers: this.workforceService.loadWorkers(),
    }).subscribe({
      next: ({ workers }) => {
        this.initMatrix(workers);
        this.loading.set(false);
      },
      error: (e) => {
        this.loadError.set('تعذّر تحميل بيانات العمال: ' + (e?.error?.detail ?? e?.message ?? 'خطأ غير متوقع'));
        this.loading.set(false);
      }
    });
  }

  private initMatrix(workers: Worker[]) {
    const dates = this.generateDateRange(this.startDate, this.endDate);
    this.dates.set(dates);
    for (const w of workers) {
      if (!this.matrix[w.id]) this.matrix[w.id] = {};
      for (const date of dates) {
        if (!this.matrix[w.id][date]) {
          this.matrix[w.id][date] = { attendanceValue: 1, overtimeHours: 0, notes: '' };
        }
      }
    }
  }

  applyFullDayAll() {
    for (const w of this.workers()) {
      for (const date of this.dates()) {
        if (this.matrix[w.id]?.[date]) {
          this.matrix[w.id][date].attendanceValue = 1;
        }
      }
    }
  }

  saveAttendance() {
    const entries: AttendanceCell[] = [];
    for (const w of this.workers()) {
      for (const date of this.dates()) {
        const cell = this.matrix[w.id]?.[date];
        if (cell) {
          entries.push({
            workerId: w.id,
            workDate: date,
            attendanceValue: cell.attendanceValue,
            overtimeHours: cell.overtimeHours || undefined,
            notes: cell.notes || undefined
          });
        }
      }
    }

    if (entries.length === 0) {
      this.notificationService.warning('لا توجد تسجيلات لحفظها');
      return;
    }

    this.saving.set(true);
    this.workforceService.saveAttendanceBatch(entries).subscribe({
      next: () => {
        this.saving.set(false);
        this.notificationService.success(`تم حفظ ${entries.length} تسجيل حضور بنجاح ✓`);
      },
      error: (e) => {
        this.saving.set(false);
        this.notificationService.error('حدث خطأ أثناء الحفظ: ' + (e?.error?.detail ?? e?.message ?? 'خطأ غير متوقع'));
      }
    });
  }

  // --- Calculation helpers ---
  getWorkerTotalDays(workerId: string): number {
    return this.dates().reduce((sum, date) => sum + (this.matrix[workerId]?.[date]?.attendanceValue ?? 0), 0);
  }

  getWorkerTotalAmount(worker: Worker): number {
    return this.getWorkerTotalDays(worker.id) * (worker.defaultDailyRate ?? 0);
  }

  getDayTotal(date: string): number {
    return this.workers().reduce((sum, w) => sum + (this.matrix[w.id]?.[date]?.attendanceValue ?? 0), 0);
  }

  // --- Date helpers ---
  private generateDateRange(start: string, end: string): string[] {
    if (!start || !end) return [];
    const dates: string[] = [];
    const cur = new Date(start + 'T00:00:00');
    const endDate = new Date(end + 'T00:00:00');
    while (cur <= endDate && dates.length <= 31) {
      dates.push(this.toIso(cur));
      cur.setDate(cur.getDate() + 1);
    }
    return dates;
  }

  isWeekend(date: string): boolean {
    const d = new Date(date + 'T00:00:00').getDay();
    return d === 5 || d === 6; // Fri/Sat
  }

  getDayName(date: string): string {
    const names = ['أحد', 'إثن', 'ثلا', 'أرب', 'خمس', 'جمع', 'سبت'];
    return names[new Date(date + 'T00:00:00').getDay()];
  }

  private toIso(d: Date): string {
    return d.toISOString().split('T')[0];
  }
}
