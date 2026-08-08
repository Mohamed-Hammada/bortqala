import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { WorkforceService } from '../../data-access/workforce.service';
import { WorkerCategory } from '../../models/workforce.models';
import { ModalDialogComponent } from '../../../../shared/ui/modal-dialog/modal-dialog.component';
import { downloadBlob } from '../../../../core/download';
import { NotificationService } from '../../../../core/notification.service';
import { I18nService } from '../../../../core/i18n.service';

@Component({
  selector: 'app-categories',
  standalone: true,
  imports: [CommonModule, FormsModule, ModalDialogComponent],
  template: `
    <div class="workforce-container">
      <header class="page-header">
        <div><span class="eyebrow">إدارة فئات العمالة</span><h1>الفئات التشغيلية واليوميات القياسية</h1></div>
        <div class="header-actions">
          <button type="button" class="btn btn-secondary" (click)="exportExcel()">⇩ تصدير Excel</button>
          <button type="button" class="btn btn-primary" (click)="openCreateModal()">+ إضافة فئة عمال جديدة</button>
        </div>
      </header>

      <div class="scope-note">
        هذه الشاشة تخص <strong>فئات العمال (WORKER)</strong>. فئات الموظفين تُدار من شاشة الفئات الرئيسية لتجنب إنشاء فئة "Both" لا يدعمها عقد واجهة Workforce الحالي.
      </div>

      <div class="card">
        <table class="data-table">
          <thead><tr>
            <th>كود الفئة</th><th>اسم الفئة</th><th>الوصف</th><th>اليومية الافتراضية</th>
            <th>ساعات اليوم القياسية</th><th>دورة التسوية الافتراضية</th><th>الحالة</th>
          </tr></thead>
          <tbody>
            <tr *ngFor="let cat of workforceService.categories()">
              <td><strong>{{ cat.code }}</strong></td><td>{{ cat.name }}</td><td>{{ cat.description || '—' }}</td>
              <td>{{ cat.defaultDailyRate | number:'1.2-2' }} ج.م</td><td>{{ cat.standardDailyHours }} س</td>
              <td>{{ getCycleLabel(cat.defaultSettlementCycle) }}</td><td><span class="badge active">{{ getStatusLabel(cat.status) }}</span></td>
            </tr>
          </tbody>
        </table>
      </div>

      <app-modal-dialog [isOpen]="isModalOpen" [title]="'إضافة فئة عمال جديدة'" size="normal"
        [preventOutsideClose]="true" (close)="closeModal()">
        <form (ngSubmit)="saveCategory()" class="modal-form">
          <div class="form-group">
            <label for="worker-category-code">كود الفئة *</label>
            <input id="worker-category-code" type="text" [(ngModel)]="form.code" name="code" required
              class="form-input" [disabled]="saving()" [attr.aria-invalid]="submitted() && !form.code?.trim()"
              aria-describedby="worker-category-code-error" />
            @if (submitted() && !form.code?.trim()) {
              <small id="worker-category-code-error" class="field-error">كود الفئة مطلوب.</small>
            }
          </div>
          <div class="form-group">
            <label for="worker-category-name">اسم الفئة *</label>
            <input id="worker-category-name" type="text" [(ngModel)]="form.name" name="name" required
              class="form-input" [disabled]="saving()" [attr.aria-invalid]="submitted() && !form.name?.trim()"
              aria-describedby="worker-category-name-error" />
            @if (submitted() && !form.name?.trim()) {
              <small id="worker-category-name-error" class="field-error">اسم الفئة مطلوب.</small>
            }
          </div>
          <div class="form-group"><label>الوصف</label><textarea [(ngModel)]="form.description" name="description" rows="2" class="form-input" [disabled]="saving()"></textarea></div>
          <div class="form-group"><label>اليومية الافتراضية (ج.م) *</label><input type="number" min="0" [(ngModel)]="form.defaultDailyRate" name="defaultDailyRate" required class="form-input" [disabled]="saving()" /></div>
          <div class="form-group"><label>ساعات العمل اليومية القياسية *</label><input type="number" min="0.5" step="0.5" [(ngModel)]="form.standardDailyHours" name="standardDailyHours" required class="form-input" [disabled]="saving()" /></div>
          <div class="form-group">
            <label>دورة التسوية الافتراضية *</label>
            <select [(ngModel)]="form.defaultSettlementCycle" name="defaultSettlementCycle" class="form-input" [disabled]="saving()">
              <option value="HALF_MONTH">نصف شهري (15 يومًا)</option>
              <option value="MONTHLY">شهري (30 يومًا)</option>
            </select>
          </div>
          @if (saveError()) { <div class="save-error" role="alert">{{ saveError() }}</div> }
        </form>
        <div modal-actions class="modal-actions-bar">
          <button type="button" class="btn btn-primary" (click)="saveCategory()" [disabled]="saving()">{{ saving() ? 'جارٍ الحفظ...' : 'حفظ الفئة' }}</button>
          <button type="button" class="btn btn-secondary" (click)="closeModal()" [disabled]="saving()">إلغاء</button>
        </div>
      </app-modal-dialog>
    </div>
  `,
  styles: [`
    .workforce-container{padding:1.5rem;display:flex;flex-direction:column;gap:1.5rem}.eyebrow{font-size:.875rem;color:#d97706;font-weight:600}
    .page-header{display:flex;justify-content:space-between;align-items:center}.header-actions{display:flex;gap:.75rem}.page-header h1{font-size:1.75rem;font-weight:800;color:#0f172a;margin:.25rem 0 0}
    .scope-note{padding:.875rem 1rem;border:1px solid #bae6fd;border-radius:10px;background:#f0f9ff;color:#0c4a6e}
    .card{background:#fff;border-radius:12px;border:1px solid #e2e8f0;padding:1.25rem;overflow-x:auto}.data-table{width:100%;border-collapse:collapse;text-align:right}
    .data-table th,.data-table td{padding:.75rem 1rem;border-bottom:1px solid #e2e8f0;white-space:nowrap}.btn{padding:.625rem 1.25rem;border-radius:8px;font-weight:600;cursor:pointer;border:none}
    .btn:disabled{opacity:.65;cursor:not-allowed}.btn-primary{background:#d97706;color:#fff}.btn-secondary{background:#e2e8f0;color:#334155}.badge{padding:.25rem .625rem;border-radius:6px;font-size:.75rem;font-weight:600}
    .badge.active{background:#dcfce7;color:#166534}.form-group{display:flex;flex-direction:column;gap:.5rem;margin-bottom:1rem}.form-group label{font-weight:600;font-size:.875rem;color:#334155}
    .form-input{padding:.625rem;border:1px solid #cbd5e1;border-radius:8px;font-size:.875rem}.modal-actions-bar{width:100%;display:flex;gap:.75rem;justify-content:flex-end}
    .save-error{margin-top:1rem;padding:.75rem 1rem;border:1px solid #fecaca;border-radius:8px;background:#fef2f2;color:#991b1b}.field-error{color:#b91c1c;font-size:.78rem}
    @media(max-width:760px){.page-header{align-items:stretch;flex-direction:column;gap:1rem}}
  `]
})
export class CategoriesComponent implements OnInit {
  workforceService = inject(WorkforceService);
  notification = inject(NotificationService);
  i18n = inject(I18nService);
  readonly saving = signal(false);
  readonly saveError = signal<string | null>(null);
  readonly submitted = signal(false);
  isModalOpen = false;

