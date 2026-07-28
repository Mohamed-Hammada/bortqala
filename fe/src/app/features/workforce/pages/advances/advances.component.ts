import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { WorkforceService } from '../../data-access/workforce.service';
import { WorkforceAdvance } from '../../models/workforce.models';
import { NotificationService } from '../../../../core/notification.service';
import { ModalDialogComponent } from '../../../../shared/ui/modal-dialog/modal-dialog.component';

@Component({
  selector: 'app-advances',
  standalone: true,
  imports: [CommonModule, FormsModule, ModalDialogComponent],
  template: `
    <div class="workforce-container">
      <header class="page-header">
        <div>
          <span class="eyebrow">السلف والأقساط</span>
          <h1>إدارة سُلف العمال والمقاولين وجدولة الأقساط</h1>
        </div>
        <button type="button" class="btn btn-primary" (click)="openCreateModal()">
          + صرف سُلفة جديدة
        </button>
      </header>

      <!-- Summary Stats -->
      <div class="stats-row">
        <div class="stat-card">
          <span class="stat-label">إجمالي السلف القائمة</span>
          <span class="stat-value">{{ workforceService.advances().length }}</span>
        </div>
        <div class="stat-card">
          <span class="stat-label">إجمالي المبالغ الممنوحة</span>
          <span class="stat-value amount-val">{{ totalGranted() | number:'1.0-0' }} ج.م</span>
        </div>
        <div class="stat-card">
          <span class="stat-label">إجمالي الأرصدة المتبقية</span>
          <span class="stat-value balance-val">{{ totalRemaining() | number:'1.0-0' }} ج.م</span>
        </div>
      </div>

      <!-- Loading -->
      @if (loading()) {
        <div class="card">
          @for (_ of [1,2,3]; track $index) {
            <div class="skeleton-row"></div>
          }
        </div>
      }

      <!-- Table -->
      @else {
        <div class="card">
          <table class="data-table">
            <thead>
              <tr>
                <th>الجهة المستفيدة</th>
                <th>النوع</th>
                <th>المبلغ الكلي</th>
                <th>نوع السلفة</th>
                <th>الأقساط</th>
                <th>قيمة القسط</th>
                <th>الرصيد المتبقي</th>
                <th>دورية الخصم</th>
                <th>أقصى خصم %</th>
                <th>الحالة</th>
                <th>تاريخ الإنشاء</th>
              </tr>
            </thead>
            <tbody>
              @for (adv of workforceService.advances(); track adv.id) {
                <tr>
                  <td><strong>{{ adv.recipientType === 'WORKER' ? adv.workerName : adv.contractorName }}</strong></td>
                  <td><span class="badge type-badge">{{ adv.recipientType === 'WORKER' ? '👷 عامل' : '🏗️ مقاول' }}</span></td>
                  <td>{{ adv.amount | number:'1.2-2' }} ج.م</td>
                  <td><span class="badge term-badge" [class.long-term]="adv.termType === 'LONG_TERM'">{{ getTermLabel(adv.termType) }}</span></td>
                  <td>{{ adv.totalInstallments }} قسط</td>
                  <td>{{ adv.installmentAmount | number:'1.2-2' }} ج.م</td>
                  <td><strong class="rem-bal">{{ adv.remainingBalance | number:'1.2-2' }} ج.م</strong></td>
                  <td>{{ getFrequencyLabel(adv.deductionFrequency) }}</td>
                  <td>{{ adv.maxDeductionPercent }} %</td>
                  <td>
                    <span class="badge" [class.status-active]="adv.status === 'ACTIVE'"
                          [class.status-paid]="adv.status === 'PAID_OFF'"
                          [class.status-suspended]="adv.status === 'PAUSED' || adv.status === 'SUSPENDED'">
                      {{ getStatusLabel(adv.status) }}
                    </span>
                  </td>
                  <td>
                    <div style="display: flex; gap: 0.25rem;">
                      @if (adv.status === 'ACTIVE') {
                        <button type="button" class="btn btn-secondary btn-sm" style="padding: 2px 6px; font-size: 0.75rem;" (click)="pauseAdvance(adv)">⏸️ إيقاف</button>
                        <button type="button" class="btn btn-secondary btn-sm" style="padding: 2px 6px; font-size: 0.75rem;" (click)="repayAdvance(adv)">💵 سداد</button>
                      }
                      @if (adv.status === 'PAUSED' || adv.status === 'SUSPENDED') {
                        <button type="button" class="btn btn-primary btn-sm" style="padding: 2px 6px; font-size: 0.75rem;" (click)="resumeAdvance(adv)">▶️ استئناف</button>
                      }
                    </div>
                  </td>
                </tr>
              }
              @if (workforceService.advances().length === 0) {
                <tr><td colspan="11" class="empty-cell">لا توجد سلف مسجّلة حتى الآن</td></tr>
              }
            </tbody>
          </table>
        </div>
      }

      <!-- Create Modal -->
      <app-modal-dialog
        [isOpen]="isModalOpen"
        title="طلب صرف سُلفة جديدة"
        size="wide"
        [preventOutsideClose]="true"
        (close)="isModalOpen = false">

        <form (ngSubmit)="saveAdvance()" class="modal-form">
          <div class="form-grid">

            <!-- Recipient Type -->
            <div class="form-group">
              <label>نوع المستفيد *</label>
              <select [(ngModel)]="form.recipientType" name="recipientType" class="form-input"
                      (ngModelChange)="onRecipientTypeChange()">
                <option value="WORKER">عامل</option>
                <option value="CONTRACTOR">مقاول</option>
              </select>
            </div>

            <!-- Worker / Contractor selector -->
            @if (form.recipientType === 'WORKER') {
              <div class="form-group">
                <label>اختر العامل *</label>
                <select [(ngModel)]="form.workerId" name="workerId" class="form-input">
                  @for (w of workforceService.workers(); track w.id) {
                    <option [value]="w.id">{{ w.fullName }} ({{ w.code }})</option>
                  }
                </select>
              </div>
            } @else {
              <div class="form-group">
                <label>اختر المقاول *</label>
                <select [(ngModel)]="form.contractorId" name="contractorId" class="form-input">
                  @for (c of workforceService.contractors(); track c.id) {
                    <option [value]="c.id">{{ c.name }} ({{ c.code }})</option>
                  }
                </select>
              </div>
            }

            <!-- Amount -->
            <div class="form-group">
              <label>مبلغ السُلفة (ج.م) *</label>
              <input type="number" [(ngModel)]="form.amount" name="amount" required
                     class="form-input" min="1" (ngModelChange)="recalcInstallment()" />
            </div>

            <!-- Term Type -->
            <div class="form-group">
              <label>نوع السلفة</label>
              <select [(ngModel)]="form.termType" name="termType" class="form-input">
                <option value="SHORT_TERM">قصيرة الأجل — خصم مباشر دفعة واحدة</option>
                <option value="LONG_TERM">طويلة الأجل — تقسيط متكرر</option>
              </select>
            </div>

            <!-- Installments count (only for LONG_TERM) -->
            @if (form.termType === 'LONG_TERM') {
              <div class="form-group">
                <label>عدد الأقساط *</label>
                <input type="number" [(ngModel)]="form.totalInstallments" name="totalInstallments"
                       class="form-input" min="2" max="60" (ngModelChange)="recalcInstallment()" />
              </div>

              <div class="form-group">
                <label>قيمة القسط (ج.م) — محسوبة تلقائياً</label>
                <input type="number" [(ngModel)]="form.installmentAmount" name="installmentAmount"
                       class="form-input" min="1" />
              </div>

              <div class="form-group">
                <label>تاريخ أول قسط</label>
                <input type="date" [(ngModel)]="form.firstInstallmentDate" name="firstInstallmentDate"
                       class="form-input" />
              </div>

              <div class="form-group">
                <label>دورية الخصم</label>
                <select [(ngModel)]="form.deductionFrequency" name="deductionFrequency" class="form-input">
                  <option value="HALF_MONTH">نصف شهري (كل 15 يوم)</option>
                  <option value="MONTHLY">شهري</option>
                  <option value="WEEKLY">أسبوعي</option>
                </select>
              </div>

              <div class="form-group">
                <label>نوع التشغيل</label>
                <select [(ngModel)]="form.deductionMode" name="deductionMode" class="form-input">
                  <option value="AUTO">تلقائي — يُخصم مع كل تسوية</option>
                  <option value="MANUAL">يدوي — يتطلب تدخل يدوي</option>
                </select>
              </div>

              <div class="form-group">
                <label>تأجيل بداية الخصم (عدد الفترات)</label>
                <input type="number" [(ngModel)]="form.deferralPeriods" name="deferralPeriods"
                       class="form-input" min="0" max="12" />
              </div>
            }

            <!-- Max deduction % -->
            <div class="form-group">
              <label>الحد الأقصى للخصم من مستحق الفترة (%)</label>
              <input type="number" [(ngModel)]="form.maxDeductionPercent" name="maxDeductionPercent"
                     class="form-input" min="10" max="100" />
            </div>

            <!-- Reason -->
            <div class="form-group col-span-2">
              <label>سبب السلفة والملاحظات</label>
              <input type="text" [(ngModel)]="form.reason" name="reason" class="form-input"
                     placeholder="مثل: ظروف طارئة، احتياج شخصي، ..." />
            </div>

            <!-- Summary for long term -->
            @if (form.termType === 'LONG_TERM' && form.totalInstallments > 0) {
              <div class="summary-box col-span-2">
                <strong>ملخص السلفة:</strong> {{ form.amount | number:'1.0-0' }} ج.م ÷
                {{ form.totalInstallments }} قسط = {{ form.installmentAmount | number:'1.2-2' }} ج.م / قسط
                @if (form.firstInstallmentDate) {
                  | أول قسط: {{ form.firstInstallmentDate }}
                }
              </div>
            }
          </div>
        </form>

        <div modal-actions class="modal-actions-bar">
          <button type="button" class="btn btn-primary" [disabled]="saving()" (click)="saveAdvance()">
            {{ saving() ? 'جارٍ الحفظ...' : 'اعتماد وصرف السلفة' }}
          </button>
          <button type="button" class="btn btn-secondary" (click)="isModalOpen = false">إلغاء</button>
        </div>
      </app-modal-dialog>
    </div>
  `,
  styles: [`
    .workforce-container { padding: 1.5rem; display: flex; flex-direction: column; gap: 1.5rem; direction: rtl; }
    .eyebrow { font-size: 0.875rem; color: #d97706; font-weight: 600; }
    .page-header { display: flex; justify-content: space-between; align-items: center; }
    .page-header h1 { font-size: 1.5rem; font-weight: 800; color: #0f172a; margin: 0.25rem 0 0 0; }
    .stats-row { display: grid; grid-template-columns: repeat(3, 1fr); gap: 1rem; }
    .stat-card { background: #fff; border-radius: 12px; border: 1px solid #e2e8f0; padding: 1rem 1.25rem; display: flex; flex-direction: column; gap: 0.375rem; }
    .stat-label { font-size: 0.8125rem; color: #64748b; }
    .stat-value { font-size: 1.5rem; font-weight: 800; color: #0f172a; }
    .amount-val { color: #1d4ed8; }
    .balance-val { color: #dc2626; }
    .card { background: #fff; border-radius: 12px; border: 1px solid #e2e8f0; padding: 1.25rem; }
    .skeleton-row { height: 48px; background: linear-gradient(90deg, #f1f5f9 25%, #e2e8f0 50%, #f1f5f9 75%); background-size: 200% 100%; border-radius: 6px; animation: shimmer 1.5s infinite; margin-bottom: 0.5rem; }
    @keyframes shimmer { 0% { background-position: 200% 0; } 100% { background-position: -200% 0; } }
    .data-table { width: 100%; border-collapse: collapse; text-align: right; }
    .data-table th, .data-table td { padding: 0.625rem 0.75rem; border-bottom: 1px solid #e2e8f0; font-size: 0.875rem; }
    .data-table th { background: #f8fafc; font-weight: 700; color: #475569; }
    .btn { padding: 0.625rem 1.25rem; border-radius: 8px; font-weight: 600; cursor: pointer; border: none; }
    .btn:disabled { opacity: 0.6; cursor: not-allowed; }
    .btn-primary { background: #d97706; color: #fff; }
    .btn-secondary { background: #e2e8f0; color: #334155; }
    .badge { padding: 0.25rem 0.625rem; border-radius: 6px; font-size: 0.75rem; font-weight: 600; }
    .badge.type-badge { background: #f1f5f9; color: #475569; }
    .badge.term-badge { background: #e0e7ff; color: #3730a3; }
    .badge.term-badge.long-term { background: #fef3c7; color: #92400e; }
    .badge.status-active { background: #dcfce7; color: #166534; }
    .badge.status-paid { background: #f0fdf4; color: #15803d; }
    .badge.status-suspended { background: #fef3c7; color: #92400e; }
    .rem-bal { color: #dc2626; }
    .empty-cell { text-align: center; color: #94a3b8; padding: 2rem; }
    .form-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 1rem; }
    .col-span-2 { grid-column: span 2; }
    .form-group { display: flex; flex-direction: column; gap: 0.375rem; }
    .form-group label { font-weight: 600; font-size: 0.8125rem; color: #334155; }
    .form-input { padding: 0.625rem; border: 1px solid #cbd5e1; border-radius: 8px; font-size: 0.875rem; }
    .summary-box { background: #eff6ff; border: 1px solid #bfdbfe; border-radius: 8px; padding: 0.75rem 1rem; font-size: 0.875rem; color: #1e40af; }
    .modal-actions-bar { width: 100%; display: flex; gap: 0.75rem; justify-content: flex-start; }
  `]
})
export class AdvancesComponent implements OnInit {
  workforceService = inject(WorkforceService);
  private notificationService = inject(NotificationService);

