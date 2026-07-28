import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { WorkforceService } from '../../data-access/workforce.service';
import { SettlementPeriod, SettlementCalculationSummary } from '../../models/workforce.models';
import { ModalDialogComponent } from '../../../../shared/ui/modal-dialog/modal-dialog.component';

@Component({
  selector: 'app-settlement-periods',
  standalone: true,
  imports: [CommonModule, FormsModule, ModalDialogComponent],
  template: `
    <div class="workforce-container">
      <header class="page-header">
        <div>
          <span class="eyebrow">دورة التسوية المستندية</span>
          <h1>فترات تسوية العمالة والمقاولين (1-15 و 16-نهاية الشهر)</h1>
        </div>
        <button type="button" class="btn btn-primary" (click)="openCreateModal()">
          + فتح فترة تسوية جديدة
        </button>
      </header>

      <div class="card">
        <table class="data-table">
          <thead>
            <tr>
              <th>كود الفترة</th>
              <th>تاريخ البداية</th>
              <th>تاريخ النهاية</th>
              <th>نوع الفترة</th>
              <th>حالة التسوية</th>
              <th>إجراءات الحساب والقفل</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let p of workforceService.settlementPeriods()">
              <td><strong>{{ p.periodCode }}</strong></td>
              <td>{{ p.startDate }}</td>
              <td>{{ p.endDate }}</td>
              <td>{{ p.cycleType === 'HALF_MONTH' ? 'نصف شهرية (15 يوم)' : p.cycleType }}</td>
              <td><span class="badge" [class.approved]="p.status === 'APPROVED'" [class.review]="p.status === 'REVIEW'">{{ p.status }}</span></td>
              <td class="actions-cell">
                <button type="button" class="btn btn-sm btn-secondary" (click)="calculatePeriod(p.id)">إعادة احتساب الفترة</button>
                <button type="button" class="btn btn-sm btn-success" (click)="exportExcel(p.id, p.periodCode)">📥 تصدير كشف المدة (إكسيل)</button>
                <button *ngIf="p.status !== 'APPROVED'" type="button" class="btn btn-sm btn-primary" (click)="approvePeriod(p.id)">اعتماد وقفل الفترة</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Calculation Summary Modal -->
      <app-modal-dialog
        [isOpen]="isSummaryModalOpen"
        title="معاينة ونتيجة احتساب فترة التسوية"
        size="wide"
        (close)="isSummaryModalOpen = false">
        
        <div *ngIf="summary" class="summary-details">
          <div class="summary-grid">
            <div class="summary-item">
              <label>عدد العمال المحسوبين:</label>
              <strong>{{ summary.totalWorkers }}</strong>
            </div>
            <div class="summary-item">
              <label>إجمالي وحدات الحضور:</label>
              <strong>{{ summary.totalAttendanceUnits }} يوم/وحدة</strong>
            </div>
            <div class="summary-item">
              <label>إجمالي مستحقات العمال القائمة:</label>
              <strong>{{ summary.grossWorkersAmount | number:'1.2-2' }} ج.م</strong>
            </div>
            <div class="summary-item">
              <label>إجمالي خصومات السلف الأقساط:</label>
              <strong>{{ summary.totalAdvanceDeductions | number:'1.2-2' }} ج.م</strong>
            </div>
            <div class="summary-item net-item">
              <label>صافي مستحق العمال النهائي:</label>
              <strong>{{ summary.netWorkersAmount | number:'1.2-2' }} ج.م</strong>
            </div>
            <div class="summary-item net-item">
              <label>صافي مستحق المقاولين النهائي:</label>
              <strong>{{ summary.netContractorsPayable | number:'1.2-2' }} ج.م</strong>
            </div>
          </div>
        </div>

        <div modal-actions class="modal-actions-bar">
          <button type="button" class="btn btn-primary" (click)="isSummaryModalOpen = false">إغلاق المعاينة</button>
        </div>
      </app-modal-dialog>

      <!-- Create Period Modal -->
      <app-modal-dialog
        [isOpen]="isCreateModalOpen"
        title="إنشاء فترة تسوية جديدة"
        size="normal"
        [preventOutsideClose]="true"
        (close)="isCreateModalOpen = false">

        <form (ngSubmit)="savePeriod()" class="modal-form">
          <div class="form-group">
            <label>كود الفترة *</label>
            <input type="text" [(ngModel)]="createForm.periodCode" name="periodCode" required class="form-input" />
          </div>
          <div class="form-group">
            <label>تاريخ البداية *</label>
            <input type="date" [(ngModel)]="createForm.startDate" name="startDate" required class="form-input" />
          </div>
          <div class="form-group">
            <label>تاريخ النهاية *</label>
            <input type="date" [(ngModel)]="createForm.endDate" name="endDate" required class="form-input" />
          </div>
        </form>

        <div modal-actions class="modal-actions-bar">
          <button type="button" class="btn btn-primary" (click)="savePeriod()">إنشاء الفترة</button>
          <button type="button" class="btn btn-secondary" (click)="isCreateModalOpen = false">إلغاء</button>
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
    .btn-success { background: #16a34a; color: #fff; }
    .btn-secondary { background: #e2e8f0; color: #334155; }
    .btn-sm { padding: 0.375rem 0.75rem; font-size: 0.875rem; }
    .actions-cell { display: flex; gap: 0.5rem; }
    .badge { padding: 0.25rem 0.625rem; border-radius: 6px; font-size: 0.75rem; font-weight: 600; }
    .badge.approved { background: #dcfce7; color: #166534; }
    .badge.review { background: #fef3c7; color: #92400e; }
    .summary-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 1rem; }
    .summary-item { background: #f8fafc; padding: 1rem; border-radius: 8px; border: 1px solid #e2e8f0; display: flex; flex-direction: column; gap: 0.25rem; }
    .summary-item.net-item { background: #fffbeb; border-color: #fde68a; }
    .summary-item label { font-size: 0.875rem; color: #64748b; }
    .summary-item strong { font-size: 1.25rem; color: #0f172a; }
    .form-group { display: flex; flex-direction: column; gap: 0.5rem; margin-bottom: 1rem; }
    .form-group label { font-weight: 600; font-size: 0.875rem; color: #334155; }
    .form-input { padding: 0.625rem; border: 1px solid #cbd5e1; border-radius: 8px; font-size: 0.875rem; }
    .modal-actions-bar { width: 100%; display: flex; gap: 0.75rem; justify-content: flex-end; }
  `]
})
export class SettlementPeriodsComponent implements OnInit {
  workforceService = inject(WorkforceService);

