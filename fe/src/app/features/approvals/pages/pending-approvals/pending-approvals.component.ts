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
  template: `
    <section class="approvals-container">
      <header class="page-header">
        <div><span class="eyebrow">{{ i18n.t('approvals.actionCenter') }}</span><h1>{{ i18n.t('approvals.pendingTitle') }}</h1></div>
        <button type="button" class="btn secondary" (click)="delegationModalOpen.set(true)">{{ i18n.t('approvals.manageDelegations') }}</button>
      </header>
      <div class="summary-grid">
        <article><strong>{{ summary().total }}</strong><span>{{ i18n.t('approvals.totalPending') }}</span></article>
        <article><strong>{{ summary().overdue }}</strong><span>{{ i18n.t('approvals.overdue') }}</span></article>
        <article><strong>{{ summary().delegated }}</strong><span>{{ i18n.t('approvals.delegatedToMe') }}</span></article>
      </div>
      <nav class="filters" [attr.aria-label]="i18n.t('approvals.filters')">
        <button type="button" [class.active]="filter() === 'ALL'" (click)="filter.set('ALL')">{{ i18n.t('common.all') }}</button>
        <button type="button" [class.active]="filter() === 'OVERDUE'" (click)="filter.set('OVERDUE')">{{ i18n.t('approvals.overdue') }}</button>
        <button type="button" [class.active]="filter() === 'DELEGATED'" (click)="filter.set('DELEGATED')">{{ i18n.t('approvals.delegated') }}</button>
      </nav>
      @if (pageError()) { <div class="alert error">{{ pageError() }}</div> }
      @if (approvalService.loading()) { <div class="alert">{{ i18n.t('approvals.loadingTasks') }}</div> }
      <div class="card table-wrap"><table class="data-table">
        <thead><tr><th>{{ i18n.t('approvals.document') }}</th><th>{{ i18n.t('approvals.currentStep') }}</th><th>{{ i18n.t('approvals.progress') }}</th><th>{{ i18n.t('approvals.age') }}</th><th>{{ i18n.t('common.actions') }}</th></tr></thead>
        <tbody>
          @for (task of visibleTasks(); track task.instanceId) {
            <tr [class.overdue-row]="task.overdue">
              <td><strong>{{ documentTypeLabel(task.documentType) }}</strong><small>{{ task.documentId }} · {{ task.submittedBy }}</small></td>
              <td>{{ task.stepName }} @if (task.delegatedFrom) { <span class="badge delegated">{{ i18n.t('approvals.from') }} {{ task.delegatedFrom }}</span> }</td>
              <td>{{ task.approvalsReceived }}/{{ task.approvalsRequired }}</td>
              <td>@if (task.overdue) { <span class="badge overdue">{{ i18n.t('approvals.overdue') }}</span> } @else { {{ task.submittedAt | date:'yyyy-MM-dd HH:mm' }} }</td>
              <td class="actions"><button type="button" class="btn success" (click)="openDecisionModal(task, 'APPROVE')">{{ i18n.t('common.approve') }}</button><button type="button" class="btn danger" (click)="openDecisionModal(task, 'REJECT')">{{ i18n.t('common.reject') }}</button><button type="button" class="btn secondary" (click)="openHistoryModal(task)">{{ i18n.t('approvals.history') }}</button></td>
            </tr>
          } @empty { <tr><td colspan="5" class="empty">{{ i18n.t('approvals.noPending') }}</td></tr> }
        </tbody>
      </table></div>

      <app-modal-dialog [isOpen]="decisionModalOpen()" [title]="decisionAction() === 'APPROVE' ? i18n.t('approvals.confirmApprove') : i18n.t('approvals.confirmReject')" (close)="decisionModalOpen.set(false)">
        @if (selectedTask(); as task) { <p>{{ documentTypeLabel(task.documentType) }} · {{ task.documentId }}</p><label class="field">{{ decisionAction() === 'REJECT' ? i18n.t('approvals.rejectionReason') : i18n.t('approvals.comment') }}<textarea [(ngModel)]="decisionComment" rows="3"></textarea></label> }
        <div modal-actions><button type="button" [class]="decisionAction() === 'APPROVE' ? 'btn success' : 'btn danger'" [disabled]="submitting()" (click)="submitDecision()">{{ i18n.t('common.confirm') }}</button><button type="button" class="btn secondary" (click)="decisionModalOpen.set(false)">{{ i18n.t('common.cancel') }}</button></div>
      </app-modal-dialog>

      <app-modal-dialog [isOpen]="historyModalOpen()" [title]="i18n.t('approvals.history')" size="wide" (close)="historyModalOpen.set(false)">
        @if (historyDetail(); as detail) { <div class="summary-line"><span>{{ i18n.t('approvals.definitionVersion') }}: {{ detail.workflowDefinitionVersion }}</span><span>{{ i18n.t('approvals.progress') }}: {{ detail.approvalsReceived }}/{{ detail.approvalsRequired }}</span><span>{{ i18n.t('common.status') }}: {{ detail.status }}</span></div><table class="data-table"><thead><tr><th>{{ i18n.t('approvals.decision') }}</th><th>{{ i18n.t('approvals.decidedBy') }}</th><th>{{ i18n.t('common.date') }}</th><th>{{ i18n.t('approvals.comment') }}</th></tr></thead><tbody>@for (item of detail.history; track item.id) { <tr><td>{{ item.decision }}</td><td>{{ item.decidedBy }} @if (item.delegatedFrom) { <small>{{ i18n.t('approvals.for') }} {{ item.delegatedFrom }}</small> }</td><td>{{ item.decidedAt | date:'yyyy-MM-dd HH:mm' }}</td><td>{{ item.comment || '—' }}</td></tr> } @empty { <tr><td colspan="4" class="empty">{{ i18n.t('approvals.noHistory') }}</td></tr> }</tbody></table> }
      </app-modal-dialog>

      <app-modal-dialog [isOpen]="delegationModalOpen()" [title]="i18n.t('approvals.manageDelegations')" size="wide" (close)="delegationModalOpen.set(false)">
        <form class="delegation-form" (ngSubmit)="saveDelegation()"><label>{{ i18n.t('approvals.delegateUser') }}<input name="delegate" [(ngModel)]="delegationForm.delegateUserId" required /></label><label>{{ i18n.t('approvals.documentType') }}<input name="type" [(ngModel)]="delegationForm.documentType" /></label><label>{{ i18n.t('common.from') }}<input name="starts" type="datetime-local" [(ngModel)]="delegationForm.startsAt" required /></label><label>{{ i18n.t('common.to') }}<input name="ends" type="datetime-local" [(ngModel)]="delegationForm.endsAt" required /></label><label class="wide">{{ i18n.t('common.reason') }}<input name="reason" [(ngModel)]="delegationForm.reason" required /></label><button class="btn primary" [disabled]="delegating()">{{ i18n.t('common.save') }}</button></form>
        <table class="data-table"><thead><tr><th>{{ i18n.t('approvals.delegateUser') }}</th><th>{{ i18n.t('approvals.documentType') }}</th><th>{{ i18n.t('common.period') }}</th><th>{{ i18n.t('common.status') }}</th><th>{{ i18n.t('common.actions') }}</th></tr></thead><tbody>@for (item of approvalService.delegations(); track item.id) { <tr><td>{{ item.delegateUserId }}</td><td>{{ item.documentType || i18n.t('common.all') }}</td><td>{{ item.startsAt | date:'yyyy-MM-dd' }} → {{ item.endsAt | date:'yyyy-MM-dd' }}</td><td>{{ item.active ? i18n.t('common.active') : i18n.t('common.inactive') }}</td><td><button type="button" class="btn danger" [disabled]="!item.active" (click)="deactivate(item.id)">{{ i18n.t('common.deactivate') }}</button></td></tr> } @empty { <tr><td colspan="5" class="empty">{{ i18n.t('approvals.noDelegations') }}</td></tr> }</tbody></table>
      </app-modal-dialog>
    </section>
  `,
  styles: [`
    .approvals-container{padding:1.5rem;display:grid;gap:1rem}.page-header{display:flex;justify-content:space-between;align-items:center}.page-header h1{margin:.2rem 0}.eyebrow{color:var(--gold);font-weight:800}.summary-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:.75rem}.summary-grid article{background:var(--surface);border:1px solid var(--line);border-radius:12px;padding:1rem;display:grid}.summary-grid strong{font-size:1.5rem}.summary-grid span,small{color:var(--muted);display:block}.filters{display:flex;gap:.5rem}.filters button,.btn{border:0;border-radius:8px;padding:.5rem .75rem;font-weight:700;cursor:pointer}.filters .active,.primary{background:var(--gold);color:#fff}.card{background:var(--surface);border:1px solid var(--line);border-radius:12px}.table-wrap{overflow:auto}.data-table{width:100%;border-collapse:collapse;min-width:760px}.data-table th,.data-table td{padding:.7rem;border-bottom:1px solid var(--line);text-align:start}.actions{display:flex;gap:.35rem}.success{background:var(--success-soft);color:var(--success-text)}.danger,.badge.overdue{background:var(--danger-soft);color:var(--danger-text)}.secondary{background:var(--surface-muted);color:inherit}.badge{padding:.2rem .45rem;border-radius:999px;font-size:.8rem}.delegated{background:var(--warning-soft);color:var(--warning-text)}.overdue-row{box-shadow:inset 3px 0 var(--danger-text)}.alert,.empty{padding:1rem;text-align:center}.alert.error{color:var(--danger-text)}.field{display:grid;gap:.4rem}.field textarea,.delegation-form input{padding:.6rem;border:1px solid var(--line);border-radius:8px;background:var(--input-bg);color:inherit}.summary-line,.delegation-form{display:grid;grid-template-columns:repeat(4,1fr);gap:.75rem;margin-bottom:1rem}.delegation-form label{display:grid;gap:.3rem}.delegation-form .wide{grid-column:span 3}@media(max-width:800px){.summary-grid,.delegation-form,.summary-line{grid-template-columns:1fr}.delegation-form .wide{grid-column:auto}}
  `]
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
