import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { I18nService } from '../../../../core/i18n.service';
import { WorkforceService } from '../../data-access/workforce.service';
import { Worker } from '../../models/workforce.models';
import { ModalDialogComponent } from '../../../../shared/ui/modal-dialog/modal-dialog.component';
import { downloadBlob } from '../../../../core/download';
import { NotificationService } from '../../../../core/notification.service';

@Component({
  selector: 'app-workers',
  standalone: true,
  imports: [CommonModule, FormsModule, ModalDialogComponent],
  template: `
    <div class="workforce-container">
      <header class="page-header">
        <div><span class="eyebrow">إدارة العمال</span><h1>سجل العمال اليوميين والمؤقتين</h1></div>
        <div class="header-actions">
          <button type="button" class="btn btn-secondary" (click)="exportExcel()">⇩ تصدير Excel</button>
          <button type="button" class="btn btn-primary" (click)="openCreateModal()">+ إضافة عامل جديد</button>
        </div>
      </header>

      <div class="card">
        <table class="data-table">
          <thead><tr>
            <th>كود العامل</th><th>الاسم الكامل</th><th>المقاول التابع له</th><th>الفئة</th>
            <th>اليومية الافتراضية</th><th>ساعات العمل</th><th>طريقة الحضور</th><th>الحالة</th><th>إجراءات</th>
          </tr></thead>
          <tbody>
            <tr *ngFor="let w of workforceService.workers()">
              <td><strong>{{ w.code }}</strong></td><td>{{ w.fullName }}</td><td>{{ w.contractorName }}</td>
              <td><span class="badge category-badge">{{ w.categoryName }}</span></td>
              <td>{{ w.defaultDailyRate | number:'1.2-2' }} ج.م</td><td>{{ w.standardDailyHours }} س</td>
              <td>{{ w.attendanceMode === 'MANUAL' ? 'يدوي' : 'بصمة' }}</td>
              <td><span class="badge active">{{ workerStatusLabel(w.status) }}</span></td>
              <td><button type="button" class="btn btn-sm" (click)="openEditModal(w)">تعديل</button></td>
            </tr>
          </tbody>
        </table>
      </div>

      <app-modal-dialog [isOpen]="isModalOpen" [title]="editingWorker ? 'تعديل بيانات العامل' : 'إضافة عامل جديد'"
        size="wide" [preventOutsideClose]="true" (close)="closeModal()">
        <form (ngSubmit)="saveWorker()" class="modal-form">
          <div class="form-grid">
            <div class="form-group"><label>كود العامل *</label><input type="text" [(ngModel)]="form.code" name="code" required class="form-input" [disabled]="saving()" /></div>
            <div class="form-group"><label>الاسم الكامل *</label><input type="text" [(ngModel)]="form.fullName" name="fullName" required class="form-input" [disabled]="saving()" /></div>
            <div class="form-group">
              <label>المقاول المكلف *</label>
              <select [(ngModel)]="form.contractorId" name="contractorId" required class="form-input" [disabled]="saving()">
                <option value="" disabled>اختر المقاول</option>
                <option *ngFor="let c of workforceService.contractors()" [value]="c.id">{{ c.name }} ({{ c.code }})</option>
              </select>
            </div>
            <div class="form-group">
              <label>فئة العامل *</label>
              <select [(ngModel)]="form.categoryId" name="categoryId" required class="form-input" [disabled]="saving()" (ngModelChange)="onCategoryChange($event)">
                <option value="" disabled>اختر الفئة</option>
                <option *ngFor="let cat of workforceService.categories()" [value]="cat.id">{{ cat.name }} — {{ cat.defaultDailyRate | number:'1.2-2' }} ج.م</option>
              </select>
              <small>عند إنشاء عامل جديد تُورث اليومية وساعات العمل من الفئة المختارة ويمكن تعديلهما بعد ذلك.</small>
            </div>
            <div class="form-group"><label>اليومية الافتراضية (ج.م) *</label><input type="number" min="0" [(ngModel)]="form.defaultDailyRate" name="defaultDailyRate" required class="form-input" [disabled]="saving()" /></div>
            <div class="form-group"><label>ساعات اليوم القياسية *</label><input type="number" min="0" step="0.5" [(ngModel)]="form.standardDailyHours" name="standardDailyHours" required class="form-input" [disabled]="saving()" /></div>
            <div class="form-group"><label>رقم الهاتف</label><input type="text" [(ngModel)]="form.phone" name="phone" class="form-input" [disabled]="saving()" /></div>
            <div class="form-group"><label>الرقم القومي</label><input type="text" [(ngModel)]="form.nationalId" name="nationalId" class="form-input" [disabled]="saving()" /></div>
          </div>
          @if (saveError()) { <div class="save-error" role="alert">{{ saveError() }}</div> }
        </form>
        <div modal-actions class="modal-actions-bar">
          <button type="button" class="btn btn-primary" (click)="saveWorker()" [disabled]="saving()">{{ saving() ? 'جارٍ الحفظ...' : 'حفظ البيانات' }}</button>
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
    .page-header h1 { font-size: 1.75rem; font-weight: 800; color: #0f172a; margin: .25rem 0 0; }
    .card { background: #fff; border-radius: 12px; border: 1px solid #e2e8f0; padding: 1.25rem; overflow-x: auto; }
    .data-table { width: 100%; border-collapse: collapse; text-align: right; }
    .data-table th,.data-table td { padding: .75rem 1rem; border-bottom: 1px solid #e2e8f0; white-space: nowrap; }
    .btn { padding: .625rem 1.25rem; border-radius: 8px; font-weight: 600; cursor: pointer; border: none; }
    .btn:disabled { opacity: .65; cursor: not-allowed; }
    .btn-primary { background: #d97706; color:#fff; }.btn-secondary { background:#e2e8f0;color:#334155; }.btn-sm { padding:.375rem .75rem;font-size:.875rem; }
    .badge { padding:.25rem .625rem;border-radius:6px;font-size:.75rem;font-weight:600; }.badge.active { background:#dcfce7;color:#166534; }.badge.category-badge { background:#e0f2fe;color:#0369a1; }
    .form-grid { display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:1rem; }.form-group { display:flex;flex-direction:column;gap:.5rem; }
    .form-group label { font-weight:600;font-size:.875rem;color:#334155; }.form-group small { color:#64748b; }
    .form-input { padding:.625rem;border:1px solid #cbd5e1;border-radius:8px;font-size:.875rem; }.modal-actions-bar { width:100%;display:flex;gap:.75rem;justify-content:flex-end; }
    .save-error { margin-top:1rem;padding:.75rem 1rem;border:1px solid #fecaca;border-radius:8px;background:#fef2f2;color:#991b1b; }
    @media (max-width:760px){.page-header{align-items:stretch;flex-direction:column;gap:1rem}.form-grid{grid-template-columns:1fr}}
  `]
})
export class WorkersComponent implements OnInit {
  workforceService = inject(WorkforceService);
  i18n = inject(I18nService);
  notification = inject(NotificationService);
  readonly saving = signal(false);
  readonly saveError = signal<string | null>(null);

