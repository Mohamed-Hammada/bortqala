import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { AuthService } from '../../../../core/auth/auth.service';
import { apiErrorDetail } from '../../../../core/api-error';
import { I18nService } from '../../../../core/i18n.service';
import { NotificationService } from '../../../../core/notification.service';
import { WorkforceService } from '../../data-access/workforce.service';
import { LaborDispatch, WorkforceDispute, WorkerAssignment } from '../../models/workforce.models';

@Component({
  selector: 'app-dispatch-disputes',
  imports: [FormsModule],
  templateUrl: './dispatch-disputes.component.html',
  styleUrl: './dispatch-disputes.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DispatchDisputesComponent implements OnInit {
  readonly workforceService = inject(WorkforceService);
  readonly i18n = inject(I18nService);
  private readonly authService = inject(AuthService);
  private readonly notificationService = inject(NotificationService);

  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly pageError = signal<string | null>(null);
  readonly selectedDispatchId = signal('');
  readonly assignments = signal<WorkerAssignment[]>([]);
  readonly selectedPeriodId = signal('');
  readonly disputes = signal<WorkforceDispute[]>([]);

  readonly canManageDispatch = computed(() => this.authService.hasAnyRole(['WORKFORCE_MANAGER']));
  readonly canManageDisputes = computed(() => this.authService.hasAnyRole(['WORKFORCE_MANAGER', 'WORKFORCE_FINANCE', 'FINANCE_MANAGER']));
  readonly canResolveDisputes = computed(() => this.authService.hasAnyRole(['FINANCE_MANAGER']));

  dispatchForm = { requestId: '', contractorId: '', dispatchDate: this.today() };
  assignmentForm = { workerId: '', requestLineId: '', fromDate: this.today(), toDate: this.today(), agreedRate: 0, agreedHours: 8 };
  disputeForm = { contractorId: '', disputedAmount: 0, reason: '' };

  ngOnInit(): void {
    this.loading.set(true);
    forkJoin({
      dispatches: this.workforceService.loadDispatches(),
      requests: this.workforceService.loadLaborRequests(),
      contractors: this.workforceService.loadContractors(),
      workers: this.workforceService.loadWorkers(),
      periods: this.workforceService.loadSettlementPeriods(),
    }).subscribe({
      next: () => this.loading.set(false),
      error: error => this.fail(error),
    });
  }

  createDispatch(): void {
    if (!this.dispatchForm.requestId || !this.dispatchForm.contractorId || !this.dispatchForm.dispatchDate) return;
    this.run(() => this.workforceService.createDispatch(this.dispatchForm), () => {
      this.dispatchForm = { requestId: '', contractorId: '', dispatchDate: this.today() };
      this.notificationService.success(this.i18n.t('workforce.dispatch.created'));
    });
  }

  transitionDispatch(item: LaborDispatch, action: 'dispatch' | 'accept' | 'cancel'): void {
    this.run(() => this.workforceService.transitionDispatch(item.id, action), updated => {
      this.notificationService.success(this.i18n.t(`workforce.dispatch.${updated.status.toLowerCase()}`));
    });
  }

  selectDispatch(id: string): void {
    this.selectedDispatchId.set(id);
    this.assignments.set([]);
    const dispatch = this.workforceService.dispatches().find(item => item.id === id);
    if (dispatch) this.assignmentForm = { ...this.assignmentForm };
    if (!id) return;
    this.workforceService.loadAssignments(id).subscribe({
      next: items => this.assignments.set(items),
      error: error => this.fail(error),
    });
  }

  addAssignment(): void {
    const dispatch = this.selectedDispatch();
    if (!dispatch || !this.assignmentForm.workerId) return;
    this.run(() => this.workforceService.assignWorker(dispatch.id, {
      ...this.assignmentForm,
      requestLineId: this.assignmentForm.requestLineId || undefined,
      contractorId: dispatch.contractorId,
    }), assignment => {
      this.assignments.update(items => [...items, assignment]);
      this.notificationService.success(this.i18n.t('workforce.assignment.created'));
    });
  }

  transitionAssignment(item: WorkerAssignment, action: 'accept' | 'reject'): void {
    const reason = action === 'reject' ? window.prompt(this.i18n.t('workforce.assignment.rejectReason')) : undefined;
    if (action === 'reject' && !reason) return;
    this.run(() => this.workforceService.transitionAssignment(item.id, action, reason ?? undefined), updated => {
      this.assignments.update(items => items.map(current => current.id === updated.id ? updated : current));
      this.notificationService.success(this.i18n.t('common.saved'));
    });
  }

  selectPeriod(id: string): void {
    this.selectedPeriodId.set(id);
    this.disputes.set([]);
    if (!id) return;
    this.workforceService.loadDisputes(id).subscribe({
      next: items => this.disputes.set(items),
      error: error => this.fail(error),
    });
  }

  createDispute(): void {
    const periodId = this.selectedPeriodId();
    if (!periodId || !this.disputeForm.contractorId || !this.disputeForm.reason || this.disputeForm.disputedAmount <= 0) return;
    this.run(() => this.workforceService.createDispute(periodId, this.disputeForm), dispute => {
      this.disputes.update(items => [...items, dispute]);
      this.disputeForm = { contractorId: '', disputedAmount: 0, reason: '' };
      this.notificationService.success(this.i18n.t('workforce.dispute.created'));
    });
  }

  transitionDispute(item: WorkforceDispute, action: 'submit' | 'resolve' | 'reject'): void {
    const notes = action === 'submit' ? undefined : window.prompt(this.i18n.t(`workforce.dispute.${action}Notes`));
    if (action !== 'submit' && !notes) return;
    this.run(() => this.workforceService.transitionDispute(item.id, action, notes ?? undefined), updated => {
      this.disputes.update(items => items.map(current => current.id === updated.id ? updated : current));
      this.notificationService.success(this.i18n.t('common.saved'));
    });
  }

  selectedDispatch(): LaborDispatch | undefined {
    return this.workforceService.dispatches().find(item => item.id === this.selectedDispatchId());
  }

  requestLabel(id: string): string {
    return this.workforceService.laborRequests().find(item => item.id === id)?.requestNumber ?? id;
  }

  contractorLabel(id: string): string {
    return this.workforceService.contractors().find(item => item.id === id)?.name ?? id;
  }

  workerLabel(id: string): string {
    return this.workforceService.workers().find(item => item.id === id)?.fullName ?? id;
  }

  dispatchStatusLabel(status: LaborDispatch['status']): string {
    const keys: Record<LaborDispatch['status'], string> = {
      DRAFT: 'workforce.dispatch.status.DRAFT',
      DISPATCHED: 'workforce.dispatch.status.DISPATCHED',
      ACCEPTED: 'workforce.dispatch.status.ACCEPTED',
      CANCELLED: 'workforce.dispatch.status.CANCELLED',
    };
    return this.i18n.t(keys[status] ?? 'common.unknown');
  }

  assignmentStatusLabel(status: WorkerAssignment['status']): string {
    const keys: Record<WorkerAssignment['status'], string> = {
      PROPOSED: 'workforce.assignment.status.PROPOSED',
      ACCEPTED: 'workforce.assignment.status.ACCEPTED',
      REJECTED: 'workforce.assignment.status.REJECTED',
      REPLACED: 'workforce.assignment.status.REPLACED',
      COMPLETED: 'workforce.assignment.status.COMPLETED',
    };
    return this.i18n.t(keys[status] ?? 'common.unknown');
  }

  disputeStatusLabel(status: WorkforceDispute['status']): string {
    const keys: Record<WorkforceDispute['status'], string> = {
      DRAFT: 'workforce.dispute.status.DRAFT',
      UNDER_REVIEW: 'workforce.dispute.status.UNDER_REVIEW',
      RESOLVED: 'workforce.dispute.status.RESOLVED',
      REJECTED: 'workforce.dispute.status.REJECTED',
    };
    return this.i18n.t(keys[status] ?? 'common.unknown');
  }

  private run<T>(operation: () => import('rxjs').Observable<T>, next: (value: T) => void): void {
    if (this.saving()) return;
    this.saving.set(true);
    operation().subscribe({
      next: value => { this.saving.set(false); next(value); },
      error: error => this.fail(error),
    });
  }

  private fail(error: unknown): void {
    this.loading.set(false);
    this.saving.set(false);
    const message = apiErrorDetail(error, this.i18n.t('common.loadError'));
    this.pageError.set(message);
    this.notificationService.error(message);
  }

  private today(): string {
    return new Date().toISOString().slice(0, 10);
  }
}