  loading = signal(false);
  saving = signal(false);
  isModalOpen = false;

  form: {
    recipientType: string; workerId: string; contractorId: string;
    amount: number; termType: string; totalInstallments: number;
    installmentAmount: number; deductionFrequency: string;
    maxDeductionPercent: number; reason: string;
    firstInstallmentDate: string; deductionMode: string; deferralPeriods: number;
  } = this.defaultForm();

  totalGranted = () => this.workforceService.advances().reduce((s, a) => s + (a.amount ?? 0), 0);
  totalRemaining = () => this.workforceService.advances().reduce((s, a) => s + (a.remainingBalance ?? 0), 0);

  ngOnInit() {
    this.loading.set(true);
    this.workforceService.loadAdvances().subscribe({
      next: () => this.loading.set(false),
      error: () => this.loading.set(false)
    });
    this.workforceService.loadWorkers().subscribe();
    this.workforceService.loadContractors().subscribe();
  }

  openCreateModal() {
    this.form = this.defaultForm();
    const workers = this.workforceService.workers();
    if (workers.length > 0) this.form.workerId = workers[0].id;
    this.isModalOpen = true;
  }

  onRecipientTypeChange() {
    this.form.workerId = '';
    this.form.contractorId = '';
    const workers = this.workforceService.workers();
    const contractors = this.workforceService.contractors();
    if (this.form.recipientType === 'WORKER' && workers.length > 0) this.form.workerId = workers[0].id;
    if (this.form.recipientType === 'CONTRACTOR' && contractors.length > 0) this.form.contractorId = contractors[0].id;
  }

