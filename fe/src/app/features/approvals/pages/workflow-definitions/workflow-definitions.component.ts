import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApprovalService } from '../../data-access/approval.service';
import { ApprovalWorkflowDefinition, ApprovalWorkflowStep } from '../../models/approval.models';
import { ModalDialogComponent } from '../../../../shared/ui/modal-dialog/modal-dialog.component';
import { NotificationService } from '../../../../core/notification.service';
import { I18nService } from '../../../../core/i18n.service';
import { apiErrorDetail } from '../../../../core/api-error';

@Component({
  selector: 'app-workflow-definitions',
  standalone: true,
  imports: [CommonModule, FormsModule, ModalDialogComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="approvals-container" dir="rtl">
      <header class="page-header">
        <div>
          <span class="eyebrow">مُحرك الاعتمادات المتعددة</span>
          <h1>مسارات الاعتماد وتحديد الصلاحيات</h1>
          <p>تحديد وإدارة خطوات الاعتماد لكل نوع مستند داخل النظام.</p>
        </div>
        <button type="button" class="btn primary" (click)="openCreateModal()">＋ إنشاء مسار جديد</button>
      </header>

      @if (pageError()) { <div class="alert error">{{ pageError() }}</div> }
      @if (approvalService.loading()) { <div class="alert">جارٍ تحميل مسارات الاعتماد…</div> }

      <div class="card table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>المستند</th>
              <th>اسم المسار</th>
              <th>عدد الخطوات</th>
              <th>الإصدار</th>
              <th>الحالة</th>
              <th>تاريخ التحديث</th>
              <th>الإجراءات</th>
            </tr>
          </thead>
          <tbody>
            @for (def of approvalService.definitions(); track def.id) {
              <tr>
                <td><strong>{{ documentTypeLabel(def.documentType) }}</strong></td>
                <td>{{ def.name }}</td>
                <td><span class="step-count">{{ def.steps?.length || 0 }} خطوات</span></td>
                <td>v{{ def.version }}</td>
                <td><span class="badge" [class.active]="def.active">{{ def.active ? 'مفعل' : 'معطل' }}</span></td>
                <td>{{ def.updatedAt | date:'yyyy-MM-dd HH:mm' }}</td>
                <td class="actions">
                  <button type="button" class="btn secondary" (click)="openEditModal(def)">✏ تعديل الخطوات</button>
                </td>
              </tr>
            } @empty {
              <tr><td colspan="7" class="empty">لا توجد مسارات اعتماد معرفة حتى الآن.</td></tr>
            }
          </tbody>
        </table>
      </div>

      <!-- Create / Edit Workflow Modal -->
      <app-modal-dialog [isOpen]="formModalOpen()" [title]="editingId() ? 'تعديل مسار الاعتماد' : 'إنشاء مسار اعتماد جديد'" size="wide" (close)="formModalOpen.set(false)">
        <form class="form-grid" dir="rtl">
          <label>
            نوع المستند *
            <select [(ngModel)]="form.documentType" name="docType" [disabled]="!!editingId()" required>
              <option value="PURCHASE_ORDER">أمر شراء (PURCHASE_ORDER)</option>
              <option value="CONTRACTOR_SETTLEMENT">تسوية مقاول (CONTRACTOR_SETTLEMENT)</option>
              <option value="PAYROLL_RUN">مسير رواتب (PAYROLL_RUN)</option>
              <option value="SUPPLIER_INVOICE">فاتورة مورد (SUPPLIER_INVOICE)</option>
              <option value="SUPPLIER_PAYMENT">سداد مورد (SUPPLIER_PAYMENT)</option>
              <option value="JOURNAL_ENTRY">قيد محاسبي (JOURNAL_ENTRY)</option>
              <option value="LABOR_REQUEST">طلب عمالة (LABOR_REQUEST)</option>
              <option value="ADVANCE">سلفة عمالة (ADVANCE)</option>
            </select>
          </label>

          <label>
            اسم المسار *
            <input [(ngModel)]="form.name" name="wfName" required placeholder="مثال: مسار اعتمادات أوامر الشراء" />
          </label>

          <label class="checkbox-label">
            <input type="checkbox" [(ngModel)]="form.active" name="active" />
            تفعيل المسار مباشرة
          </label>

          <div class="steps-builder">
            <div class="builder-header">
              <h3>خطوات الاعتماد المتتابعة</h3>
              <button type="button" class="btn secondary" (click)="addStep()">＋ إضافة خطوة</button>
            </div>

            @for (step of form.steps; track $index) {
              <div class="step-card">
                <span class="step-num">#{{ step.stepOrder }}</span>
                <div class="step-fields">
                  <input [(ngModel)]="step.name" [name]="'stepName_' + $index" placeholder="اسم الخطوة (مثال: اعتماد المدير المالي)" required />
                  <select [(ngModel)]="step.requiredRole" [name]="'stepRole_' + $index">
                    <option value="">اختر الدور المطلوب…</option>
                    <option value="SUPER_ADMIN">SUPER_ADMIN</option>
                    <option value="ADMIN">ADMIN</option>
                    <option value="WORKFORCE_MANAGER">WORKFORCE_MANAGER</option>
                    <option value="WORKFORCE_FINANCE">WORKFORCE_FINANCE</option>
                    <option value="FINANCE_MANAGER">FINANCE_MANAGER</option>
                    <option value="PROCUREMENT_MANAGER">PROCUREMENT_MANAGER</option>
                    <option value="PAYROLL_MANAGER">PAYROLL_MANAGER</option>
                    <option value="HR_MANAGER">HR_MANAGER</option>
                  </select>
                  <label>{{ i18n.t('approvals.minimumApprovals') }}<input type="number" min="1" [(ngModel)]="step.minimumApprovals" [name]="'minimum_' + $index" /></label>
                  <label>{{ i18n.t('approvals.escalationHours') }}<input type="number" min="1" [(ngModel)]="step.escalationHours" [name]="'sla_' + $index" /></label>
                </div>
                <button type="button" class="btn danger" (click)="removeStep($index)">🗑</button>
              </div>
            } @empty {
              <p class="empty">اضغط إضافة خطوة لبناء مسار الاعتماد المتتابع.</p>
            }
          </div>
        </form>

        <div modal-actions>
          <button type="button" class="btn primary" [disabled]="submitting()" (click)="saveWorkflow()">
            {{ submitting() ? 'جارٍ الحفظ…' : 'حفظ المسار' }}
          </button>
          <button type="button" class="btn secondary" (click)="formModalOpen.set(false)">إلغاء</button>
        </div>
      </app-modal-dialog>
    </section>
  `,
  styles: [`
    .approvals-container{padding:1.5rem;display:grid;gap:1.25rem}.page-header{display:flex;justify-content:space-between;align-items:center}.page-header h1{margin:.2rem 0}.eyebrow{color:#b7791f;font-weight:800}.card{background:#fff;border:1px solid #e2e8f0;border-radius:14px}.table-wrap{overflow:auto}.data-table{width:100%;border-collapse:collapse;min-width:900px}.data-table th,.data-table td{padding:.75rem;border-bottom:1px solid #edf0f4;text-align:right}.step-count{background:#fef3c7;color:#92400e;padding:.25rem .6rem;border-radius:999px;font-weight:700}.badge{padding:.25rem .6rem;border-radius:999px;background:#fee2e2;color:#991b1b}.badge.active{background:#dcfce7;color:#166534}.btn{border:0;border-radius:8px;padding:.55rem .75rem;font-weight:700;cursor:pointer;background:#e8edf3;color:#243247}.primary{background:#b7791f;color:#fff}.secondary{background:#e8edf3}.danger{background:#fee2e2;color:#991b1b}.alert{padding:.8rem;border-radius:10px;background:#eff6ff}.alert.error{background:#fef2f2;color:#991b1b}.form-grid{display:grid;gap:1rem}.form-grid label{display:grid;gap:.35rem;font-weight:700}.form-grid input,.form-grid select{padding:.65rem;border:1px solid #cbd5e1;border-radius:8px}.checkbox-label{display:flex;align-items:center;gap:.5rem;cursor:pointer}.steps-builder{border:1px solid #e2e8f0;border-radius:10px;padding:1rem;display:grid;gap:.75rem}.builder-header{display:flex;justify-content:space-between;align-items:center}.builder-header h3{margin:0;font-size:1.05rem}.step-card{display:flex;gap:.75rem;align-items:center;background:#fafaf9;padding:.65rem;border:1px solid #e7e5e4;border-radius:8px}.step-num{font-weight:800;color:#b7791f}.step-fields{display:grid;grid-template-columns:1fr 1fr;gap:.5rem;flex:1}.empty{text-align:center;color:#64748b;padding:1rem}
  `]
})
export class WorkflowDefinitionsComponent implements OnInit {
  readonly approvalService = inject(ApprovalService);
  readonly i18n = inject(I18nService);
  private readonly notification = inject(NotificationService);

  readonly formModalOpen = signal(false);
  readonly submitting = signal(false);
  readonly editingId = signal<string | null>(null);
  readonly pageError = signal<string | null>(null);

  form = {
    documentType: 'PURCHASE_ORDER',
    name: '',
    active: true,
    steps: [] as ApprovalWorkflowStep[]
  };

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.approvalService.loadWorkflowDefinitions().subscribe({
      error: err => this.pageError.set(apiErrorDetail(err, 'تعذر تحميل مسارات الاعتماد.'))
    });
  }

  openCreateModal(): void {
    this.editingId.set(null);
    this.form = {
      documentType: 'PURCHASE_ORDER',
      name: '',
      active: true,
      steps: [
        { stepOrder: 1, stepCode: 'STEP_1', name: this.i18n.t('approvals.defaultFirstStep'), requiredRole: 'PROCUREMENT_MANAGER', minimumApprovals: 1, allowSelfApproval: false, decisionPolicy: 'ANY_N' }
      ]
    };
    this.formModalOpen.set(true);
  }

  openEditModal(def: ApprovalWorkflowDefinition): void {
    this.editingId.set(def.id);
    this.form = {
      documentType: def.documentType,
      name: def.name,
      active: def.active,
      steps: def.steps ? def.steps.map(s => ({ ...s })) : []
    };
    this.formModalOpen.set(true);
  }

  addStep(): void {
    const nextOrder = this.form.steps.length + 1;
    this.form.steps.push({
      stepOrder: nextOrder,
      stepCode: `STEP_${nextOrder}`,
      name: `الخطوة ${nextOrder}`,
      requiredRole: 'FINANCE_MANAGER',
      minimumApprovals: 1,
      allowSelfApproval: false,
      decisionPolicy: 'ANY_N'
    });
  }

  removeStep(index: number): void {
    this.form.steps.splice(index, 1);
    this.form.steps.forEach((s, idx) => s.stepOrder = idx + 1);
  }

  saveWorkflow(): void {
    if (!this.form.name || !this.form.steps.length) {
      this.notification.error(this.i18n.t('approvals.workflowNameRequired'));
      return;
    }

    this.submitting.set(true);
    const action$ = this.editingId()
      ? this.approvalService.updateWorkflowDefinition(this.editingId()!, this.form)
      : this.approvalService.createWorkflowDefinition(this.form);

    action$.subscribe({
      next: () => {
        this.submitting.set(false);
        this.formModalOpen.set(false);
        this.notification.success(this.i18n.t('approvals.workflowSavedSuccess'));
        this.reload();
      },
      error: err => {
        this.submitting.set(false);
        this.notification.error(apiErrorDetail(err, 'تعذر حفظ المسار.'));
      }
    });
  }

  documentTypeLabel(type: string): string {
    return ({
      PURCHASE_ORDER: 'أمر شراء',
      CONTRACTOR_SETTLEMENT: 'تسوية مقاول',
      PAYROLL_RUN: 'مسير رواتب',
      SUPPLIER_INVOICE: 'فاتورة مورد',
      SUPPLIER_PAYMENT: 'سداد مورد',
      JOURNAL_ENTRY: 'قيد محاسبي',
      LABOR_REQUEST: 'طلب عمالة',
      ADVANCE: 'سلفة عمالة'
    } as Record<string, string>)[type] ?? type;
  }
}
