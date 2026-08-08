import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { I18nService } from '../../../../core/i18n.service';
import { WorkforceService } from '../../data-access/workforce.service';
import { Contractor } from '../../models/workforce.models';
import { ModalDialogComponent } from '../../../../shared/ui/modal-dialog/modal-dialog.component';
import { downloadBlob } from '../../../../core/download';
import { NotificationService } from '../../../../core/notification.service';

@Component({
  selector: 'app-contractors',
  standalone: true,
  imports: [CommonModule, FormsModule, ModalDialogComponent],
  template: `
    <div class="workforce-container">
      <header class="page-header">
        <div>
          <span class="eyebrow">إدارة المقاولين</span>
          <h1>دليل المقاولين ونماذج المحاسبة</h1>
        </div>
        <div class="header-actions">
          <button type="button" class="btn btn-secondary" (click)="exportExcel()">⇩ تصدير Excel</button>
          <button type="button" class="btn btn-primary" (click)="openCreateModal()">+ إضافة مقاول جديد</button>
        </div>
      </header>

      <div class="card">
        <table class="data-table">
          <thead>
            <tr>
              <th>الكود</th><th>الاسم الكامل</th><th>الاسم التجاري</th><th>الهاتف</th>
              <th>نموذج المحاسبة</th><th>جهة الصرف</th><th>حالة التعامل</th><th>إجراءات</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let c of workforceService.contractors()">
              <td><strong>{{ c.code }}</strong></td>
              <td>{{ c.name }}</td>
              <td>{{ c.tradeName || '—' }}</td>
              <td>{{ c.phone }}</td>
              <td><span class="badge model-badge">{{ getModelLabel(c.accountingModel) }}</span></td>
              <td>{{ getRoutingLabel(c.paymentRouting) }}</td>
              <td><span class="badge active">{{ contractorStatusLabel(c.status) }}</span></td>
              <td><button type="button" class="btn btn-sm" (click)="openEditModal(c)">تعديل</button></td>
            </tr>
          </tbody>
        </table>
      </div>

      <app-modal-dialog
        [isOpen]="isModalOpen"
        [title]="editingContractor ? 'تعديل بيانات المقاول' : 'إضافة مقاول جديد'"
        size="wide"
        [preventOutsideClose]="true"
        (close)="closeModal()">

        <form (ngSubmit)="saveContractor()" class="modal-form">
          <div class="form-grid">
            <div class="form-group">
              <label>كود المقاول *</label>
              <input type="text" [(ngModel)]="form.code" name="code" required class="form-input" [disabled]="saving()" />
            </div>
            <div class="form-group">
              <label>اسم المقاول *</label>
              <input type="text" [(ngModel)]="form.name" name="name" required class="form-input" [disabled]="saving()" />
            </div>
            <div class="form-group">
              <label>الاسم التجاري</label>
              <input type="text" [(ngModel)]="form.tradeName" name="tradeName" class="form-input" [disabled]="saving()" />
            </div>
            <div class="form-group">
              <label>رقم الهاتف *</label>
              <input type="text" [(ngModel)]="form.phone" name="phone" required class="form-input" [disabled]="saving()" />
            </div>

            <div class="form-group col-span-2">
              <label>نموذج محاسبة المقاول الإلزامي *</label>
              <div class="model-cards">
                <label class="model-card" [class.selected]="form.accountingModel === 'worker_net_total'">
                  <input type="radio" [(ngModel)]="form.accountingModel" name="accountingModel" value="worker_net_total" [disabled]="saving()" />
                  <strong>مجموع صافي العمال</strong>
                  <p>مستحق المقاول يساوي مجموع صافي العمال بعد الخصومات والسلف.</p>
                </label>
                <label class="model-card" [class.selected]="form.accountingModel === 'contractor_daily_rate'">
                  <input type="radio" [(ngModel)]="form.accountingModel" name="accountingModel" value="contractor_daily_rate" [disabled]="saving()" />
                  <strong>سعر تعاقد مستقل</strong>
                  <p>الحساب بسعر المقاول لكل يوم/ساعة بصرف النظر عن يومية العامل.</p>
                </label>
                <label class="model-card" [class.selected]="form.accountingModel === 'worker_cost_plus_fee'">
                  <input type="radio" [(ngModel)]="form.accountingModel" name="accountingModel" value="worker_cost_plus_fee" [disabled]="saving()" />
                  <strong>تكلفة العمال + عمولة</strong>
                  <p>تكلفة العمال مضافاً إليها عمولة ثابتة أو نسبة مئوية.</p>
                </label>
                <label class="model-card" [class.selected]="form.accountingModel === 'fixed_period_amount'">
                  <input type="radio" [(ngModel)]="form.accountingModel" name="accountingModel" value="fixed_period_amount" [disabled]="saving()" />
                  <strong>مبلغ ثابت للفترة</strong>
                  <p>مبلغ ثابت متفق عليه لكل فترة تسوية مع تعديلات الإضافة والخصم.</p>
                </label>
              </div>
            </div>

            <div class="form-group" *ngIf="form.accountingModel === 'contractor_daily_rate'">
              <label>سعر المقاول اليومي الافتراضي</label>
              <input type="number" min="0" [(ngModel)]="form.defaultDailyRate" name="defaultDailyRate" class="form-input" [disabled]="saving()" />
            </div>
            <div class="form-group" *ngIf="form.accountingModel === 'worker_cost_plus_fee'">
              <label>قيمة العمولة (مبلغ أو نسبة %)</label>
              <input type="number" min="0" [(ngModel)]="form.feeValue" name="feeValue" class="form-input" [disabled]="saving()" />
            </div>
            <div class="form-group" *ngIf="form.accountingModel === 'fixed_period_amount'">
              <label>مبلغ الفترة الثابت</label>
              <input type="number" min="0" [(ngModel)]="form.fixedPeriodAmount" name="fixedPeriodAmount" class="form-input" [disabled]="saving()" />
            </div>

            <div class="form-group">
              <label>دورة التسوية</label>
              <select [(ngModel)]="form.settlementCycleDays" name="settlementCycleDays" class="form-input" [disabled]="saving()">
                <option [ngValue]="15">كل 15 يومًا (فترتان شهريًا)</option>
                <option [ngValue]="30">كل 30 يومًا</option>
              </select>
            </div>

            <div class="form-group">
              <label>جهة الصرف</label>
              <select [(ngModel)]="form.paymentRouting" name="paymentRouting" class="form-input" [disabled]="saving()">
                <option value="contractor_full">صرف كامل المبلغ للمقاول</option>
                <option value="worker_direct">صرف مستحقات العمال مباشرة وصرف العمولة للمقاول</option>
                <option value="mixed">صرف مختلط</option>
              </select>
            </div>
          </div>

          @if (saveError()) {
            <div class="save-error" role="alert">
              <strong>تعذر حفظ المقاول.</strong>
              <span>{{ saveError() }}</span>
              <span>راجع البيانات أو الاتصال ثم أعد المحاولة. لن تُغلق النافذة قبل تأكيد الحفظ من الخادم.</span>
            </div>
          }
        </form>

        <div modal-actions class="modal-actions-bar">
          <button type="button" class="btn btn-primary" (click)="saveContractor()" [disabled]="saving()">
            {{ saving() ? 'جارٍ الحفظ والتحقق...' : 'حفظ البيانات' }}
          </button>
          <button type="button" class="btn btn-secondary" (click)="closeModal()" [disabled]="saving()">إلغاء</button>
        </div>
      </app-modal-dialog>
    </div>
  `,
  styles: [`
    .workforce-container { padding: 1.5rem; display: flex; flex-direction: column; gap: 1.5rem; }
    .eyebrow { font-size: 0.875rem; color: #d97706; font-weight: 600; }
    .page-header { display: flex; justify-content: space-between; align-items: center; }
    .header-actions { display: flex; gap: .75rem; }
    .page-header h1 { font-size: 1.75rem; font-weight: 800; color: #0f172a; margin: 0.25rem 0 0 0; }
    .card { background: #fff; border-radius: 12px; border: 1px solid #e2e8f0; padding: 1.25rem; overflow-x: auto; }
    .data-table { width: 100%; border-collapse: collapse; text-align: right; }
    .data-table th, .data-table td { padding: 0.75rem 1rem; border-bottom: 1px solid #e2e8f0; white-space: nowrap; }
    .btn { padding: 0.625rem 1.25rem; border-radius: 8px; font-weight: 600; cursor: pointer; border: none; }
    .btn:disabled { cursor: not-allowed; opacity: .65; }
    .btn-primary { background: #d97706; color: #fff; }
    .btn-secondary { background: #e2e8f0; color: #334155; }
    .btn-sm { padding: 0.375rem 0.75rem; font-size: 0.875rem; }
    .badge { padding: 0.25rem 0.625rem; border-radius: 6px; font-size: 0.75rem; font-weight: 600; }
    .badge.active { background: #dcfce7; color: #166534; }
    .badge.model-badge { background: #fef3c7; color: #92400e; }
    .form-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 1rem; }
    .col-span-2 { grid-column: span 2; }
    .form-group { display: flex; flex-direction: column; gap: 0.5rem; }
    .form-group label { font-weight: 600; font-size: 0.875rem; color: #334155; }
    .form-input { padding: 0.625rem; border: 1px solid #cbd5e1; border-radius: 8px; font-size: 0.875rem; }
    .model-cards { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 0.75rem; }
    .model-card { border: 1px solid #e2e8f0; border-radius: 8px; padding: 0.875rem; cursor: pointer; display: flex; flex-direction: column; gap: 0.25rem; }
    .model-card.selected { border-color: #d97706; background: #fffbeb; }
    .model-card p { font-size: 0.75rem; color: #64748b; margin: 0; }
    .modal-actions-bar { width: 100%; display: flex; gap: 0.75rem; justify-content: flex-end; }
    .save-error { grid-column: 1 / -1; display: flex; flex-direction: column; gap: .25rem; margin-top: 1rem; padding: .75rem 1rem; border: 1px solid #fecaca; border-radius: 8px; background: #fef2f2; color: #991b1b; }
    @media (max-width: 760px) {
      .page-header { align-items: stretch; flex-direction: column; gap: 1rem; }
      .form-grid, .model-cards { grid-template-columns: 1fr; }
      .col-span-2 { grid-column: auto; }
    }
  `]
})
export class ContractorsComponent implements OnInit {
  workforceService = inject(WorkforceService);
  i18n = inject(I18nService);
  notification = inject(NotificationService);