  recalcInstallment() {
    if (this.form.totalInstallments > 0 && this.form.amount > 0) {
      this.form.installmentAmount = Math.round((this.form.amount / this.form.totalInstallments) * 100) / 100;
    }
  }

  saveAdvance() {
    if (!this.form.amount || this.form.amount <= 0) {
      this.notificationService.warning('يجب إدخال مبلغ السلفة');
      return;
    }
    if (this.form.termType === 'LONG_TERM' && this.form.totalInstallments < 2) {
      this.notificationService.warning('السلفة طويلة الأجل تتطلب 2 قسط على الأقل');
      return;
    }
    this.saving.set(true);
    this.workforceService.createAdvance(this.form).subscribe({
      next: () => {
        this.saving.set(false);
        this.isModalOpen = false;
        this.notificationService.success('تم صرف السلفة بنجاح ✓');
      },
      error: (e) => {
        this.saving.set(false);
        const msg = e?.error?.detail ?? e?.error?.message ?? e?.message ?? 'خطأ غير متوقع';
        this.notificationService.error('فشل حفظ السلفة: ' + msg);
      }
    });
  }

  pauseAdvance(adv: WorkforceAdvance) {
    if (!confirm(`هل أنت متأكد من إيقاف اقتطاع السلفة الخاصة بـ (${adv.recipientType === 'WORKER' ? adv.workerName : adv.contractorName})؟`)) return;
    this.workforceService.pauseAdvance(adv.id).subscribe({
      next: () => {
        this.notificationService.success('تم إيقاف خصم السلفة مؤقتاً ✓');
        this.workforceService.loadAdvances().subscribe();
      },
      error: (e) => this.notificationService.error('فشل إيقاف السلفة: ' + (e?.error?.message ?? e?.message))
    });
  }

