import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { WorkforceService } from '../../data-access/workforce.service';
import { WorkerCategory } from '../../models/workforce.models';
import { ModalDialogComponent } from '../../../../shared/ui/modal-dialog/modal-dialog.component';

@Component({
  selector: 'app-categories',
  standalone: true,
  imports: [CommonModule, FormsModule, ModalDialogComponent],
  template: `
    <div class="workforce-container">
      <header class="page-header">
        <div>
          <span class="eyebrow">إدارة فئات العمالة</span>
          <h1>الفئات التشغيلية واليوميات القياسية</h1>
        </div>
        <button type="button" class="btn btn-primary" (click)="openCreateModal()">
          + إضافة فئة جديدة
        </button>
      </header>

      <div class="card">
        <table class="data-table">
          <thead>
            <tr>
              <th>كود الفئة</th>
              <th>اسم الفئة</th>
              <th>الوصف</th>
              <th>اليومية الافتراضية</th>
              <th>ساعات اليوم القياسية</th>
              <th>دورة التسوية الافتراضية</th>
              <th>الحالة</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let cat of workforceService.categories()">
              <td><strong>{{ cat.code }}</strong></td>
              <td>{{ cat.name }}</td>
              <td>{{ cat.description || '—' }}</td>
              <td>{{ cat.defaultDailyRate | number:'1.2-2' }} ج.م</td>
              <td>{{ cat.standardDailyHours }} س</td>
              <td>{{ getCycleLabel(cat.defaultSettlementCycle) }}</td>
              <td><span class="badge active">{{ getStatusLabel(cat.status) }}</span></td>
            </tr>
          </tbody>
        </table>
      </div>

      <app-modal-dialog
        [isOpen]="isModalOpen"
        [title]="'إضافة فئة موحدّة وقواعد دوام جديدة'"
        size="normal"
        [preventOutsideClose]="true"
        (close)="closeModal()">
        
        <form (ngSubmit)="saveCategory()" class="modal-form">
          <div class="form-group">
            <label>نطاق الفئة (Category Scope) *</label>
            <select [(ngModel)]="form.scope" name="scope" class="form-input">
              <option value="BOTH">كلاهما (موظفون وعمال - Both)</option>
              <option value="EMPLOYEE">موظفون فقط (Employees Only)</option>
              <option value="WORKER">عمال فقط (Workers Only)</option>
            </select>
          </div>
          <div class="form-group">
            <label>كود الفئة *</label>
            <input type="text" [(ngModel)]="form.code" name="code" required class="form-input" />
          </div>
          <div class="form-group">
            <label>اسم الفئة *</label>
            <input type="text" [(ngModel)]="form.name" name="name" required class="form-input" />
          </div>
          <div class="form-group">
            <label>وقت بداية الحضور المخطط (Start Time) *</label>
            <input type="time" [(ngModel)]="form.startTime" name="startTime" class="form-input" />
          </div>
          <div class="form-group">
            <label>وقت نهاية الانصراف المخطط (End Time) *</label>
            <input type="time" [(ngModel)]="form.endTime" name="endTime" class="form-input" />
          </div>
          <div class="form-group">
            <label>فترة السماح بالدقائق (Grace Period Mins)</label>
            <input type="number" min="0" [(ngModel)]="form.gracePeriodMinutes" name="gracePeriodMinutes" class="form-input" />
          </div>
          <div class="form-group">
            <label>اليومية / الراتب الافتراضي (ج.م) *</label>
            <input type="number" [(ngModel)]="form.defaultDailyRate" name="defaultDailyRate" required class="form-input" />
          </div>
          <div class="form-group">
            <label>ساعات العمل اليومية القياسية *</label>
            <input type="number" [(ngModel)]="form.standardDailyHours" name="standardDailyHours" required class="form-input" />
          </div>
        </form>

        <div modal-actions class="modal-actions-bar">
          <button type="button" class="btn btn-primary" (click)="saveCategory()">حفظ الفئة وقواعد الدوام</button>
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
    .badge { padding: 0.25rem 0.625rem; border-radius: 6px; font-size: 0.75rem; font-weight: 600; }
    .badge.active { background: #dcfce7; color: #166534; }
    .form-group { display: flex; flex-direction: column; gap: 0.5rem; margin-bottom: 1rem; }
    .form-group label { font-weight: 600; font-size: 0.875rem; color: #334155; }
    .form-input { padding: 0.625rem; border: 1px solid #cbd5e1; border-radius: 8px; font-size: 0.875rem; }
    .modal-actions-bar { width: 100%; display: flex; gap: 0.75rem; justify-content: flex-end; }
  `]
})
export class CategoriesComponent implements OnInit {
  workforceService = inject(WorkforceService);

  isModalOpen = false;
  form: Partial<WorkerCategory> & { scope?: string; startTime?: string; endTime?: string; gracePeriodMinutes?: number } = {
    code: '', name: '', description: '', defaultDailyRate: 200, standardDailyHours: 8, status: 'ACTIVE',
    scope: 'BOTH', startTime: '08:00', endTime: '16:00', gracePeriodMinutes: 15
  };

  ngOnInit() {
    this.workforceService.loadCategories().subscribe();
  }

  getCycleLabel(cycle: string): string {
    const map: Record<string, string> = {
      HALF_MONTH: 'نصف شهري (15 يومًا)',
      MONTHLY: 'شهري (30 يومًا)',
      WEEKLY: 'أسبوعي',
      THIRTY_DAYS: 'شهري (30 يومًا)',
      HALF_MONTHLY: 'نصف شهري (15 يومًا)',
    };
    return map[cycle] ?? cycle;
  }

  getStatusLabel(status: string): string {
    const map: Record<string, string> = { ACTIVE: 'نشط', INACTIVE: 'موقوف', SUSPENDED: 'معلق' };
    return map[status] ?? status;
  }

  openCreateModal() {
    this.form = {
      code: 'CAT-' + Math.floor(10 + Math.random() * 90),
      name: '', description: '', defaultDailyRate: 200, standardDailyHours: 8, status: 'ACTIVE',
      scope: 'BOTH', startTime: '08:00', endTime: '16:00', gracePeriodMinutes: 15
    };
    this.isModalOpen = true;
  }

  closeModal() {
    this.isModalOpen = false;
  }

  saveCategory() {
    if (!this.form.code || !this.form.name) return;
    this.workforceService.createCategory(this.form).subscribe(() => this.closeModal());
  }
}
