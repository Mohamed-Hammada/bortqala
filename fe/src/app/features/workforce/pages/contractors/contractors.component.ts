import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { I18nService } from '../../../../core/i18n.service';
import { WorkforceService } from '../../data-access/workforce.service';
import { Contractor, ContractorAccountingModel, PaymentRouting } from '../../models/workforce.models';
import { ModalDialogComponent } from '../../../../shared/ui/modal-dialog/modal-dialog.component';

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
        <button type="button" class="btn btn-primary" (click)="openCreateModal()">
          + إضافة مقاول جديد
        </button>
      </header>

      <div class="card">
        <table class="data-table">
          <thead>
            <tr>
              <th>الكود</th>
              <th>الاسم الكامل</th>
              <th>الاسم التجاري</th>
              <th>الهاتف</th>
              <th>نموذج المحاسبة</th>
              <th>جهة الصرف</th>
              <th>حالة التعامل</th>
              <th>إجراءات</th>
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
              <td>
                <button type="button" class="btn btn-sm" (click)="openEditModal(c)">تعديل</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Centered Modal Dialog for Contractor Add/Edit complying with Section 2 of spec -->
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
              <input type="text" [(ngModel)]="form.code" name="code" required class="form-input" />
            </div>

            <div class="form-group">
              <label>اسم المقاول *</label>
              <input type="text" [(ngModel)]="form.name" name="name" required class="form-input" />
            </div>

            <div class="form-group">
              <label>الاسم التجاري</label>
              <input type="text" [(ngModel)]="form.tradeName" name="tradeName" class="form-input" />
            </div>

            <div class="form-group">
              <label>رقم الهاتف *</label>
              <input type="text" [(ngModel)]="form.phone" name="phone" required class="form-input" />
            </div>

            <div class="form-group col-span-2">
              <label>نموذج محاسبة المقاول الإلزامي *</label>
              <div class="model-cards">
                <label class="model-card" [class.selected]="form.accountingModel === 'worker_net_total'">
                  <input type="radio" [(ngModel)]="form.accountingModel" name="accountingModel" value="worker_net_total" />
                  <strong>مجموع صافي العمال</strong>
                  <p>مستحق المقاول يساوي مجموع صافي العمال بعد الخصومات والسلف.</p>
                </label>

                <label class="model-card" [class.selected]="form.accountingModel === 'contractor_daily_rate'">
                  <input type="radio" [(ngModel)]="form.accountingModel" name="accountingModel" value="contractor_daily_rate" />
                  <strong>سعر تعاقد مستقل</strong>
                  <p>الحساب بسعر المقاول لكل يوم/ساعة بصرف النظر عن يومية العامل.</p>
                </label>

                <label class="model-card" [class.selected]="form.accountingModel === 'worker_cost_plus_fee'">
                  <input type="radio" [(ngModel)]="form.accountingModel" name="accountingModel" value="worker_cost_plus_fee" />
                  <strong>تكلفة العمال + عمولة</strong>
                  <p>تكلفة العمال مضافاً إليها عمولة ثابتة أو نسبة مئوية.</p>
                </label>

                <label class="model-card" [class.selected]="form.accountingModel === 'fixed_period_amount'">
                  <input type="radio" [(ngModel)]="form.accountingModel" name="accountingModel" value="fixed_period_amount" />
                  <strong>مبلغ ثابت للفترة</strong>
                  <p>مبلغ ثابت متفق عليه لكل فترة تسوية مع تعديلات الإضافة والخصم.</p>
                </label>
              </div>
            </div>

            <div class="form-group" *ngIf="form.accountingModel === 'contractor_daily_rate'">
              <label>سعر المقاول اليومي الافتراضي</label>
              <input type="number" [(ngModel)]="form.defaultDailyRate" name="defaultDailyRate" class="form-input" />
            </div>

            <div class="form-group" *ngIf="form.accountingModel === 'worker_cost_plus_fee'">
              <label>قيمة العمولة (مبلغ أو نسبة %)</label>
              <input type="number" [(ngModel)]="form.feeValue" name="feeValue" class="form-input" />
            </div>

            <div class="form-group" *ngIf="form.accountingModel === 'fixed_period_amount'">
              <label>مبلغ الفترة الثابت</label>
              <input type="number" [(ngModel)]="form.fixedPeriodAmount" name="fixedPeriodAmount" class="form-input" />
            </div>

            <div class="form-group">
              <label>جهة الصرف</label>
              <select [(ngModel)]="form.paymentRouting" name="paymentRouting" class="form-input">
                <option value="contractor_full">صرف كامل المبلغ للمقاول</option>
                <option value="worker_direct">صرف مستحقات العمال مباشرة وصرف العمولة للمقاول</option>
                <option value="mixed">صرف مختلط</option>
              </select>
            </div>
          </div>
        </form>

        <div modal-actions class="modal-actions-bar">
          <button type="button" class="btn btn-primary" (click)="saveContractor()">حفظ البيانات</button>
          <button type="button" class="btn btn-secondary" (click)="closeModal()">إلغاء</button>
        </div>
      </app-modal-dialog>
    </div>
  `,
  styles: [`
    .workforce-container { padding: 1.5rem; display: flex; flex-direction: column; gap: 1.5rem; }
    .eyebrow { font-size: 0.875rem; color: #d97706; font-weight: 600; }
    .page-header { display: flex; justify-content: space-between; align-items: center; }
    .page-header h1 { font-size: 1.75rem; font-weight: 800; color: #0f172a; margin: 0.25rem 0 0 0; }
    .card { background: #fff; border-radius: 12px; border: 1px solid #e2e8f0; padding: 1.25rem; }
    .data-table { width: 100%; border-collapse: collapse; text-align: right; }
    .data-table th, .data-table td { padding: 0.75rem 1rem; border-bottom: 1px solid #e2e8f0; }
    .btn { padding: 0.625rem 1.25rem; border-radius: 8px; font-weight: 600; cursor: pointer; border: none; }
    .btn-primary { background: #d97706; color: #fff; }
    .btn-secondary { background: #e2e8f0; color: #334155; }
    .btn-sm { padding: 0.375rem 0.75rem; font-size: 0.875rem; }
    .badge { padding: 0.25rem 0.625rem; border-radius: 6px; font-size: 0.75rem; font-weight: 600; }
    .badge.active { background: #dcfce7; color: #166534; }
    .badge.model-badge { background: #fef3c7; color: #92400e; }
    
    .form-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 1rem; }
    .col-span-2 { grid-column: span 2; }
    .form-group { display: flex; flex-direction: column; gap: 0.5rem; }
    .form-group label { font-weight: 600; font-size: 0.875rem; color: #334155; }
    .form-input { padding: 0.625rem; border: 1px solid #cbd5e1; border-radius: 8px; font-size: 0.875rem; }
    
    .model-cards { display: grid; grid-template-columns: repeat(2, 1fr); gap: 0.75rem; }
    .model-card { border: 1px solid #e2e8f0; border-radius: 8px; padding: 0.875rem; cursor: pointer; display: flex; flex-direction: column; gap: 0.25rem; }
    .model-card.selected { border-color: #d97706; background: #fffbeb; }
    .model-card p { font-size: 0.75rem; color: #64748b; margin: 0; }
    .modal-actions-bar { width: 100%; display: flex; gap: 0.75rem; justify-content: flex-end; }
  `]
})
export class ContractorsComponent implements OnInit {
  workforceService = inject(WorkforceService);
  i18n = inject(I18nService);

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

  openCreateModal() {
    this.editingContractor = null;
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
    this.form = { ...c };
    this.isModalOpen = true;
  }

  closeModal() {
    this.isModalOpen = false;
  }

  saveContractor() {
    if (!this.form.code || !this.form.name || !this.form.phone) return;
    if (this.editingContractor) {
      this.workforceService.updateContractor(this.editingContractor.id, this.form).subscribe(() => this.closeModal());
    } else {
      this.workforceService.createContractor(this.form).subscribe(() => this.closeModal());
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