  isCreateModalOpen = false;
  isSummaryModalOpen = false;
  summary: SettlementCalculationSummary | null = null;

  createForm = { periodCode: '', startDate: '2026-08-01', endDate: '2026-08-15', cycleType: 'HALF_MONTH' };

  ngOnInit() {
    this.workforceService.loadSettlementPeriods().subscribe();
  }

  openCreateModal() {
    this.createForm = { periodCode: 'PER-' + Math.floor(100 + Math.random() * 900), startDate: '2026-08-01', endDate: '2026-08-15', cycleType: 'HALF_MONTH' };
    this.isCreateModalOpen = true;
  }

  savePeriod() {
    this.workforceService.createSettlementPeriod(this.createForm).subscribe(() => this.isCreateModalOpen = false);
  }

  calculatePeriod(id: string) {
    this.workforceService.calculatePeriod(id).subscribe(res => {
      this.summary = res;
      this.isSummaryModalOpen = true;
    });
  }

  approvePeriod(id: string) {
    this.workforceService.approvePeriod(id).subscribe(() => alert('تم اعتماد وقفل فترة التسوية بنجاح'));
  }

  exportExcel(id: string, code: string) {
    this.workforceService.exportSettlementPeriodExcel(id).subscribe(blob => {
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `Workforce_Settlement_Period_${code}.xlsx`;
      a.click();
      window.URL.revokeObjectURL(url);
    });
  }
}
