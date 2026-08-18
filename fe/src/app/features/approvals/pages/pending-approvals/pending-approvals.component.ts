import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApprovalService } from '../../data-access/approval.service';
import { ApprovalTask, ApprovalInstanceDetail } from '../../models/approval.models';
import { ModalDialogComponent } from '../../../../shared/ui/modal-dialog/modal-dialog.component';
import { NotificationService } from '../../../../core/notification.service';
import { I18nService } from '../../../../core/i18n.service';
import { AuthService } from '../../../../core/auth/auth.service';
import { apiErrorDetail } from '../../../../core/api-error';

@Component({
  selector: 'app-pending-approvals',
  standalone: true,
  imports: [CommonModule, FormsModule, ModalDialogComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './pending-approvals.component.html',
  styleUrls: ['./pending-approvals.component.scss']
})
export class PendingApprovalsComponent implements OnInit {
  readonly approvalService = inject(ApprovalService);
  readonly i18n = inject(I18nService);
  private readonly notification = inject(NotificationService);
  private readonly auth = inject(AuthService);
  readonly decisionModalOpen = signal(false);
  readonly historyModalOpen = signal(false);
  readonly delegationModalOpen = signal(false);
  readonly submitting = signal(false);
  readonly delegating = signal(false);
  readonly decisionAction = signal<'APPROVE' | 'REJECT'>('APPROVE');
  readonly selectedTask = signal<ApprovalTask | null>(null);
  readonly historyDetail = signal<ApprovalInstanceDetail | null>(null);
  readonly pageError = signal<string | null>(null);
  readonly filter = signal<'ALL' | 'OVERDUE' | 'DELEGATED'>('ALL');
  readonly visibleTasks = computed(() => this.approvalService.myTasks().filter(task => this.filter() === 'ALL' || this.filter() === 'OVERDUE' && task.overdue || this.filter() === 'DELEGATED' && !!task.delegatedFrom));
  readonly summary = computed(() => ({ total: this.approvalService.myTasks().length, overdue: this.approvalService.myTasks().filter(x => x.overdue).length, delegated: this.approvalService.myTasks().filter(x => !!x.delegatedFrom).length }));
  decisionComment = '';
  delegationForm = { delegateUserId: '', documentType: '', startsAt: '', endsAt: '', reason: '' };

  ngOnInit(): void { this.reload(); this.approvalService.loadDelegations().subscribe({ error: err => this.pageError.set(apiErrorDetail(err, this.i18n.t('approvals.loadFailed'))) }); }
  reload(): void { this.pageError.set(null); this.approvalService.loadMyTasks().subscribe({ error: err => this.pageError.set(apiErrorDetail(err, this.i18n.t('approvals.loadFailed'))) }); }
  openDecisionModal(task: ApprovalTask, action: 'APPROVE' | 'REJECT'): void { this.selectedTask.set(task); this.decisionAction.set(action); this.decisionComment = ''; this.decisionModalOpen.set(true); }
  openHistoryModal(task: ApprovalTask): void { this.approvalService.getApprovalHistory(task.documentType, task.documentId).subscribe({ next: detail => { this.historyDetail.set(detail); this.historyModalOpen.set(true); }, error: err => this.notification.error(apiErrorDetail(err, this.i18n.t('approvals.historyLoadFailed'))) }); }
  submitDecision(): void {
    const task = this.selectedTask(); if (!task) return;
    if (this.decisionAction() === 'REJECT' && !this.decisionComment.trim()) { this.notification.error(this.i18n.t('approvals.rejectionReasonRequired')); return; }
    this.submitting.set(true);
    const request = this.decisionAction() === 'APPROVE' ? this.approvalService.approveStep(task.instanceId, this.decisionComment) : this.approvalService.rejectStep(task.instanceId, this.decisionComment);
    request.subscribe({ next: () => { this.submitting.set(false); this.decisionModalOpen.set(false); this.notification.success(this.i18n.t('approvals.decisionSaved')); this.reload(); }, error: err => { this.submitting.set(false); this.notification.error(apiErrorDetail(err, this.i18n.t('approvals.decisionFailed'))); } });
  }
  saveDelegation(): void {
    const startsAt = Date.parse(this.delegationForm.startsAt), endsAt = Date.parse(this.delegationForm.endsAt);
    if (!this.delegationForm.delegateUserId.trim() || !this.delegationForm.reason.trim() || !Number.isFinite(startsAt) || !Number.isFinite(endsAt)) { this.notification.error(this.i18n.t('approvals.delegationRequired')); return; }
    this.delegating.set(true);
    this.approvalService.createDelegation({ delegatorUserId: this.auth.user()?.username ?? '', delegateUserId: this.delegationForm.delegateUserId, documentType: this.delegationForm.documentType || undefined, startsAt, endsAt, reason: this.delegationForm.reason }).subscribe({ next: () => { this.delegating.set(false); this.delegationForm = { delegateUserId: '', documentType: '', startsAt: '', endsAt: '', reason: '' }; this.notification.success(this.i18n.t('approvals.delegationSaved')); }, error: err => { this.delegating.set(false); this.notification.error(apiErrorDetail(err, this.i18n.t('approvals.delegationFailed'))); } });
  }
  deactivate(id: string): void { this.approvalService.deactivateDelegation(id).subscribe({ next: () => this.notification.success(this.i18n.t('approvals.delegationDeactivated')), error: err => this.notification.error(apiErrorDetail(err, this.i18n.t('approvals.delegationFailed'))) }); }
  documentTypeLabel(type: string): string { return this.i18n.t(`approvals.documentType.${type}`); }
}