  workerStatusLabel(s: string): string {
    return s === 'ACTIVE' ? this.i18n.t('common.active') : this.i18n.t('common.inactive');
  }

  isModalOpen = false;
  editingWorker: Worker | null = null;
  form: Partial<Worker> = {
    code: '', fullName: '', contractorId: '', categoryId: '',
    defaultDailyRate: 0, standardDailyHours: 8, attendanceMode: 'MANUAL', status: 'ACTIVE'
  };

  ngOnInit() {
    this.workforceService.loadWorkers().subscribe();
    this.workforceService.loadContractors().subscribe();
    this.workforceService.loadCategories().subscribe();
  }

  exportExcel(): void {
    this.workforceService.exportWorkersExcel().subscribe(blob => downloadBlob(blob, `workers-${new Date().toISOString().slice(0, 10)}.xlsx`));
  }

  openCreateModal() {
    this.editingWorker = null;
    this.saveError.set(null);
    const cats = this.workforceService.categories();
    const ctrs = this.workforceService.contractors();
    const firstCategory = cats[0];
    this.form = {
      code: 'WRK-' + Math.floor(1000 + Math.random() * 9000),
      fullName: '',
      contractorId: ctrs[0]?.id ?? '',
      categoryId: firstCategory?.id ?? '',
      defaultDailyRate: firstCategory?.defaultDailyRate ?? 0,
      standardDailyHours: firstCategory?.standardDailyHours ?? 8,
      attendanceMode: 'MANUAL',
      status: 'ACTIVE'
    };
    this.isModalOpen = true;
  }

  openEditModal(w: Worker) {
    this.editingWorker = w;
    this.saveError.set(null);
    this.form = { ...w };
    this.isModalOpen = true;
  }

  onCategoryChange(categoryId: string): void {
    if (this.editingWorker) return;
    const category = this.workforceService.categories().find(item => item.id === categoryId);
    if (!category) return;
    this.form.defaultDailyRate = category.defaultDailyRate;
    this.form.standardDailyHours = category.standardDailyHours;
  }

  closeModal() {
    if (this.saving()) return;
    this.isModalOpen = false;
    this.saveError.set(null);
  }

  async saveWorker(): Promise<void> {
    if (this.saving()) return;
    if (!this.form.code?.trim() || !this.form.fullName?.trim() || !this.form.contractorId || !this.form.categoryId) {
      this.saveError.set('كود العامل والاسم والمقاول والفئة حقول إلزامية.');
      return;
    }
    if ((this.form.defaultDailyRate ?? 0) < 0 || (this.form.standardDailyHours ?? 0) <= 0) {
      this.saveError.set('اليومية يجب ألا تكون سالبة وساعات العمل يجب أن تكون أكبر من صفر.');
      return;
    }

    const wasEditing = !!this.editingWorker;
    this.saving.set(true);
    this.saveError.set(null);
    try {
      const saved = this.editingWorker
        ? await firstValueFrom(this.workforceService.updateWorker(this.editingWorker.id, this.form))
        : await firstValueFrom(this.workforceService.createWorker(this.form));
      const refreshed = await firstValueFrom(this.workforceService.loadWorkers());
      if (!saved?.id || !refreshed.some(worker => worker.id === saved.id)) {
        throw new Error('تم إرسال طلب الحفظ لكن العامل لم يظهر بعد إعادة تحميل البيانات.');
      }
      this.isModalOpen = false;
      this.notification.success(wasEditing ? 'تم تحديث بيانات العامل بنجاح' : 'تم إنشاء العامل بنجاح');
    } catch (error: any) {
      this.saveError.set(error?.error?.message ?? error?.error?.detail ?? error?.message ?? 'تعذر حفظ العامل. أعد المحاولة.');
    } finally {
      this.saving.set(false);
    }
  }
}
