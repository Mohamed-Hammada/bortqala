import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApprovalService } from '../../data-access/approval.service';
import { ApprovalTask, ApprovalInstanceDetail } from '../../models/approval.models';
import { ModalDialogComponent } from '../../../../shared/ui/modal-dialog/modal-dialog.component';
import { NotificationService } from '../../../../core/notification.service';
import { I18nService } from '../../../../core/i18n.service';
import { apiErrorDetail } from '../../../../core/api-error';

@Component({
  selector: 'app-pending-approvals',
  standalone: true,
  imports: [CommonModule, FormsModule, ModalDialogComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="approvals-container" dir="rtl">
      <header class="page-header">
        <div>
          <span class="eyebrow">مركز إجراءات الاعتماد</span>
          <h1>طلبات الاعتماد المعلقة</h1>
          <p>مراجعة واتخاذ القرار بشأن طلبات وأوامر العمل الواردة.</p>
        </div>
      </header>

      @if (pageError()) { <div class="alert error">{{ pageError() }}</div> }
      @if (approvalService.loading()) { <div class="alert">جارٍ تحميل المهام المعلقة…</div> }

      <div class="card table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>المستند</th>
              <th>رقم المستند</th>
              <th>الخطوة الحالية</th>
              <th>الصلاحية المطلوبة</th>
              <th>طالب الاعتماد</th>
              <th>تاريخ الطلب</th>
              <th>الإجراءات</th>
            </tr>
          </thead>
          <tbody>
            @for (task of approvalService.myTasks(); track task.instanceId) {
              <tr>
                <td><strong>{{ documentTypeLabel(task.documentType) }}</strong></td>
                <td><code>{{ task.documentId }}</code></td>
                <td><span class="step-badge">الخطوة {{ task.currentStepOrder }}: {{ task.stepName }}</span></td>
                <td><span class="role-badge">{{ task.requiredRole || 'مخصص' }}</span></td>
                <td>{{ task.submittedBy }}</td>
                <td>{{ task.submittedAt | date:'yyyy-MM-dd HH:mm' }}</td>
                <td class="actions">
                  <button type="button" class="btn success" (click)="openDecisionModal(task, 'APPROVE')">✔ اعتماد</button>
                  <button type="button" class="btn danger" (click)="openDecisionModal(task, 'REJECT')">✖ رفض</button>
                  <button type="button" class="btn secondary" (click)="openHistoryModal(task)">📜 السجل</button>
                </td>
              </tr>
            } @empty {
              <tr><td colspan="7" class="empty">لا توجد طلبات اعتماد معلقة بانتظارك.</td></tr>
            }
          </tbody>
        </table>
      </div>

      <!-- Decision Action Modal -->
      <app-modal-dialog [isOpen]="decisionModalOpen()" [title]="decisionAction() === 'APPROVE' ? 'تأكيد اعتماد المستند' : 'تأكيد رفض المستند'" (close)="decisionModalOpen.set(false)">
        @if (selectedTask(); as task) {
          <div class="decision-form" dir="rtl">
            <p>المستند: <strong>{{ documentTypeLabel(task.documentType) }} ({{ task.documentId }})</strong></p>
            <p>الخطوة: <strong>{{ task.stepName }}</strong></p>

            <label>
              {{ decisionAction() === 'REJECT' ? 'سبب الرفض * (مطلوب)' : 'ملاحظات أو تعليق الاعتماد (اختياري)' }}
              <textarea [(ngModel)]="decisionComment" rows="3" [required]="decisionAction() === 'REJECT'" placeholder="أدخل الملاحظات هنا…"></textarea>
            </label>
          </div>
        }
        <div modal-actions>
          <button type="button" [class]="decisionAction() === 'APPROVE' ? 'btn success' : 'btn danger'" [disabled]="submitting()" (click)="submitDecision()">
            {{ submitting() ? 'جارٍ المعالجة…' : (decisionAction() === 'APPROVE' ? 'تأكيد الاعتماد' : 'تأكيد الرفض') }}
          </button>
          <button type="button" class="btn secondary" (click)="decisionModalOpen.set(false)">إلغاء</button>
        </div>
      </app-modal-dialog>

      <!-- History Modal -->
      <app-modal-dialog [isOpen]="historyModalOpen()" title="سجل موافقات المستند" size="wide" (close)="historyModalOpen.set(false)">
        @if (historyDetail(); as history) {
          <div class="history-list" dir="rtl">
            <p>حالة المستند: <strong [class.status-approved]="history.status === 'APPROVED'" [class.status-rejected]="history.status === 'REJECTED'">{{ history.status }}</strong></p>
            <table class="data-table">
              <thead><tr><th>القرار</th><th>بواسطة</th><th>التاريخ</th><th>الملاحظات / السبب</th></tr></thead>
              <tbody>
                @for (dec of history.history; track dec.id) {
                  <tr>
                    <td><span class="badge" [class.approved]="dec.decision === 'APPROVED'" [class.rejected]="dec.decision === 'REJECTED'">{{ dec.decision }}</span></td>
                    <td>{{ dec.decidedBy }}</td>
                    <td>{{ dec.decidedAt | date:'yyyy-MM-dd HH:mm' }}</td>
                    <td>{{ dec.comment || '—' }}</td>
                  </tr>
                } @empty {
                  <tr><td colspan="4" class="empty">لا يوجد سجل قرارات حتى الآن.</td></tr>
                }
              </tbody>
            </table>
          </div>
        }
        <div modal-actions>
          <button type="button" class="btn secondary" (click)="historyModalOpen.set(false)">إغلاق</button>
        </div>
      </app-modal-dialog>
    </section>
  `,
  styles: [`
    .approvals-container{padding:1.5rem;display:grid;gap:1.25rem}.page-header h1{margin:.2rem 0}.eyebrow{color:#b7791f;font-weight:800}.card{background:#fff;border:1px solid #e2e8f0;border-radius:14px}.table-wrap{overflow:auto}.data-table{width:100%;border-collapse:collapse;min-width:900px}.data-table th,.data-table td{padding:.75rem;border-bottom:1px solid #edf0f4;text-align:right}.step-badge{background:#e0f2fe;color:#0369a1;padding:.2rem .5rem;border-radius:6px;font-weight:700}.role-badge{background:#f3e8ff;color:#6b21a8;padding:.2rem .5rem;border-radius:6px;font-weight:600}.actions{display:flex;gap:.35rem}.btn{border:0;border-radius:8px;padding:.45rem .75rem;font-weight:700;cursor:pointer;background:#e8edf3;color:#243247}.btn:disabled{opacity:.5;cursor:not-allowed}.primary{background:#b7791f;color:#fff}.secondary{background:#e8edf3}.success{background:#dcfce7;color:#166534}.danger{background:#fee2e2;color:#991b1b}.alert{padding:.8rem;border-radius:10px;background:#eff6ff}.alert.error{background:#fef2f2;color:#991b1b}.decision-form{display:grid;gap:.75rem}.decision-form label{display:grid;gap:.35rem;font-weight:700}.decision-form textarea{padding:.65rem;border:1px solid #cbd5e1;border-radius:8px;resize:vertical}.empty{text-align:center;color:#64748b;padding:1rem}.status-approved,.badge.approved{color:#166534;font-weight:800}.status-rejected,.badge.rejected{color:#991b1b;font-weight:800}
  `]
})
export class PendingApprovalsComponent implements OnInit {
  readonly approvalService = inject(ApprovalService);
  readonly i18n = inject(I18nService);
  private readonly notification = inject(NotificationService);

  readonly decisionModalOpen = signal(false);
  readonly historyModalOpen = signal(false);
  readonly submitting = signal(false);
  readonly decisionAction = signal<'APPROVE' | 'REJECT'>('APPROVE');
  readonly selectedTask = signal<ApprovalTask | null>(null);
  readonly historyDetail = signal<ApprovalInstanceDetail | null>(null);
  readonly pageError = signal<string | null>(null);

  decisionComment = '';

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.approvalService.loadMyTasks().subscribe({
      error: err => this.pageError.set(apiErrorDetail(err, 'تعذر تحميل المهام المعلقة.'))
    });
  }

  openDecisionModal(task: ApprovalTask, action: 'APPROVE' | 'REJECT'): void {
    this.selectedTask.set(task);
    this.decisionAction.set(action);
    this.decisionComment = '';
    this.decisionModalOpen.set(true);
  }

  openHistoryModal(task: ApprovalTask): void {
    this.approvalService.getApprovalHistory(task.documentType, task.documentId).subscribe({
      next: detail => {
        this.historyDetail.set(detail);
        this.historyModalOpen.set(true);
      },
      error: err => this.notification.error(apiErrorDetail(err, 'تعذر تحميل سجل المواقفات.'))
    });
  }

  submitDecision(): void {
    const task = this.selectedTask();
    if (!task) return;

    if (this.decisionAction() === 'REJECT' && (!this.decisionComment || !this.decisionComment.trim())) {
      this.notification.error(this.i18n.t('approvals.rejectionReasonRequired'));
      return;
    }

    this.submitting.set(true);
    const action$ = this.decisionAction() === 'APPROVE'
      ? this.approvalService.approveStep(task.instanceId, this.decisionComment)
      : this.approvalService.rejectStep(task.instanceId, this.decisionComment);

    action$.subscribe({
      next: () => {
        this.submitting.set(false);
        this.decisionModalOpen.set(false);
        this.notification.success(this.decisionAction() === 'APPROVE' ? 'تم اعتماد الخطوة بنجاح.' : 'تم رفض الطلب.');
        this.reload();
      },
      error: err => {
        this.submitting.set(false);
        this.notification.error(apiErrorDetail(err, 'تعذر تنفيذ القرار.'));
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