  readonly saving = signal(false);
  readonly saveError = signal<string | null>(null);

  contractorStatusLabel(s: string): string {
    return s === 'ACTIVE' ? this.i18n.t('common.active') : this.i18n.t('common.inactive');
  }

  isModalOpen = false;
  editingContractor: Contractor | null = null;
  form: Partial<Contractor> = {
    code: '', name: '', tradeName: '', phone: '',
    accountingModel: 'worker_net_total', paymentRouting: 'contractor_full',
    settlementCycleDays: 15, defaultDailyRate: 0, status: 'ACTIVE'
  };

  ngOnInit() {
    this.workforceService.loadContractors().subscribe();
  }

  exportExcel(): void {
    this.workforceService.exportContractorsExcel().subscribe(blob => downloadBlob(blob, `contractors-${new Date().toISOString().slice(0, 10)}.xlsx`));
  }

  openCreateModal() {
    this.editingContractor = null;
    this.saveError.set(null);
    this.form = {
      code: 'CTR-' + Math.floor(100 + Math.random() * 900),
      name: '', tradeName: '', phone: '',
      accountingModel: 'worker_net_total', paymentRouting: 'contractor_full',
      settlementCycleDays: 15, defaultDailyRate: 0, status: 'ACTIVE'
    };
    this.isModalOpen = true;
  }

