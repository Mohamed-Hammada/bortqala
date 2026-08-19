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
  templateUrl: './workflow-definitions.component.html',
  styleUrls: ['./workflow-definitions.component.scss']
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
      error: err => this.pageError.set(apiErrorDetail(err, this.i18n.t('approvals.loadError')))
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
      name: this.i18n.t('approvals.stepName', { order: String(nextOrder) }),
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
        this.notification.error(apiErrorDetail(err, this.i18n.t('approvals.saveError')));
      }
    });
  }

  documentTypeLabel(type: string): string {
    const key = ({
      PURCHASE_ORDER: 'approvals.docTypePO',
      CONTRACTOR_SETTLEMENT: 'approvals.docTypeCS',
      PAYROLL_RUN: 'approvals.docTypePR',
      SUPPLIER_INVOICE: 'approvals.docTypeSI',
      SUPPLIER_PAYMENT: 'approvals.docTypeSP',
      JOURNAL_ENTRY: 'approvals.docTypeJE',
      LABOR_REQUEST: 'approvals.docTypeLR',
      ADVANCE: 'approvals.docTypeADV',
    } as Record<string, string>)[type];
    return key ? this.i18n.t(key) : type;
  }
}
