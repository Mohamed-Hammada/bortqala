import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { WorkforceService } from '../../data-access/workforce.service';
import { WorkforceAdvance, AdvanceRepayRequest, AdvancePolicy } from '../../models/workforce.models';
import { NotificationService } from '../../../../core/notification.service';
import { exportCsv } from '../../../../core/download';
import { ModalDialogComponent } from '../../../../shared/ui/modal-dialog/modal-dialog.component';
import { AppTooltipDirective } from '../../../../shared/ui/app-tooltip/app-tooltip.directive';

@Component({
  selector: 'app-advances',
  standalone: true,
  imports: [CommonModule, FormsModule, ModalDialogComponent, AppTooltipDirective],
  template: `
    <div class="workforce-container">
      <header class="page-header">
        <div>
          <span class="eyebrow">السلف والأقساط</span>
          <h1>إدارة سُلف العمال والمقاولين وجدولة الأقساط</h1>
        </div>
        <div style="display: flex; gap: 0.75rem;">
          <button type="button" class="btn btn-secondary" (click)="openPolicyModal()">⚙ سياسة الخصم</button>
          <button type="button" class="btn btn-secondary" (click)="exportCsv()">⇩ Excel</button>
          <button type="button" class="btn btn-primary" (click)="openCreateModal()">
            + صرف سُلفة جديدة
          </button>
        </div>
      </header>

      <div class="card policy-summary-card">
        <div><strong>سياسة السلف الافتراضية والاستثناءات</strong><p>الأولوية: استثناء العامل ← استثناء الفئة ← الإعداد العام. ويمكن تجاوزها داخل السلفة نفسها.</p></div>
        <div class="policy-chips">@for (policy of workforceService.advancePolicies(); track policy.id) {<span class="badge policy-chip">{{ policy.scopeName || getPolicyScopeLabel(policy) }} · {{ policy.deductionMode === 'AUTO' ? 'تلقائي' : 'يدوي' }} · {{ policy.maxDeductionPercent }}%</span>}</div>
      </div>

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
                <select [(ngModel)]="form.workerId" name="workerId" class="form-input" (ngModelChange)="applyAdvancePolicy()">
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
                <label>قيمة القسط (ج.م) — محسوبة تلقائياً <span tabindex="0" aria-label="شرح احتساب الأقساط" appTooltip="طريقة احتساب الأقساط — مبلغ السلفة ÷ عدد الأقساط ويمكن تعديله">ⓘ</span></label>
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

      <app-modal-dialog [isOpen]="policyModalOpen()" title="سياسة خصم السلف" size="normal" [preventOutsideClose]="true" (close)="policyModalOpen.set(false)">
        <form class="modal-form">
          <div class="form-grid">
            <div class="form-group"><label>نطاق السياسة *</label><select [(ngModel)]="policyForm.scopeType" name="policyScope" class="form-input" (ngModelChange)="policyForm.scopeId = ''"><option value="GLOBAL">إعداد عام</option><option value="CATEGORY">استثناء حسب الفئة</option><option value="WORKER">استثناء حسب العامل</option></select></div>
            @if (policyForm.scopeType === 'CATEGORY') {<div class="form-group"><label>الفئة *</label><select [(ngModel)]="policyForm.scopeId" name="policyCategory" class="form-input">@for (category of workforceService.categories(); track category.id) {<option [value]="category.id">{{ category.name }}</option>}</select></div>}
            @if (policyForm.scopeType === 'WORKER') {<div class="form-group"><label>العامل *</label><select [(ngModel)]="policyForm.scopeId" name="policyWorker" class="form-input">@for (worker of workforceService.workers(); track worker.id) {<option [value]="worker.id">{{ worker.fullName }} ({{ worker.code }})</option>}</select></div>}
            <div class="form-group"><label>طريقة الخصم الافتراضية</label><select [(ngModel)]="policyForm.deductionMode" name="policyMode" class="form-input"><option value="AUTO">تلقائي مع التسوية</option><option value="MANUAL">يدوي</option></select></div>
            <div class="form-group"><label>دورية الخصم</label><select [(ngModel)]="policyForm.deductionFrequency" name="policyFrequency" class="form-input"><option value="HALF_MONTH">نصف شهري</option><option value="MONTHLY">شهري</option><option value="WEEKLY">أسبوعي</option></select></div>
            <div class="form-group"><label>الحد الأقصى من مستحق الفترة %</label><input type="number" min="1" max="100" [(ngModel)]="policyForm.maxDeductionPercent" name="policyMax" class="form-input" /></div>
            <div class="form-group"><label>عدد الأقساط الافتراضي</label><input type="number" min="1" max="60" [(ngModel)]="policyForm.defaultInstallments" name="policyInstallments" class="form-input" /></div>
            <div class="form-group"><label>فترات التأجيل الافتراضية</label><input type="number" min="0" max="12" [(ngModel)]="policyForm.deferralPeriods" name="policyDeferral" class="form-input" /></div>
            <label class="form-group"><span>الحالة</span><input type="checkbox" [(ngModel)]="policyForm.active" name="policyActive" /> مفعّلة</label>
          </div>
        </form>
        <div modal-actions class="modal-actions-bar"><button type="button" class="btn btn-primary" [disabled]="saving()" (click)="savePolicy()">حفظ السياسة</button><button type="button" class="btn btn-secondary" (click)="policyModalOpen.set(false)">إلغاء</button></div>
      </app-modal-dialog>

      <!-- Repayment Modal -->
      <app-modal-dialog
        [isOpen]="repayModalOpen()"
        title="سداد سلفة مبكر"
        size="normal"
        [preventOutsideClose]="true"
        (close)="closeRepayModal()">

        @if (repayTarget(); as adv) {
          <div class="repay-container">

            <!-- Recipient Info -->
            <div class="repay-info-grid">
              <div class="repay-info-item">
                <span class="info-label">المستفيد</span>
                <span class="info-value">{{ adv.recipientType === 'WORKER' ? adv.workerName : adv.contractorName }}</span>
              </div>
              <div class="repay-info-item">
                <span class="info-label">النوع</span>
                <span class="info-value">{{ adv.recipientType === 'WORKER' ? 'عامل' : 'مقاول' }}</span>
              </div>
              <div class="repay-info-item">
                <span class="info-label">المبلغ الأصلي <span tabindex="0" aria-label="شرح المبلغ الأصلي" appTooltip="المبلغ الأصلي — كامل قيمة السلفة عند الصرف">ⓘ</span></span>
                <span class="info-value">{{ adv.amount | number:'1.0-0' }} ج.م</span>
              </div>
              <div class="repay-info-item">
                <span class="info-label">المسدّد سابقاً</span>
                <span class="info-value">{{ (adv.amount - adv.remainingBalance) | number:'1.0-0' }} ج.م</span>
              </div>
              <div class="repay-info-item">
                <span class="info-label">الرصيد المتبقي <span tabindex="0" aria-label="شرح الرصيد المتبقي" appTooltip="الرصيد المتبقي — المبلغ الأصلي ناقص كل السداد والخصومات المسجلة">ⓘ</span></span>
                <span class="info-value balance-highlight">{{ adv.remainingBalance | number:'1.0-0' }} ج.م</span>
              </div>
            </div>

            <!-- Repayment Type -->
            <div class="repay-type-group">
              <label class="repay-type-option">
                <input type="radio" [(ngModel)]="repayForm.repaymentType" name="repayType"
                       value="FULL" (change)="onRepayTypeChange()" />
                <span>سداد كامل — سيتم إغلاق السلفة بالكامل</span>
              </label>
              <label class="repay-type-option">
                <input type="radio" [(ngModel)]="repayForm.repaymentType" name="repayType"
                       value="PARTIAL" (change)="onRepayTypeChange()" />
                <span>سداد جزئي — سيتبقى رصيد بعد السداد</span>
              </label>
            </div>

            <!-- Amount -->
            <div class="form-group">
              <label>مبلغ السداد (ج.م) *</label>
              <input type="number" [(ngModel)]="repayForm.amount" name="repayAmount"
                     class="form-input" min="1" [max]="adv.remainingBalance" />
              <small class="hint">الحد الأقصى: {{ adv.remainingBalance | number:'1.0-0' }} ج.م</small>
            </div>

            <!-- Date & Method Row -->
            <div class="form-grid">
              <div class="form-group">
                <label>تاريخ السداد</label>
                <input type="date" [(ngModel)]="repayForm.repaymentDate" name="repayDate" class="form-input" />
              </div>
              <div class="form-group">
                <label>طريقة السداد</label>
                <select [(ngModel)]="repayForm.paymentMethod" name="repayMethod" class="form-input">
                  <option value="">اختر</option>
                  <option value="CASH">نقداً</option>
                  <option value="BANK_TRANSFER">تحويل بنكي</option>
                  <option value="CHEQUE">شيك</option>
                  <option value="DEDUCTION">خصم من المستحقات</option>
                </select>
              </div>
            </div>

            <!-- Receipt Ref -->
            <div class="form-group">
              <label>رقم الإيصال أو مرجع الدفع</label>
              <input type="text" [(ngModel)]="repayForm.receiptRef" name="receiptRef" class="form-input"
                     placeholder="اختياري" />
            </div>

            <!-- Notes -->
            <div class="form-group">
              <label>ملاحظات</label>
              <input type="text" [(ngModel)]="repayForm.notes" name="repayNotes" class="form-input"
                     placeholder="اختياري" />
            </div>

            <!-- Preview -->
            @if (repayPreview(); as preview) {
              <div class="repay-preview" [class.preview-close]="preview.willClose">
                <strong>معاينة النتيجة:</strong>
                <div class="preview-row">
                  <span>الرصيد قبل السداد:</span>
                  <span>{{ preview.before | number:'1.0-0' }} ج.م</span>
                </div>
                <div class="preview-row">
                  <span>مبلغ السداد:</span>
                  <span class="deduct-amount">- {{ preview.amount | number:'1.0-0' }} ج.م</span>
                </div>
                <div class="preview-row total-row">
                  <span>الرصيد بعد السداد:</span>
                  <span [class.zero-balance]="preview.after <= 0">{{ preview.after | number:'1.0-0' }} ج.م</span>
                </div>
                <div class="preview-impact">{{ preview.impact }}</div>
              </div>
            }
          </div>
        }
        @if (repayTarget()) {
          <div modal-actions class="modal-actions-bar">
            <button type="button" class="btn btn-primary" [disabled]="saving()" (click)="confirmRepayment()">
              {{ saving() ? 'جارٍ التنفيذ...' : 'تأكيد السداد' }}
            </button>
            <button type="button" class="btn btn-secondary" [disabled]="saving()" (click)="closeRepayModal()">إلغاء</button>
          </div>
        }
      </app-modal-dialog>

      <!-- Confirmation Dialog -->
      @if (confirmAction(); as action) {
        <div class="confirm-overlay" (click)="cancelAction()">
          <div class="confirm-dialog" (click)="$event.stopPropagation()">
            <div class="confirm-icon">⚠️</div>
            <div class="confirm-message">{{ action.message }}</div>
            <div class="confirm-actions">
              <button type="button" class="btn btn-primary" (click)="action.onConfirm()">تأكيد</button>
              <button type="button" class="btn btn-secondary" (click)="cancelAction()">إلغاء</button>
            </div>
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
    .stats-row { display: grid; grid-template-columns: repeat(3, 1fr); gap: 1rem; }
    .policy-summary-card { display: flex; justify-content: space-between; align-items: center; gap: 1rem; }
    .policy-summary-card p { margin: .25rem 0 0; color: #64748b; font-size: .8rem; }
    .policy-chips { display: flex; flex-wrap: wrap; gap: .4rem; justify-content: flex-end; }
    .policy-chip { background: #eff6ff; color: #1d4ed8; }
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

    /* Repayment Modal Styles */
    .repay-container { display: flex; flex-direction: column; gap: 1rem; }
    .repay-info-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 0.75rem; background: #f8fafc; border-radius: 8px; padding: 0.75rem; }
    .repay-info-item { display: flex; flex-direction: column; gap: 0.25rem; }
    .info-label { font-size: 0.75rem; color: #64748b; }
    .info-value { font-size: 0.9375rem; font-weight: 700; color: #0f172a; }
    .balance-highlight { color: #dc2626; font-size: 1.125rem; }
    .repay-type-group { display: flex; flex-direction: column; gap: 0.5rem; background: #fffbeb; border: 1px solid #fde68a; border-radius: 8px; padding: 0.75rem; }
    .repay-type-option { display: flex; align-items: center; gap: 0.5rem; cursor: pointer; font-size: 0.875rem; }
    .repay-type-option input[type="radio"] { width: 1rem; height: 1rem; accent-color: #d97706; }
    .repay-preview { background: #f0fdf4; border: 1px solid #bbf7d0; border-radius: 8px; padding: 0.75rem 1rem; font-size: 0.875rem; display: flex; flex-direction: column; gap: 0.375rem; }
    .repay-preview.preview-close { background: #fef2f2; border-color: #fecaca; }
    .preview-row { display: flex; justify-content: space-between; }
    .preview-row.total-row { border-top: 1px solid #e2e8f0; padding-top: 0.375rem; font-weight: 700; }
    .deduct-amount { color: #dc2626; }
    .zero-balance { color: #16a34a; }
    .preview-impact { font-weight: 600; color: #1e40af; margin-top: 0.25rem; }
    .repay-preview.preview-close .preview-impact { color: #dc2626; }
    .hint { font-size: 0.75rem; color: #94a3b8; }

    /* Confirmation Dialog */
    .confirm-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.4); display: flex; align-items: center; justify-content: center; z-index: 9999; }
    .confirm-dialog { background: #fff; border-radius: 12px; padding: 2rem; min-width: 360px; max-width: 480px; text-align: center; display: flex; flex-direction: column; gap: 1rem; box-shadow: 0 20px 60px rgba(0,0,0,0.2); }
    .confirm-icon { font-size: 2rem; }
    .confirm-message { font-size: 1rem; color: #334155; line-height: 1.6; }
    .confirm-actions { display: flex; gap: 0.75rem; justify-content: center; }
  `]
})
export class AdvancesComponent implements OnInit {
  workforceService = inject(WorkforceService);
  private notificationService = inject(NotificationService);

