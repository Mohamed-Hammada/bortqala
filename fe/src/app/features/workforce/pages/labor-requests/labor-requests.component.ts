import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { WorkforceService } from '../../data-access/workforce.service';
import { LaborRequest } from '../../models/workforce.models';
import { ModalDialogComponent } from '../../../../shared/ui/modal-dialog/modal-dialog.component';

@Component({
  selector: 'app-labor-requests',
  standalone: true,
  imports: [CommonModule, FormsModule, ModalDialogComponent],
  template: `
    <div class="workforce-container">
      <header class="page-header">
        <div>
          <span class="eyebrow">طلبات العمالة</span>
          <h1>سجل طلبات الاحتياج والمتابعة اليومية</h1>
        </div>
        <button type="button" class="btn btn-primary" (click)="openCreateModal()">
          + إنشاء طلب عمالة جديد
        </button>
      </header>

      <div class="card">
        <table class="data-table">
          <thead>
            <tr>
              <th>رقم الطلب</th>
              <th>التاريخ</th>
              <th>المقاول المكلف</th>
              <th>الوردية</th>
              <th>حالة الطلب</th>
              <th>منشئ الطلب</th>
              <th>إجراءات</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let req of workforceService.laborRequests()">
              <td><strong>{{ req.requestNumber }}</strong></td>
              <td>{{ req.requestDate | date:'yyyy-MM-dd' }}</td>
              <td>{{ req.contractorName }}</td>
              <td>{{ req.shiftName || 'الوردية الأولى' }}</td>
              <td><span class="badge" [class.approved]="isApproved(req.status)" [class.draft]="isDraft(req.status)">{{ req.status }}</span></td>
              <td>{{ req.createdBy || 'النظام' }}</td>
              <td>
                <button *ngIf="isDraft(req.status)" type="button" class="btn btn-sm" (click)="approveRequest(req.id)">اعتماد الطلب</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <app-modal-dialog
        [isOpen]="isModalOpen"
        title="إنشاء طلب عمالة جديد"
        size="wide"
        [preventOutsideClose]="true"
        (close)="closeModal()">
        
        <form (ngSubmit)="saveRequest()" class="modal-form">
          <div class="form-grid">
            <div class="form-group">
              <label>رقم الطلب *</label>
              <input type="text" [(ngModel)]="form.requestNumber" name="requestNumber" required class="form-input" />
            </div>

            <div class="form-group">
              <label>المقاول المكلف *</label>
              <select [(ngModel)]="form.contractorId" name="contractorId" required class="form-input">
                <option *ngFor="let c of workforceService.contractors()" [value]="c.id">{{ c.name }}</option>
              </select>
            </div>

            <div class="form-group">
              <label>اسم الوردية</label>
              <input type="text" [(ngModel)]="form.shiftName" name="shiftName" class="form-input" placeholder="الوردية الصباحية" />
            </div>

            <div class="form-group">
              <label>ملاحظات الطلب</label>
              <input type="text" [(ngModel)]="form.notes" name="notes" class="form-input" />
            </div>
          </div>
        </form>

        <div modal-actions class="modal-actions-bar">
          <button type="button" class="btn btn-primary" (click)="saveRequest()">إرسال الطلب للمقاول</button>
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
    .badge.approved { background: #dcfce7; color: #166534; }
    .badge.draft { background: #fef3c7; color: #92400e; }
    .form-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 1rem; }
    .form-group { display: flex; flex-direction: column; gap: 0.5rem; }
    .form-group label { font-weight: 600; font-size: 0.875rem; color: #334155; }
    .form-input { padding: 0.625rem; border: 1px solid #cbd5e1; border-radius: 8px; font-size: 0.875rem; }
    .modal-actions-bar { width: 100%; display: flex; gap: 0.75rem; justify-content: flex-end; }
  `]
})
export class LaborRequestsComponent implements OnInit {
  workforceService = inject(WorkforceService);

  isModalOpen = false;
  form = {
    requestNumber: '', contractorId: '', shiftName: 'الوردية الأولى', notes: ''
  };

  isApproved(status: any): boolean {
    return String(status) === 'APPROVED';
  }

  isDraft(status: any): boolean {
    return String(status) === 'DRAFT';
  }

  ngOnInit() {
    this.workforceService.loadLaborRequests().subscribe();
    this.workforceService.loadContractors().subscribe();
  }

  openCreateModal() {
    const ctrs = this.workforceService.contractors();
    this.form = {
      requestNumber: 'REQ-' + Math.floor(100 + Math.random() * 900),
      contractorId: ctrs.length > 0 ? ctrs[0].id : '',
      shiftName: 'الوردية الأولى', notes: ''
    };
    this.isModalOpen = true;
  }

  closeModal() {
    this.isModalOpen = false;
  }

  saveRequest() {
    if (!this.form.requestNumber || !this.form.contractorId) return;
    this.workforceService.createLaborRequest(this.form).subscribe(() => this.closeModal());
  }

  approveRequest(id: string) {
    // approve logic
  }
}
