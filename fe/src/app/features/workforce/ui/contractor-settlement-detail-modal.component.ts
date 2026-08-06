import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ModalDialogComponent } from '../../../shared/ui/modal-dialog/modal-dialog.component';
import { WorkforceService } from '../data-access/workforce.service';
import { ContractorSettlementDetail } from '../models/workforce.models';
import { NotificationService } from '../../../core/notification.service';
import { apiErrorDetail } from '../../../core/api-error';

@Component({
  selector: 'app-contractor-settlement-detail-modal',
  standalone: true,
  imports: [CommonModule, FormsModule, ModalDialogComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <app-modal-dialog [isOpen]="isOpen" [title]="'تفاصيل تسوية المقاول — ' + (settlement?.contractorName || '')" size="wide" (close)="onClose()">
      @if (settlement; as item) {
        <div class="settlement-detail-container" dir="rtl">
          <!-- Summary Cards -->
          <div class="meta-grid">
            <article class="meta-card">
              <small>اسم المقاول</small>
              <strong>{{ item.contractorName }}</strong>
            </article>
            <article class="meta-card">
              <small>نموذج المحاسبة</small>
              <span>{{ item.accountingModel }}</span>
            </article>
            <article class="meta-card">
              <small>صافي المستحق للمقاول</small>
              <strong class="highlight-amount">{{ item.netPayable | number:'1.2-2' }} ج.م</strong>
            </article>
            <article class="meta-card">
              <small>المبلغ المنصرف</small>
              <span [class.paid-full]="item.paidAmount >= item.netPayable">{{ item.paidAmount | number:'1.2-2' }} ج.م</span>
            </article>
            <article class="meta-card">
              <small>حالة الترحيل للمالية</small>
              <span [class.status-posted]="item.postedJournalEntryId">{{ item.postedJournalEntryId ? 'تم الترحيل (' + item.postedJournalEntryId + ')' : 'غير مرحل' }}</span>
            </article>
            <article class="meta-card">
              <small>رقم الفاتورة والمرجع</small>
              <span>{{ item.invoiceNumber ? item.invoiceNumber + ' (' + (item.invoiceDate | date:'yyyy-MM-dd') + ')' : 'غير مربوطة' }}</span>
            </article>
          </div>

          <!-- Financial Lifecycle Actions Bar -->
          <div class="actions-bar">
            @if (!item.postedJournalEntryId) {
              <button type="button" class="btn primary" [disabled]="submitting()" (click)="postToFinance()">
                {{ submitting() ? 'جارٍ الترحيل…' : '⚖ ترحيل للمالية (قيد المستحق)' }}
              </button>
            }

            <button type="button" class="btn secondary" (click)="toggleInvoiceForm()">
              📄 {{ showInvoiceForm() ? 'إلغاء ربط الفاتورة' : 'ربط فاتورة المقاول' }}
            </button>

            @if (item.netPayable > item.paidAmount) {
              <button type="button" class="btn success" (click)="togglePaymentForm()">
                💰 {{ showPaymentForm() ? 'إلغاء تسجيل السداد' : 'تسجيل صرف مستحقات' }}
              </button>
            }
          </div>

          <!-- Inline Invoice Link Form -->
          @if (showInvoiceForm()) {
            <div class="inline-form-card">
              <h4>ربط فاتورة المقاول الرسمية</h4>
              <div class="form-row">
                <label>رقم الفاتورة *<input [(ngModel)]="invoiceForm.invoiceNumber" name="invNo" required /></label>
                <label>تاريخ الفاتورة *<input type="date" [(ngModel)]="invoiceForm.invoiceDate" name="invDate" required /></label>
                <label>مبلغ الفاتورة<input type="number" [(ngModel)]="invoiceForm.invoiceAmount" name="invAmt" /></label>
              </div>
              <div class="form-actions">
                <button type="button" class="btn primary" [disabled]="submitting()" (click)="saveInvoiceLink()">حفظ الربط</button>
              </div>
            </div>
          }

          <!-- Inline Record Payment Form -->
          @if (showPaymentForm()) {
            <div class="inline-form-card">
              <h4>تسجيل صرف مستحقات للمقاول</h4>
              <div class="form-row">
                <label>مبلغ الصرف *<input type="number" [(ngModel)]="paymentForm.amount" name="payAmt" required /></label>
                <label>تاريخ الصرف<input type="date" [(ngModel)]="paymentForm.paymentDate" name="payDate" /></label>
                <label>رقم المرجع / الشيك<input [(ngModel)]="paymentForm.paymentReference" name="payRef" /></label>
              </div>
              <div class="form-actions">
                <button type="button" class="btn success" [disabled]="submitting()" (click)="savePayment()">تأكيد الصرف</button>
              </div>
            </div>
          }

          <!-- Worker Lines Table -->
          <div class="lines-section">
            <h3>تفاصيل أجور عمال المقاول ({{ item.lines.length }})</h3>
            <div class="table-wrap">
              <table class="data-table">
                <thead>
                  <tr>
                    <th>العامل</th>
                    <th>أيام الحضور</th>
                    <th>فئة اليومية</th>
                    <th>الإجمالي</th>
                    <th>الخصومات</th>
                    <th>خصم السلف</th>
                    <th>الصافي</th>
                  </tr>
                </thead>
                <tbody>
                  @for (line of item.lines; track line.id) {
                    <tr>
                      <td><strong>{{ line.workerName }}</strong></td>
                      <td>{{ line.attendanceDays }} يوم</td>
                      <td>{{ line.dailyWage | number:'1.2-2' }}</td>
                      <td>{{ line.grossWage | number:'1.2-2' }}</td>
                      <td>{{ line.deductionsAmount | number:'1.2-2' }}</td>
                      <td class="advance-deduction">-{{ line.advanceInstallments | number:'1.2-2' }}</td>
                      <td><strong>{{ line.netWage | number:'1.2-2' }}</strong></td>
                    </tr>
                  } @empty {
                    <tr><td colspan="7" class="empty">لا توجد تفاصيل عمال لهذه التسوية.</td></tr>
                  }
                </tbody>
              </table>
            </div>
          </div>
        </div>
      }
      <div modal-actions>
        <button type="button" class="btn secondary" (click)="onClose()">إغلاق</button>
      </div>
    </app-modal-dialog>
  `,
  styles: [`
    .settlement-detail-container{display:grid;gap:1.25rem}.meta-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:.75rem}.meta-card{background:#fafaf9;border:1px solid #e7e5e4;border-radius:10px;padding:.75rem;display:grid;gap:.25rem}.meta-card small{color:#78716c;font-weight:600}.highlight-amount{color:#b7791f;font-size:1.15rem}.paid-full{color:#166534;font-weight:700}.status-posted{color:#1d4ed8;font-weight:700}.actions-bar{display:flex;gap:.5rem;flex-wrap:wrap;background:#f5f5f4;padding:.75rem;border-radius:10px}.btn{border:0;border-radius:8px;padding:.55rem .85rem;font-weight:700;cursor:pointer;background:#e7e5e4;color:#292524}.btn:disabled{opacity:.5;cursor:not-allowed}.primary{background:#b7791f;color:#fff}.secondary{background:#e7e5e4;color:#292524}.success{background:#dcfce7;color:#166534}.inline-form-card{background:#fff;border:1px solid #d6d3d1;border-radius:10px;padding:1rem;display:grid;gap:.75rem}.inline-form-card h4{margin:0;color:#44403c}.form-row{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:.75rem}.form-row label{display:grid;gap:.25rem;font-weight:700;font-size:.9rem}.form-row input{padding:.55rem;border:1px solid #cbd5e1;border-radius:6px}.form-actions{display:flex;justify-content:flex-end}.lines-section h3{margin-bottom:.5rem;font-size:1.05rem;color:#292524}.table-wrap{overflow:auto;max-height:350px}.data-table{width:100%;border-collapse:collapse}.data-table th,.data-table td{padding:.65rem;border-bottom:1px solid #e7e5e4;text-align:right}.advance-deduction{color:#b91c1c}.empty{text-align:center;color:#78716c;padding:1rem}
  `]
})
export class ContractorSettlementDetailModalComponent {
  @Input() isOpen = false;
  @Input() settlement: ContractorSettlementDetail | null = null;
  @Output() close = new EventEmitter<void>();
  @Output() updated = new EventEmitter<ContractorSettlementDetail>();

  private readonly workforceService = inject(WorkforceService);
  private readonly notification = inject(NotificationService);

  readonly submitting = signal(false);
  readonly showInvoiceForm = signal(false);
  readonly showPaymentForm = signal(false);

  invoiceForm = { invoiceNumber: '', invoiceDate: new Date().toISOString().slice(0, 10), invoiceAmount: 0 };
  paymentForm = { amount: 0, paymentDate: new Date().toISOString().slice(0, 10), paymentReference: '' };

  onClose(): void {
    this.showInvoiceForm.set(false);
    this.showPaymentForm.set(false);
    this.close.emit();
  }

  toggleInvoiceForm(): void {
    if (this.settlement) {
      this.invoiceForm.invoiceNumber = this.settlement.invoiceNumber || '';
      this.invoiceForm.invoiceAmount = this.settlement.netPayable;
    }
    this.showInvoiceForm.set(!this.showInvoiceForm());
    this.showPaymentForm.set(false);
  }

  togglePaymentForm(): void {
    if (this.settlement) {
      this.paymentForm.amount = this.settlement.netPayable - this.settlement.paidAmount;
    }
    this.showPaymentForm.set(!this.showPaymentForm());
    this.showInvoiceForm.set(false);
  }

  postToFinance(): void {
    if (!this.settlement) return;
    this.submitting.set(true);
    const operationId = crypto.randomUUID();
    this.workforceService.postSettlementToFinance(this.settlement.id, {
      operationId,
      expectedVersion: this.settlement.version || 0,
      reason: 'ترحيل أجور ومستحقات تسوية المقاول'
    }).subscribe({
      next: updatedItem => {
        this.submitting.set(false);
        this.notification.success('تم ترحيل التسوية للمالية بنجاح وسداد القيد.');
        this.updated.emit(updatedItem);
      },
      error: error => {
        this.submitting.set(false);
        this.notification.error(apiErrorDetail(error, 'تعذر ترحيل التسوية للمالية.'));
      }
    });
  }

  saveInvoiceLink(): void {
    if (!this.settlement || !this.invoiceForm.invoiceNumber) return;
    this.submitting.set(true);
    this.workforceService.linkSettlementInvoice(this.settlement.id, {
      invoiceNumber: this.invoiceForm.invoiceNumber,
      invoiceDate: new Date(this.invoiceForm.invoiceDate).getTime(),
      invoiceAmount: this.invoiceForm.invoiceAmount
    }).subscribe({
      next: updatedItem => {
        this.submitting.set(false);
        this.showInvoiceForm.set(false);
        this.notification.success('تم ربط الفاتورة بنجاح.');
        this.updated.emit(updatedItem);
      },
      error: error => {
        this.submitting.set(false);
        this.notification.error(apiErrorDetail(error, 'تعذر ربط الفاتورة.'));
      }
    });
  }

  savePayment(): void {
    if (!this.settlement || !this.paymentForm.amount) return;
    this.submitting.set(true);
    const operationId = crypto.randomUUID();
    this.workforceService.recordSettlementPayment(this.settlement.id, {
      operationId,
      amount: this.paymentForm.amount,
      paymentDate: new Date(this.paymentForm.paymentDate).getTime(),
      paymentReference: this.paymentForm.paymentReference
    }).subscribe({
      next: updatedItem => {
        this.submitting.set(false);
        this.showPaymentForm.set(false);
        this.notification.success('تم تسجيل الصرف بنجاح.');
        this.updated.emit(updatedItem);
      },
      error: error => {
        this.submitting.set(false);
        this.notification.error(apiErrorDetail(error, 'تعذر تسجيل الصرف.'));
      }
    });
  }
}