  loading = signal(false);
  saving = signal(false);
  isModalOpen = false;
  policyModalOpen = signal(false);
  policyForm: AdvancePolicy = this.defaultPolicyForm();

  // Repayment modal state
  repayModalOpen = signal(false);
  repayTarget = signal<WorkforceAdvance | null>(null);
  repayForm: {
    repaymentType: 'PARTIAL' | 'FULL';
    amount: number;
    repaymentDate: string;
    paymentMethod: string;
    receiptRef: string;
    notes: string;
  } = this.defaultRepayForm();

  repayPreview = computed(() => {
    const adv = this.repayTarget();
    if (!adv) return null;
    const before = adv.remainingBalance;
    const amount = this.repayForm.amount || 0;
    const after = Math.max(0, before - amount);
    const willClose = after <= 0;
    const impact = willClose ? 'سيتم إغلاق السلفة بالكامل' : `سيتبقى رصيد ${after} ج.م بعد السداد`;
    return { before, amount, after, willClose, impact };
  });

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
    this.workforceService.loadCategories().subscribe();
    this.workforceService.loadAdvancePolicies().subscribe();
  }

  openCreateModal() {
    this.form = this.defaultForm();
    const workers = this.workforceService.workers();
    if (workers.length > 0) this.form.workerId = workers[0].id;
    this.applyAdvancePolicy();
    this.isModalOpen = true;
  }

  onRecipientTypeChange() {
    this.form.workerId = '';
    this.form.contractorId = '';
    const workers = this.workforceService.workers();
    const contractors = this.workforceService.contractors();
    if (this.form.recipientType === 'WORKER' && workers.length > 0) this.form.workerId = workers[0].id;
    if (this.form.recipientType === 'CONTRACTOR' && contractors.length > 0) this.form.contractorId = contractors[0].id;
    this.applyAdvancePolicy();
  }

  openPolicyModal(): void { this.policyForm = this.defaultPolicyForm(); this.policyModalOpen.set(true); }

  savePolicy(): void {
    if (this.policyForm.scopeType !== 'GLOBAL' && !this.policyForm.scopeId) { this.notificationService.warning('اختر الفئة أو العامل للاستثناء'); return; }
    this.saving.set(true);
    this.workforceService.saveAdvancePolicy(this.policyForm).subscribe({
      next: () => { this.saving.set(false); this.policyModalOpen.set(false); this.notificationService.success('تم حفظ سياسة السلف بنجاح ✓'); },
      error: error => { this.saving.set(false); this.notificationService.error(error?.error?.detail ?? 'تعذّر حفظ السياسة'); },
    });
  }

  applyAdvancePolicy(): void {
    const policies = this.workforceService.advancePolicies().filter(policy => policy.active);
    const worker = this.workforceService.workers().find(item => item.id === this.form.workerId);
    const policy = policies.find(item => item.scopeType === 'WORKER' && item.scopeId === worker?.id)
      ?? policies.find(item => item.scopeType === 'CATEGORY' && item.scopeId === worker?.categoryId)
      ?? policies.find(item => item.scopeType === 'GLOBAL');
    if (!policy) return;
    Object.assign(this.form, { deductionMode: policy.deductionMode, deductionFrequency: policy.deductionFrequency, maxDeductionPercent: policy.maxDeductionPercent, totalInstallments: policy.defaultInstallments, deferralPeriods: policy.deferralPeriods });
    this.recalcInstallment();
  }

  getPolicyScopeLabel(policy: AdvancePolicy): string { return policy.scopeType === 'GLOBAL' ? 'الإعداد العام' : policy.scopeType === 'CATEGORY' ? 'استثناء فئة' : 'استثناء عامل'; }

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

  confirmAction = signal<{ message: string; onConfirm: () => void } | null>(null);

  pauseAdvance(adv: WorkforceAdvance) {
    const msg = `هل أنت متأكد من إيقاف اقتطاع السلفة الخاصة بـ (${adv.recipientType === 'WORKER' ? adv.workerName : adv.contractorName})؟`;
    this.confirmAction.set({
      message: msg,
      onConfirm: () => {
        this.confirmAction.set(null);
        this.workforceService.pauseAdvance(adv.id).subscribe({
          next: () => {
            this.notificationService.success('تم إيقاف خصم السلفة مؤقتاً ✓');
            this.workforceService.loadAdvances().subscribe();
          },
          error: (e) => this.notificationService.error('فشل إيقاف السلفة: ' + (e?.error?.message ?? e?.message))
        });
      }
    });
  }

  resumeAdvance(adv: WorkforceAdvance) {
    this.confirmAction.set({
      message: 'هل أنت متأكد من استئناف اقتطاع السلفة؟',
      onConfirm: () => {
        this.confirmAction.set(null);
        this.workforceService.resumeAdvance(adv.id).subscribe({
          next: () => {
            this.notificationService.success('تم استئناف خصم السلفة بنجاح ✓');
            this.workforceService.loadAdvances().subscribe();
          },
          error: (e) => this.notificationService.error('فشل استئناف السلفة: ' + (e?.error?.message ?? e?.message))
        });
      }
    });
  }

  cancelAction() {
    this.confirmAction.set(null);
  }

  openRepayModal(adv: WorkforceAdvance) {
    this.repayTarget.set(adv);
    this.repayForm = this.defaultRepayForm();
    this.repayForm.amount = adv.remainingBalance;
    this.repayForm.repaymentDate = new Date().toISOString().slice(0, 10);
    this.repayModalOpen.set(true);
  }

  onRepayTypeChange() {
    const adv = this.repayTarget();
    if (!adv) return;
    if (this.repayForm.repaymentType === 'FULL') {
      this.repayForm.amount = adv.remainingBalance;
    }
  }

  confirmRepayment() {
    const adv = this.repayTarget();
    if (!adv) return;
    const amount = this.repayForm.amount;
    if (!amount || amount <= 0) {
      this.notificationService.warning('يجب إدخال مبلغ سداد صحيح');
      return;
    }
    if (amount > adv.remainingBalance) {
      this.notificationService.warning('مبلغ السداد لا يمكن أن يتجاوز الرصيد المتبقي');
      return;
    }
    if (this.repayForm.repaymentType === 'FULL' && amount !== adv.remainingBalance) {
      this.notificationService.warning('عند السداد الكامل، يجب أن يساوي المبلغ الرصيد المتبقي');
      return;
    }

    this.saving.set(true);
    const payload: AdvanceRepayRequest = {
      amount,
      repaymentType: this.repayForm.repaymentType,
      repaymentDate: this.repayForm.repaymentDate || undefined,
      paymentMethod: this.repayForm.paymentMethod || undefined,
      receiptRef: this.repayForm.receiptRef || undefined,
      notes: this.repayForm.notes || undefined
    };
    this.workforceService.repayAdvance(adv.id, payload).subscribe({
      next: () => {
        this.saving.set(false);
        this.repayModalOpen.set(false);
        this.repayTarget.set(null);
        this.notificationService.success(`تم تسجيل السداد بمبلغ ${amount} ج.م بنجاح ✓`);
        this.workforceService.loadAdvances().subscribe();
      },
      error: (e) => {
        this.saving.set(false);
        const msg = e?.error?.detail ?? e?.error?.message ?? e?.message ?? 'خطأ غير متوقع';
        this.notificationService.error('فشل السداد: ' + msg);
      }
    });
  }

  closeRepayModal() {
    this.repayModalOpen.set(false);
    this.repayTarget.set(null);
  }

  repayAdvance(adv: WorkforceAdvance) {
    this.openRepayModal(adv);
  }

  exportCsv(): void {
    const rows = this.workforceService.advances().map((adv) => ({
      recipient: adv.recipientType === 'WORKER' ? adv.workerName : adv.contractorName,
      type: adv.recipientType === 'WORKER' ? 'عامل' : 'مقاول',
      amount: adv.amount,
      termType: this.getTermLabel(adv.termType),
      totalInstallments: adv.totalInstallments,
      installmentAmount: adv.installmentAmount,
      remainingBalance: adv.remainingBalance,
      deductionFrequency: this.getFrequencyLabel(adv.deductionFrequency),
      maxDeductionPercent: adv.maxDeductionPercent,
      status: this.getStatusLabel(adv.status),
    }));
    exportCsv(
      rows,
      [
        { key: 'recipient', label: 'الجهة المستفيدة' },
        { key: 'type', label: 'النوع' },
        { key: 'amount', label: 'المبلغ الكلي' },
        { key: 'termType', label: 'نوع السلفة' },
        { key: 'totalInstallments', label: 'الأقساط' },
        { key: 'installmentAmount', label: 'قيمة القسط' },
        { key: 'remainingBalance', label: 'الرصيد المتبقي' },
        { key: 'deductionFrequency', label: 'دورية الخصم' },
        { key: 'maxDeductionPercent', label: 'أقصى خصم %' },
        { key: 'status', label: 'الحالة' },
      ],
      `advances-${new Date().toISOString().slice(0, 10)}.csv`,
    );
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

  private defaultRepayForm() {
    return {
      repaymentType: 'FULL' as 'PARTIAL' | 'FULL',
      amount: 0,
      repaymentDate: '',
      paymentMethod: '',
      receiptRef: '',
      notes: ''
    };
  }

  private defaultPolicyForm(): AdvancePolicy {
    return { scopeType: 'GLOBAL', scopeId: '', deductionMode: 'AUTO', deductionFrequency: 'HALF_MONTH', maxDeductionPercent: 50, defaultInstallments: 1, deferralPeriods: 0, active: true };
  }
}