  form: Partial<WorkerCategory> = {
    code: '', name: '', description: '', defaultDailyRate: 0, standardDailyHours: 8,
    defaultSettlementCycle: 'HALF_MONTH', status: 'ACTIVE'
  };

  ngOnInit() {
    this.workforceService.loadCategories().subscribe();
  }

  exportExcel(): void {
    this.workforceService.exportCategoriesExcel().subscribe(blob => downloadBlob(blob, `worker-categories-${new Date().toISOString().slice(0, 10)}.xlsx`));
  }

  getCycleLabel(cycle: string): string {
    const map: Record<string, string> = {
      HALF_MONTH: 'نصف شهري (15 يومًا)', MONTHLY: 'شهري (30 يومًا)',
      THIRTY_DAYS: 'شهري (30 يومًا)', HALF_MONTHLY: 'نصف شهري (15 يومًا)'
    };
    return map[cycle] ?? cycle;
  }

  getStatusLabel(status: string): string {
    const map: Record<string, string> = { ACTIVE: 'نشط', INACTIVE: 'موقوف', SUSPENDED: 'معلق' };
    return map[status] ?? status;
  }

  openCreateModal() {
    this.saveError.set(null);
    this.submitted.set(false);
    this.form = {
      code: 'CAT-' + Math.floor(10 + Math.random() * 90),
      name: '', description: '', defaultDailyRate: 0, standardDailyHours: 8,
      defaultSettlementCycle: 'HALF_MONTH', status: 'ACTIVE'
    };
    this.isModalOpen = true;
  }

  closeModal() {
    if (this.saving()) return;
    this.isModalOpen = false;
    this.saveError.set(null);
    this.submitted.set(false);
  }

  async saveCategory(): Promise<void> {
    if (this.saving()) return;
    this.submitted.set(true);
    if (!this.form.code?.trim() || !this.form.name?.trim()) {
      this.saveError.set('كود الفئة واسم الفئة حقول إلزامية.');
      return;
    }
    if ((this.form.defaultDailyRate ?? 0) < 0 || (this.form.standardDailyHours ?? 0) <= 0) {
      this.saveError.set('اليومية يجب ألا تكون سالبة وساعات العمل يجب أن تكون أكبر من صفر.');
      return;
    }

    this.saving.set(true);
    this.saveError.set(null);
    try {
      const saved = await firstValueFrom(this.workforceService.createCategory(this.form));
      const refreshed = await firstValueFrom(this.workforceService.loadCategories());
      if (!saved?.id || !refreshed.some(category => category.id === saved.id)) {
        throw new Error('تم إرسال طلب الحفظ لكن الفئة لم تظهر بعد إعادة تحميل البيانات.');
      }
      this.isModalOpen = false;
      this.submitted.set(false);
      this.notification.success(this.i18n.t('workforce.categoryCreatedSuccess'));
    } catch (error: any) {
      this.saveError.set(error?.error?.message ?? error?.error?.detail ?? error?.message ?? 'تعذر حفظ الفئة. أعد المحاولة.');
    } finally {
      this.saving.set(false);
    }
  }
}