  openEditModal(c: Contractor) {
    this.editingContractor = c;
    this.saveError.set(null);
    this.form = { ...c };
    this.isModalOpen = true;
  }

  closeModal() {
    if (this.saving()) return;
    this.isModalOpen = false;
    this.saveError.set(null);
  }

  async saveContractor(): Promise<void> {
    if (this.saving()) return;
    if (!this.form.code?.trim() || !this.form.name?.trim() || !this.form.phone?.trim()) {
      this.saveError.set('الكود واسم المقاول ورقم الهاتف حقول إلزامية.');
      return;
    }

    const wasEditing = !!this.editingContractor;
    this.saving.set(true);
    this.saveError.set(null);
    try {
      const saved = this.editingContractor
        ? await firstValueFrom(this.workforceService.updateContractor(this.editingContractor.id, this.form))
        : await firstValueFrom(this.workforceService.createContractor(this.form));

      const refreshed = await firstValueFrom(this.workforceService.loadContractors());
      if (!saved?.id || !refreshed.some(contractor => contractor.id === saved.id)) {
        throw new Error('استجاب الخادم للحفظ لكن السجل لم يظهر بعد إعادة تحميل البيانات.');
      }

      this.editingContractor = saved;
      this.isModalOpen = false;
      this.notification.success(wasEditing ? 'تم تحديث بيانات المقاول بنجاح' : 'تم إنشاء المقاول بنجاح');
    } catch (error: any) {
      const message =
        error?.error?.message ??
        error?.error?.detail ??
        error?.message ??
        'حدث خطأ غير متوقع أثناء حفظ المقاول.';
      this.saveError.set(message);
    } finally {
      this.saving.set(false);
    }
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

  getRoutingLabel(routing: string): string {
    switch (routing) {
      case 'contractor_full': return 'كامل للمقاول';
      case 'worker_direct': return 'مباشر للعمال والعمولة للمقاول';
      case 'mixed': return 'مختلط';
      default: return routing;
    }
  }
}