  resumeAdvance(adv: WorkforceAdvance) {
    if (!confirm(`هل أنت متأكد من استئناف اقتطاع السلفة؟`)) return;
    this.workforceService.resumeAdvance(adv.id).subscribe({
      next: () => {
        this.notificationService.success('تم استئناف خصم السلفة بنجاح ✓');
        this.workforceService.loadAdvances().subscribe();
      },
      error: (e) => this.notificationService.error('فشل استئناف السلفة: ' + (e?.error?.message ?? e?.message))
    });
  }

  repayAdvance(adv: WorkforceAdvance) {
    const valStr = prompt(`أدخل مبلغ السداد المبكر (الرصيد المتبقي الحالي: ${adv.remainingBalance} ج.م):`, String(adv.installmentAmount));
    if (!valStr) return;
    const val = parseFloat(valStr);
    if (isNaN(val) || val <= 0) {
      this.notificationService.warning('يرجى إدخال مبلغ سداد صحيح');
      return;
    }
    this.workforceService.repayAdvance(adv.id, val).subscribe({
      next: () => {
        this.notificationService.success(`تم إدخال السداد المبكر بمبلغ ${val} ج.م بنجاح ✓`);
        this.workforceService.loadAdvances().subscribe();
      },
      error: (e) => this.notificationService.error('فشل السداد: ' + (e?.error?.message ?? e?.message))
    });
  }

  // --- Labels ---
  getTermLabel(term: string): string {
    return term === 'SHORT_TERM' ? 'قصيرة الأجل' : 'طويلة الأجل';
  }

  getFrequencyLabel(freq: string): string {
    const m: Record<string, string> = {
      HALF_MONTH: 'نصف شهري', MONTHLY: 'شهري', WEEKLY: 'أسبوعي'
    };
    return m[freq] ?? freq;
  }

  getStatusLabel(status: string): string {
    const m: Record<string, string> = { ACTIVE: 'نشطة', PAID_OFF: 'مسدّدة', SUSPENDED: 'موقوفة' };
    return m[status] ?? status;
  }

  private defaultForm() {
    return {
      recipientType: 'WORKER', workerId: '', contractorId: '',
      amount: 1000, termType: 'SHORT_TERM', totalInstallments: 1,
      installmentAmount: 1000, deductionFrequency: 'HALF_MONTH',
      maxDeductionPercent: 50, reason: '',
      firstInstallmentDate: '', deductionMode: 'AUTO', deferralPeriods: 0
    };
  }
}
