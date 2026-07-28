import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { WorkforceService } from '../../data-access/workforce.service';
import { WorkforceAdvance } from '../../models/workforce.models';
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

      <div class="card">
        <table class="data-table">
          <thead>
            <tr>
              <th>الجهة المستفيدة</th>
              <th>المبلغ الكلي</th>
              <th>نوع السلفة</th>
              <th>الأقساط</th>
              <th>قيمة القسط</th>
              <th>الرصيد المتبقي</th>
              <th>أقصى خصم %</th>
              <th>الحالة</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let adv of workforceService.advances()">
              <td><strong>{{ adv.recipientType === 'WORKER' ? adv.workerName : adv.contractorName }}</strong></td>
              <td>{{ adv.amount | number:'1.2-2' }} ج.م</td>
              <td><span class="badge term-badge">{{ adv.termType === 'SHORT_TERM' ? 'قصيرة الأجل' : 'طويلة الأجل' }}</span></td>
              <td>{{ adv.totalInstallments }} قسط</td>
              <td>{{ adv.installmentAmount | number:'1.2-2' }} ج.م</td>
              <td><strong class="rem-bal">{{ adv.remainingBalance | number:'1.2-2' }} ج.م</strong></td>
              <td>{{ adv.maxDeductionPercent }} %</td>
              <td><span class="badge active">{{ adv.status }}</span></td>
            </tr>
          </tbody>
        </table>
      </div>

      <app-modal-dialog
        [isOpen]="isModalOpen"
        title="طلب صرف سُلفة جديدة"
        size="wide"
        [preventOutsideClose]="true"
        (close)="isModalOpen = false">

        <form (ngSubmit)="saveAdvance()" class="modal-form">
          <div class="form-grid">
            <div class="form-group">
              <label>المستفيد *</label>
              <select [(ngModel)]="form.recipientType" name="recipientType" class="form-input">
                <option value="WORKER">عامل</option>
                <option value="CONTRACTOR">مقاول</option>
              </select>
            </div>

            <div class="form-group" *ngIf="form.recipientType === 'WORKER'">
              <label>اختر العامل *</label>
              <select [(ngModel)]="form.workerId" name="workerId" class="form-input">
                <option *ngFor="let w of workforceService.workers()" [value]="w.id">{{ w.fullName }} ({{ w.code }})</option>
              </select>
            </div>

            <div class="form-group" *ngIf="form.recipientType === 'CONTRACTOR'">
              <label>اختر المقاول *</label>
              <select [(ngModel)]="form.contractorId" name="contractorId" class="form-input">
                <option *ngFor="let c of workforceService.contractors()" [value]="c.id">{{ c.name }} ({{ c.code }})</option>
              </select>
            </div>

            <div class="form-group">
              <label>مبلغ السُلفة (ج.م) *</label>
              <input type="number" [(ngModel)]="form.amount" name="amount" required class="form-input" />
            </div>

            <div class="form-group">
              <label>نوع السلفة</label>
              <select [(ngModel)]="form.termType" name="termType" class="form-input">
                <option value="SHORT_TERM">قصيرة الأجل (خصم مباشر)</option>
                <option value="LONG_TERM">طويلة الأجل (أقساط متكررة)</option>
              </select>
            </div>

            <div class="form-group">
              <label>عدد الأقساط *</label>
              <input type="number" [(ngModel)]="form.totalInstallments" name="totalInstallments" class="form-input" min="1" />
            </div>

            <div class="form-group">
              <label>الحد الأقصى للخصم من مستحق الفترة (%)</label>
              <input type="number" [(ngModel)]="form.maxDeductionPercent" name="maxDeductionPercent" class="form-input" />
            </div>

            <div class="form-group col-span-2">
              <label>سبب السلفة والملاحظات</label>
              <input type="text" [(ngModel)]="form.reason" name="reason" class="form-input" />
            </div>
          </div>
        </form>

        <div modal-actions class="modal-actions-bar">
          <button type="button" class="btn btn-primary" (click)="saveAdvance()">اعتماد وصرف السلفة</button>
          <button type="button" class="btn btn-secondary" (click)="isModalOpen = false">إلغاء</button>
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
    .badge { padding: 0.25rem 0.625rem; border-radius: 6px; font-size: 0.75rem; font-weight: 600; }
    .badge.active { background: #dcfce7; color: #166534; }
    .badge.term-badge { background: #e0e7ff; color: #3730a3; }
    .rem-bal { color: #dc2626; }
    .form-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 1rem; }
    .col-span-2 { grid-column: span 2; }
    .form-group { display: flex; flex-direction: column; gap: 0.5rem; }
    .form-group label { font-weight: 600; font-size: 0.875rem; color: #334155; }
    .form-input { padding: 0.625rem; border: 1px solid #cbd5e1; border-radius: 8px; font-size: 0.875rem; }
    .modal-actions-bar { width: 100%; display: flex; gap: 0.75rem; justify-content: flex-end; }
  `]
})
export class AdvancesComponent implements OnInit {
  workforceService = inject(WorkforceService);

  isModalOpen = false;
  form = {
    recipientType: 'WORKER', workerId: '', contractorId: '',
    amount: 1000, termType: 'SHORT_TERM', totalInstallments: 1,
    maxDeductionPercent: 50, reason: ''
  };

  ngOnInit() {
    this.workforceService.loadAdvances().subscribe();
    this.workforceService.loadWorkers().subscribe();
    this.workforceService.loadContractors().subscribe();
  }

  openCreateModal() {
    const workers = this.workforceService.workers();
    this.form = {
      recipientType: 'WORKER',
      workerId: workers.length > 0 ? workers[0].id : '',
      contractorId: '',
      amount: 1000, termType: 'SHORT_TERM', totalInstallments: 1,
      maxDeductionPercent: 50, reason: ''
    };
    this.isModalOpen = true;
  }

  saveAdvance() {
    if (!this.form.amount || this.form.amount <= 0) return;
    this.workforceService.createAdvance(this.form).subscribe(() => this.isModalOpen = false);
  }
}
