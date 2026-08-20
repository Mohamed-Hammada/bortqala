import { signal, WritableSignal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { AuthService } from '../../../../core/auth/auth.service';
import { I18nService } from '../../../../core/i18n.service';
import { NotificationService } from '../../../../core/notification.service';
import { ApprovalService } from '../../data-access/approval.service';
import { ApprovalTask } from '../../models/approval.models';
import { PendingApprovalsComponent } from './pending-approvals.component';

describe('PendingApprovalsComponent advanced approvals', () => {
  let component: PendingApprovalsComponent;
  let tasks: WritableSignal<ApprovalTask[]>;
  let createDelegation: ReturnType<typeof vi.fn>;
  let error: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    tasks = signal<ApprovalTask[]>([]);
    createDelegation = vi.fn(() => of({}));
    error = vi.fn();
    const approvalService = {
      myTasks: tasks, delegations: signal([]), loading: signal(false), loadMyTasks: vi.fn(() => of([])),
      loadDelegations: vi.fn(() => of([])), createDelegation, deactivateDelegation: vi.fn(() => of(undefined)),
      approveStep: vi.fn(() => of({})), rejectStep: vi.fn(() => of({})), getApprovalHistory: vi.fn(() => of({})),
    };
    await TestBed.configureTestingModule({ imports: [PendingApprovalsComponent], providers: [
      { provide: ApprovalService, useValue: approvalService },
      { provide: I18nService, useValue: { t: (key: string) => key } },
      { provide: NotificationService, useValue: { success: vi.fn(), error } },
      { provide: AuthService, useValue: { user: () => ({ username: 'owner' }) } },
    ] }).compileComponents();
    component = TestBed.createComponent(PendingApprovalsComponent).componentInstance;
  });

  it('summarizes and filters overdue and delegated tasks', () => {
    tasks.set([task('one', true), { ...task('two', false), delegatedFrom: 'owner' }]);
    expect(component.summary()).toEqual({ total: 2, overdue: 1, delegated: 1 });
    component.filter.set('OVERDUE');
    expect(component.visibleTasks().map(x => x.instanceId)).toEqual(['one']);
    component.filter.set('DELEGATED');
    expect(component.visibleTasks().map(x => x.instanceId)).toEqual(['two']);
  });

  it('blocks incomplete delegation dates before calling the API', () => {
    component.delegationForm = { delegateUserId: 'backup', documentType: '', startsAt: '', endsAt: '', reason: 'leave' };
    component.saveDelegation();
    expect(createDelegation).not.toHaveBeenCalled();
    expect(error).toHaveBeenCalledWith('approvals.delegationRequired');
  });

  it('sends the signed-in user and date window when delegating', () => {
    component.delegationForm = { delegateUserId: 'backup', documentType: 'PURCHASE_ORDER', startsAt: '2026-08-10T08:00', endsAt: '2026-08-12T08:00', reason: 'leave' };
    component.saveDelegation();
    expect(createDelegation).toHaveBeenCalledWith(expect.objectContaining({ delegatorUserId: 'owner', delegateUserId: 'backup', documentType: 'PURCHASE_ORDER' }));
  });

  it('navigates to the corresponding document workbench when openDocument is clicked', () => {
    const router = (component as any).router;
    const navigateSpy = vi.spyOn(router, 'navigate');

    component.openDocument(task('PO-101', false));
    expect(navigateSpy).toHaveBeenCalledWith(['/trade/procurement'], { queryParams: { po: 'PO-101' } });

    component.openDocument({ ...task('PRJ-202', false), documentType: 'PROJECT_LIFECYCLE' });
    expect(navigateSpy).toHaveBeenCalledWith(['/projects', 'PRJ-202']);

    component.openDocument({ ...task('VO-303', false), documentType: 'VARIATION_ORDER' });
    expect(navigateSpy).toHaveBeenCalledWith(['/projects', 'VO-303'], { queryParams: { tab: 'claims' } });
  });

  function task(id: string, overdue: boolean): ApprovalTask {
    return { instanceId: id, documentType: 'PURCHASE_ORDER', documentId: id, currentStepOrder: 1,
      stepName: 'Manager', status: 'SUBMITTED', submittedBy: 'requester', submittedAt: 1, overdue,
      escalationLevel: overdue ? 1 : 0, approvalsReceived: 0, approvalsRequired: 2 };
  }
});
