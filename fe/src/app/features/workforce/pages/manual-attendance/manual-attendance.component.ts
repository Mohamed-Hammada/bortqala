import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { WorkforceService } from '../../data-access/workforce.service';
import { NotificationService } from '../../../../core/notification.service';
import { exportCsv } from '../../../../core/download';
import { Worker, AttendanceCell } from '../../models/workforce.models';

interface DayCell {
  attendanceValue: number; // 1, 0.5, 0
  overtimeHours: number;
  deductionHours: number;
  notes: string;
}

type AttendanceMatrix = { [workerId: string]: { [date: string]: DayCell } };

interface CalculationRules {
  overtimeRate: number;
  overtimeThresholdHours: number;
  deductionRatePerHour: number;
  holidayPayRate: number;
  standardDailyHours: string;
  description: string;
}

type StatusOption<T = string> = { value: T; label: string };

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
          <button type="button" class="btn btn-secondary" (click)="exportCsv()">⇩ Excel</button>
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
            <label>المقاول</label>
            <select [(ngModel)]="selectedContractorId" (change)="onFilterChange()" class="form-input">
              <option value="">الكل</option>
              @for (c of contractors(); track c.id) {
                <option [value]="c.id">{{ c.name }}</option>
              }
            </select>
          </div>
          <div class="control-group">
            <label>فئة العامل</label>
            <select [(ngModel)]="selectedCategoryId" (change)="onFilterChange()" class="form-input">
              <option value="">الكل</option>
              @for (cat of categories(); track cat.id) {
                <option [value]="cat.id">{{ cat.name }}</option>
              }
            </select>
          </div>
          <div class="control-group">
            <label>بحث بالاسم/الكود</label>
            <input type="text" [(ngModel)]="searchText" (input)="onFilterChange()"
              placeholder="ابحث..." class="form-input search-input" />
          </div>
          <div class="control-group">
            <label>حالة الحضور</label>
              <select [(ngModel)]="selectedAttendanceStatus" (change)="onFilterChange()" class="form-input">
              @for (opt of attendanceStatusOptions; track opt.value) {
                <option [value]="opt.value">{{ opt.label }}</option>
              }
            </select>
          </div>
          <div class="control-group toggle-group">
            <label>&nbsp;</label>
            <label class="toggle-label">
              <input type="checkbox" [(ngModel)]="showInactive" (change)="onFilterChange()" />
              <span class="toggle-text">إظهار العمال غير النشطين</span>
            </label>
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
            | {{ filteredWorkers().length }} عامل | إجمالي الخلايا: {{ dates().length * filteredWorkers().length }}
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
      @else if (filteredWorkers().length === 0) {
        <div class="card empty-card">
          <div class="empty-icon">👷</div>
          <p>{{ selectedContractorId() || selectedCategoryId() ? 'لا يوجد عمال مطابقون للفلترة المحددة.' : 'لا يوجد عمال مسجلون. أضف عمالاً من قسم العمال أولاً.' }}</p>
        </div>
      }

      <!-- Calculation Rules Banner -->
      @if (rules(); as r) {
        <div class="card rules-card">
          <details>
            <summary class="rules-summary">📊 قواعد احتساب الحضور والانصراف</summary>
            <div class="rules-body">
              <div class="rule-item">
                <span class="rule-label">معدل الأوفرتايم:</span>
                <span class="rule-value">{{ r.overtimeRate }}× بعد {{ r.overtimeThresholdHours }} ساعات</span>
              </div>
              <div class="rule-item">
                <span class="rule-label">معدل خصم ساعة:</span>
                <span class="rule-value">الأجر اليومي ÷ {{ r.standardDailyHours }}</span>
              </div>
              <div class="rule-item">
                <span class="rule-label">معدل أجر العطلات:</span>
                <span class="rule-value">{{ r.holidayPayRate }}× الأجر العادي</span>
              </div>
              <p class="rules-desc">{{ r.description }}</p>
            </div>
          </details>
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

          <!-- Bulk Actions Bar -->
          @if (selectedWorkerIds().size > 0) {
            <div class="bulk-bar">
              <span class="bulk-count">{{ selectedWorkerIds().size }} عامل × {{ selectedDateIds().size }} يوم</span>
              <select [(ngModel)]="bulkStatusValue" class="form-input bulk-select">
                @for (opt of bulkStatusOptions; track opt.value) {
                  <option [ngValue]="opt.value">{{ opt.label }}</option>
                }
              </select>
              <button type="button" class="btn btn-primary btn-sm" (click)="previewBulk('attendance')">
                معاينة تطبيق الحضور
              </button>
              <input type="number" min="0" step="0.5" [(ngModel)]="bulkOvertimeHours" class="form-input bulk-select" aria-label="ساعات الأوفر تايم" />
              <button type="button" class="btn btn-primary btn-sm" (click)="previewBulk('overtime')">معاينة الأوفر تايم</button>
              <button type="button" class="btn btn-outline btn-sm" (click)="clearSelection()">
                إلغاء التحديد
              </button>
            </div>
            @if (bulkPreview(); as preview) {
              <div class="bulk-bar" role="alertdialog" aria-label="معاينة التعديل المجمع">
                <strong>سيتم تحديث {{ preview.workerCount }} عامل في {{ preview.dayCount }} يوم ({{ preview.cellCount }} خلية).</strong>
                <span>{{ preview.kind === 'attendance' ? 'قيمة الحضور: ' + bulkStatusValue() : 'ساعات الأوفر تايم: ' + bulkOvertimeHours() }}</span>
                <button type="button" class="btn btn-primary btn-sm" (click)="confirmBulk()">تأكيد التطبيق</button>
                <button type="button" class="btn btn-outline btn-sm" (click)="bulkPreview.set(null)">إلغاء</button>
              </div>
            }
          }

          <div class="table-scroll-wrapper">
            <table class="matrix-table">
              <thead>
                <tr>
                  <th class="sticky-col check-col">
                    <input type="checkbox"
                      [checked]="allSelected()"
                      (change)="toggleSelectAll()"
                      class="row-check" />
                  </th>
                  <th class="sticky-col worker-col">كود العامل</th>
                  <th class="sticky-col name-col">اسم العامل</th>
                  <th class="sticky-col rate-col">اليومية</th>
                  @for (date of dates(); track date) {
                    <th class="date-col" [class.weekend-col]="isWeekend(date)">
                      <div class="date-header">
                        <input type="checkbox" [checked]="selectedDateIds().has(date)" (change)="toggleDate(date)" [attr.aria-label]="'تحديد ' + date" />
                        <span class="day-name">{{ getDayName(date) }}</span>
                        <span class="day-num">{{ date.slice(-2) }}</span>
                      </div>
                    </th>
                  }
                  <th class="total-col">إجمالي أيام</th>
                  <th class="total-col">الإجمالي (ج.م)</th>
                  <th class="indicator-col">مؤشرات</th>
                </tr>
              </thead>
              <tbody>
                @for (w of filteredWorkers(); track w.id) {
                  <tr [class.selected-row]="isSelected(w.id)">
                    <td class="sticky-col check-col">
                      <input type="checkbox"
                        [checked]="isSelected(w.id)"
                        (change)="toggleWorker(w.id)"
                        class="row-check" />
                    </td>
                    <td class="sticky-col worker-col"><strong>{{ w.code }}</strong></td>
                    <td class="sticky-col name-col">
                      {{ w.fullName }}<br>
                      <small class="contractor-name">{{ w.contractorName }}</small>
                      @if (w.status === 'INACTIVE') {
                        <span class="badge-inactive">غير نشط</span>
                      }
                    </td>
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
                    <td class="indicator-col">
                      @if (hasOvertime(w.id)) {
                        <span class="indicator overtime-i" title="يوجد أوفرتايم">⏰</span>
                      }
                      @if (hasDeduction(w.id)) {
                        <span class="indicator deduction-i" title="يوجد خصم">⚠️</span>
                      }
                    </td>
                  </tr>
                }
              </tbody>
              <tfoot>
                <tr class="totals-row">
                  <td class="sticky-col" colspan="4"><strong>الإجمالي اليومي</strong></td>
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
    /* Search input */
    .search-input { min-width: 140px; }
    /* Toggle group */
    .toggle-group { justify-content: flex-end; }
    .toggle-label { display: flex; align-items: center; gap: 0.375rem; cursor: pointer; font-size: 0.8125rem; color: #334155; user-select: none; }
    .toggle-text { font-weight: 500; }
    /* Checkbox column */
    .check-col { position: sticky; background: #fff; z-index: 8; right: 310px; min-width: 36px; text-align: center; }
    .row-check { width: 16px; height: 16px; cursor: pointer; accent-color: #d97706; }
    .selected-row { background: #fefce8 !important; }
    /* Bulk bar */
    .bulk-bar { display: flex; align-items: center; gap: 0.75rem; padding: 0.625rem 0.75rem; margin-bottom: 0.625rem; background: #fffbeb; border: 1px solid #fde68a; border-radius: 8px; }
    .bulk-count { font-weight: 700; color: #b45309; font-size: 0.875rem; }
    .bulk-select { width: auto; padding: 0.3rem 0.5rem; }
    .btn-sm { padding: 0.3rem 0.75rem; font-size: 0.8125rem; }
    /* Indicator column */
    .indicator-col { min-width: 48px; background: #f8fafc; font-size: 1rem; text-align: center; }
    .indicator { cursor: help; display: inline-block; margin: 0 1px; }
    .badge-inactive { display: inline-block; background: #fef2f2; color: #dc2626; font-size: 0.6875rem; padding: 0.125rem 0.375rem; border-radius: 4px; margin-right: 0.375rem; font-weight: 600; }
    /* Calculation rules card */
    .rules-card { background: #f0fdf4; border-color: #bbf7d0; padding: 0.75rem 1rem; }
    .rules-summary { font-weight: 700; color: #166534; cursor: pointer; font-size: 0.875rem; }
    .rules-body { margin-top: 0.625rem; display: flex; flex-direction: column; gap: 0.375rem; }
    .rule-item { display: flex; gap: 0.5rem; font-size: 0.8125rem; }
    .rule-label { color: #475569; font-weight: 600; min-width: 110px; }
    .rule-value { color: #0f172a; font-weight: 500; }
    .rules-desc { font-size: 0.75rem; color: #64748b; margin-top: 0.375rem; font-style: italic; }
  `]
})
export class ManualAttendanceComponent implements OnInit {
  private workforceService = inject(WorkforceService);
  private notificationService = inject(NotificationService);
  private http = inject(HttpClient);

  workers = this.workforceService.workers;
  contractors = this.workforceService.contractors;
  categories = this.workforceService.categories;
  loading = signal(false);
  saving = signal(false);
  loadError = signal<string | null>(null);
  dates = signal<string[]>([]);

  selectedContractorId = signal<string>('');
  selectedCategoryId = signal<string>('');
  searchText = signal<string>('');
  selectedAttendanceStatus = signal<string>('all');
  showInactive = signal<boolean>(false);
  selectedWorkerIds = signal<Set<string>>(new Set());
  selectedDateIds = signal<Set<string>>(new Set());
  bulkStatusValue = signal<number>(1);
  bulkOvertimeHours = signal<number>(0);
  bulkPreview = signal<{ kind: 'attendance' | 'overtime'; workerCount: number; dayCount: number; cellCount: number } | null>(null);
  rules = signal<CalculationRules | null>(null);

  readonly attendanceStatusOptions: StatusOption[] = [
    { value: 'all', label: 'الكل' },
    { value: 'present', label: 'حاضر (1)' },
    { value: 'half', label: 'نصف يوم (½)' },
    { value: 'absent', label: 'غائب (—)' },
  ];

  readonly bulkStatusOptions: StatusOption<number>[] = [
    { value: 1, label: 'يوم كامل (1)' },
    { value: 0.5, label: 'نصف يوم (½)' },
    { value: 0, label: 'غائب (—)' },
  ];

  filteredWorkers = computed(() => {
    let list = this.workers();
    const contractorId = this.selectedContractorId();
    const categoryId = this.selectedCategoryId();
    const search = this.searchText().trim().toLowerCase();
    const statusFilter = this.selectedAttendanceStatus();
    const inactiveShown = this.showInactive();

    if (contractorId) {
      list = list.filter(w => w.contractorId === contractorId);
    }
    if (categoryId) {
      list = list.filter(w => w.categoryId === categoryId);
    }
    if (search) {
      list = list.filter(w =>
        w.fullName.toLowerCase().includes(search) ||
        w.code.toLowerCase().includes(search)
      );
    }
    if (!inactiveShown) {
      list = list.filter(w => w.status !== 'INACTIVE');
    }
    if (statusFilter !== 'all') {
      const targetValue = statusFilter === 'present' ? 1 : statusFilter === 'half' ? 0.5 : 0;
      list = list.filter(w => {
        const cells = this.matrix[w.id];
        if (!cells) return false;
        return Object.values(cells).some(c => c.attendanceValue === targetValue);
      });
    }
    return list;
  });

  matrix: AttendanceMatrix = {};

  startDate = '';
  endDate = '';

  allSelected = computed(() => {
    const fw = this.filteredWorkers();
    if (fw.length === 0) return false;
    return fw.every(w => this.selectedWorkerIds().has(w.id));
  });

  grandTotal = computed(() => {
    let total = 0;
    for (const w of this.filteredWorkers()) {
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
    const mStr = String(month + 1).padStart(2, '0');
    if (today.getDate() <= 15) {
      this.startDate = `${year}-${mStr}-01`;
      this.endDate = `${year}-${mStr}-15`;
    } else {
      const lastDay = new Date(year, month + 1, 0).getDate();
      this.startDate = `${year}-${mStr}-16`;
      this.endDate = `${year}-${mStr}-${String(lastDay).padStart(2, '0')}`;
    }
    this.onPeriodChange();
  }

  setLastHalfMonth() {
    const today = new Date();
    let year = today.getFullYear();
    let month = today.getMonth(); // 0-indexed
    if (today.getDate() <= 15) {
      month -= 1;
      if (month < 0) { month = 11; year -= 1; }
      const lastDay = new Date(year, month + 1, 0).getDate();
      const mStr = String(month + 1).padStart(2, '0');
      this.startDate = `${year}-${mStr}-16`;
      this.endDate = `${year}-${mStr}-${String(lastDay).padStart(2, '0')}`;
    } else {
      const mStr = String(month + 1).padStart(2, '0');
      this.startDate = `${year}-${mStr}-01`;
      this.endDate = `${year}-${mStr}-15`;
    }
    this.onPeriodChange();
  }

  setCurrentMonth() {
    const today = new Date();
    const year = today.getFullYear();
    const month = today.getMonth();
    const lastDay = new Date(year, month + 1, 0).getDate();
    const mStr = String(month + 1).padStart(2, '0');
    this.startDate = `${year}-${mStr}-01`;
    this.endDate = `${year}-${mStr}-${String(lastDay).padStart(2, '0')}`;
    this.onPeriodChange();
  }

  onFilterChange() {
    this.selectedWorkerIds.set(new Set());
  }

  onPeriodChange() {
    const newDates = this.generateDateRange(this.startDate, this.endDate);
    this.dates.set(newDates);
    this.selectedDateIds.set(new Set(newDates));
    for (const w of this.workers()) {
      if (!this.matrix[w.id]) this.matrix[w.id] = {};
      for (const date of newDates) {
        if (!this.matrix[w.id][date]) {
          this.matrix[w.id][date] = { attendanceValue: 1, overtimeHours: 0, deductionHours: 0, notes: '' };
        }
      }
    }
  }

  loadData() {
    if (!this.startDate || !this.endDate) return;
    this.loading.set(true);
    this.loadError.set(null);
    this.loadCalculationRules();

    forkJoin({
      contractors: this.workforceService.loadContractors(),
      categories: this.workforceService.loadCategories(),
      workers: this.workforceService.loadWorkers(),
    }).subscribe({
      next: ({ workers }) => {
        this.initMatrix(workers);
        this.selectedDateIds.set(new Set(this.dates()));
        this.loading.set(false);
      },
      error: (e) => {
        this.loadError.set('تعذّر تحميل بيانات العمال: ' + (e?.error?.detail ?? e?.message ?? 'خطأ غير متوقع'));
        this.loading.set(false);
      }
    });
  }

  private loadCalculationRules() {
    this.http.get<CalculationRules>('/api/v1/workforce/attendance/calculation-rules', {
      params: { date: this.startDate || '' }
    }).subscribe({
      next: (r) => this.rules.set(r),
      error: () => { /* non-critical */ }
    });
  }

  exportCsv(): void {
    const workers = this.filteredWorkers();
    const dateList = this.dates();
    const rows = workers.map((w) => {
      const row: Record<string, string | number | null | undefined> = {
        code: w.code,
        name: w.fullName,
        dailyRate: w.defaultDailyRate ?? 0,
        totalDays: this.getWorkerTotalDays(w.id),
        totalAmount: this.getWorkerTotalAmount(w),
      };
      for (const date of dateList) {
        row[date] = this.matrix[w.id]?.[date]?.attendanceValue ?? 0;
      }
      return row;
    });
    const columns = [
      { key: 'code', label: 'كود العامل' },
      { key: 'name', label: 'اسم العامل' },
      { key: 'dailyRate', label: 'اليومية' },
      ...dateList.map((d) => ({ key: d, label: d })),
      { key: 'totalDays', label: 'إجمالي أيام' },
      { key: 'totalAmount', label: 'الإجمالي (ج.م)' },
    ];
    exportCsv(rows, columns, `manual-attendance-${this.startDate}-${this.endDate}.csv`);
  }

  private initMatrix(workers: Worker[]) {
    const dates = this.generateDateRange(this.startDate, this.endDate);
    this.dates.set(dates);
    for (const w of workers) {
      if (!this.matrix[w.id]) this.matrix[w.id] = {};
      for (const date of dates) {
        if (!this.matrix[w.id][date]) {
          this.matrix[w.id][date] = { attendanceValue: 1, overtimeHours: 0, deductionHours: 0, notes: '' };
        }
      }
    }
  }

  applyFullDayAll() {
    for (const w of this.filteredWorkers()) {
      for (const date of this.dates()) {
        if (this.matrix[w.id]?.[date]) {
          this.matrix[w.id][date].attendanceValue = 1;
        }
      }
    }
  }

  // --- Bulk selection ---
  toggleSelectAll() {
    const current = this.selectedWorkerIds();
    if (this.allSelected()) {
      this.selectedWorkerIds.set(new Set());
    } else {
      this.selectedWorkerIds.set(new Set(this.filteredWorkers().map(w => w.id)));
    }
  }

  isSelected(workerId: string): boolean {
    return this.selectedWorkerIds().has(workerId);
  }

  clearSelection() {
    this.selectedWorkerIds.set(new Set());
    this.bulkPreview.set(null);
  }

  toggleDate(date: string): void {
    const next = new Set(this.selectedDateIds());
    if (next.has(date)) next.delete(date); else next.add(date);
    this.selectedDateIds.set(next);
    this.bulkPreview.set(null);
  }

  toggleWorker(workerId: string) {
    const next = new Set(this.selectedWorkerIds());
    if (next.has(workerId)) {
      next.delete(workerId);
    } else {
      next.add(workerId);
    }
    this.selectedWorkerIds.set(next);
  }

  previewBulk(kind: 'attendance' | 'overtime'): void {
    const ids = [...this.selectedWorkerIds()];
    const selectedDates = [...this.selectedDateIds()];
    if (ids.length === 0 || selectedDates.length === 0) {
      this.notificationService.warning('اختر عاملاً واحداً على الأقل');
      return;
    }
    if (kind === 'overtime' && this.bulkOvertimeHours() < 0) {
      this.notificationService.warning('ساعات الأوفر تايم لا يمكن أن تكون سالبة');
      return;
    }
    this.bulkPreview.set({ kind, workerCount: ids.length, dayCount: selectedDates.length, cellCount: ids.length * selectedDates.length });
  }

  confirmBulk(): void {
    const preview = this.bulkPreview();
    if (!preview) return;
    const ids = [...this.selectedWorkerIds()];
    const selectedDates = [...this.selectedDateIds()];
    const value = this.bulkStatusValue();
    for (const wId of ids) {
      for (const date of selectedDates) {
        if (this.matrix[wId]?.[date]) {
          if (preview.kind === 'attendance') this.matrix[wId][date].attendanceValue = value;
          else this.matrix[wId][date].overtimeHours = this.bulkOvertimeHours();
        }
      }
    }
    this.notificationService.success(`تم تطبيق التحديث على ${preview.cellCount} خلية`);
    this.selectedWorkerIds.set(new Set());
    this.bulkPreview.set(null);
  }

  saveAttendance() {
    const entries: AttendanceCell[] = [];
    for (const w of this.filteredWorkers()) {
      for (const date of this.dates()) {
        const cell = this.matrix[w.id]?.[date];
        if (cell) {
          entries.push({
            workerId: w.id,
            workDate: date,
            attendanceValue: cell.attendanceValue,
            overtimeHours: cell.overtimeHours || undefined,
            deductionHours: cell.deductionHours || undefined,
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
    return this.filteredWorkers().reduce((sum, w) => sum + (this.matrix[w.id]?.[date]?.attendanceValue ?? 0), 0);
  }

  getStatusLabel(value: number): string {
    if (value === 1) return 'حاضر';
    if (value === 0.5) return 'نصف يوم';
    return 'غائب';
  }

  getStatusClass(value: number): string {
    if (value === 1) return 'status-present';
    if (value === 0.5) return 'status-half';
    return 'status-absent';
  }

  hasOvertime(workerId: string): boolean {
    return this.dates().some(date =>
      (this.matrix[workerId]?.[date]?.overtimeHours ?? 0) > 0
    );
  }

  hasDeduction(workerId: string): boolean {
    return this.dates().some(date =>
      (this.matrix[workerId]?.[date]?.deductionHours ?? 0) > 0
    );
  }

  // --- Date helpers ---
  private generateDateRange(start: string, end: string): string[] {
    if (!start || !end) return [];
    const dates: string[] = [];
    const [sY, sM, sD] = start.split('-').map(Number);
    const [eY, eM, eD] = end.split('-').map(Number);
    const cur = new Date(sY, sM - 1, sD, 12, 0, 0);
    const endDate = new Date(eY, eM - 1, eD, 12, 0, 0);
    while (cur <= endDate && dates.length <= 31) {
      dates.push(this.toIso(cur));
      cur.setDate(cur.getDate() + 1);
    }
    return dates;
  }

  isWeekend(dateStr: string): boolean {
    const [y, m, d] = dateStr.split('-').map(Number);
    const day = new Date(y, m - 1, d, 12, 0, 0).getDay();
    return day === 5 || day === 6;
  }

  getDayName(dateStr: string): string {
    const names = ['أحد', 'إثن', 'ثلا', 'أرب', 'خمس', 'جمع', 'سبت'];
    const [y, m, d] = dateStr.split('-').map(Number);
    return names[new Date(y, m - 1, d, 12, 0, 0).getDay()];
  }

  private toIso(d: Date): string {
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
  }
}
