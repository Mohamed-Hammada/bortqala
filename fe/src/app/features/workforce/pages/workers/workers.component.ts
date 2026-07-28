import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { I18nService } from '../../../../core/i18n.service';
import { WorkforceService } from '../../data-access/workforce.service';
import { Worker } from '../../models/workforce.models';
import { ModalDialogComponent } from '../../../../shared/ui/modal-dialog/modal-dialog.component';

@Component({
  selector: 'app-workers',
  standalone: true,
  imports: [CommonModule, FormsModule, ModalDialogComponent],
  template: `
    <div class="workforce-container">
      <header class="page-header">
        <div>
          <span class="eyebrow">إدارة العمال</span>
          <h1>سجل العمال اليوميين والمؤقتين</h1>
        </div>
        <button type="button" class="btn btn-primary" (click)="openCreateModal()">
          + إضافة عامل جديد
        </button>
      </header>

      <div class="card">
        <table class="data-table">
          <thead>
            <tr>
              <th>كود العامل</th>
              <th>الاسم الكامل</th>
              <th>المقاول التابع له</th>
              <th>الفئة</th>
              <th>اليومية الافتراضية</th>
              <th>ساعات العمل</th>
              <th>طريقة الحضور</th>
              <th>الحالة</th>
              <th>إجراءات</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let w of workforceService.workers()">
              <td><strong>{{ w.code }}</strong></td>
              <td>{{ w.fullName }}</td>
              <td>{{ w.contractorName }}</td>
              <td><span class="badge category-badge">{{ w.categoryName }}</span></td>
              <td>{{ w.defaultDailyRate | number:'1.2-2' }} ج.م</td>
              <td>{{ w.standardDailyHours }} س</td>
              <td>{{ w.attendanceMode === 'MANUAL' ? 'يدوي' : 'بصمة' }}</td>
              <td><span class="badge active">{{ workerStatusLabel(w.status) }}</span></td>
              <td>
                <button type="button" class="btn btn-sm" (click)="openEditModal(w)">تعديل</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Centered Modal Dialog complying with Section 2 of spec -->
      <app-modal-dialog
        [isOpen]="isModalOpen"
        [title]="editingWorker ? 'تعديل بيانات العامل' : 'إضافة عامل جديد'"
        size="wide"
        [preventOutsideClose]="true"
        (close)="closeModal()">

        <form (ngSubmit)="saveWorker()" class="modal-form">
          <div class="form-grid">
            <div class="form-group">
              <label>كود العامل *</label>
              <input type="text" [(ngModel)]="form.code" name="code" required class="form-input" />
            </div>

            <div class="form-group">
              <label>الاسم الكامل *</label>
              <input type="text" [(ngModel)]="form.fullName" name="fullName" required class="form-input" />
            </div>

            <div class="form-group">
              <label>المقاول المكلف *</label>
              <select [(ngModel)]="form.contractorId" name="contractorId" required class="form-input">
                <option *ngFor="let c of workforceService.contractors()" [value]="c.id">{{ c.name }} ({{ c.code }})</option>
              </select>
            </div>

            <div class="form-group">
              <label>فئة العامل *</label>
              <select [(ngModel)]="form.categoryId" name="categoryId" required class="form-input">
                <option *ngFor="let cat of workforceService.categories()" [value]="cat.id">{{ cat.name }}</option>
              </select>
            </div>

            <div class="form-group">
              <label>اليومية الافتراضية (ج.م) *</label>
              <input type="number" [(ngModel)]="form.defaultDailyRate" name="defaultDailyRate" required class="form-input" />
            </div>

            <div class="form-group">
              <label>ساعات اليوم القياسية *</label>
              <input type="number" [(ngModel)]="form.standardDailyHours" name="standardDailyHours" required class="form-input" />
            </div>

            <div class="form-group">
              <label>رقم الهاتف</label>
              <input type="text" [(ngModel)]="form.phone" name="phone" class="form-input" />
            </div>

            <div class="form-group">
              <label>الرقم القومي</label>
              <input type="text" [(ngModel)]="form.nationalId" name="nationalId" class="form-input" />
            </div>
          </div>
        </form>

        <div modal-actions class="modal-actions-bar">
          <button type="button" class="btn btn-primary" (click)="saveWorker()">حفظ البيانات</button>
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
    .badge.category-badge { background: #e0f2fe; color: #0369a1; }
    .form-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 1rem; }
    .form-group { display: flex; flex-direction: column; gap: 0.5rem; }
    .form-group label { font-weight: 600; font-size: 0.875rem; color: #334155; }
    .form-input { padding: 0.625rem; border: 1px solid #cbd5e1; border-radius: 8px; font-size: 0.875rem; }
    .modal-actions-bar { width: 100%; display: flex; gap: 0.75rem; justify-content: flex-end; }
  `]
})
export class WorkersComponent implements OnInit {
  workforceService = inject(WorkforceService);
  i18n = inject(I18nService);

  workerStatusLabel(s: string): string {
    return s === 'ACTIVE' ? this.i18n.t('common.active') : this.i18n.t('common.inactive');
  }

  isModalOpen = false;
  editingWorker: Worker | null = null;
  form: Partial<Worker> = {
    code: '', fullName: '', contractorId: '', categoryId: '',
    defaultDailyRate: 200, standardDailyHours: 8, attendanceMode: 'MANUAL', status: 'ACTIVE'
  };

  ngOnInit() {
    this.workforceService.loadWorkers().subscribe();
    this.workforceService.loadContractors().subscribe();
    this.workforceService.loadCategories().subscribe();
  }

  openCreateModal() {
    this.editingWorker = null;
    const cats = this.workforceService.categories();
    const ctrs = this.workforceService.contractors();
    this.form = {
      code: 'WRK-' + Math.floor(1000 + Math.random() * 9000),
      fullName: '',
      contractorId: ctrs.length > 0 ? ctrs[0].id : '',
      categoryId: cats.length > 0 ? cats[0].id : '',
      defaultDailyRate: 200, standardDailyHours: 8, attendanceMode: 'MANUAL', status: 'ACTIVE'
    };
    this.isModalOpen = true;
  }

  openEditModal(w: Worker) {
    this.editingWorker = w;
    this.form = { ...w };
    this.isModalOpen = true;
  }

  closeModal() {
    this.isModalOpen = false;
  }

  saveWorker() {
    if (!this.form.code || !this.form.fullName || !this.form.contractorId || !this.form.categoryId) return;
    if (this.editingWorker) {
      this.workforceService.updateWorker(this.editingWorker.id, this.form).subscribe(() => this.closeModal());
    } else {
      this.workforceService.createWorker(this.form).subscribe(() => this.closeModal());
    }
  }
}
