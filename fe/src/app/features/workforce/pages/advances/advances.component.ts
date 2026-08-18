import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { WorkforceService } from '../../data-access/workforce.service';
import { WorkforceAdvance, AdvanceRepayRequest, AdvancePolicy } from '../../models/workforce.models';
import { NotificationService } from '../../../../core/notification.service';
import { I18nService } from '../../../../core/i18n.service';
import { exportCsv } from '../../../../core/download';
import { apiErrorDetail } from '../../../../core/api-error';
import { ModalDialogComponent } from '../../../../shared/ui/modal-dialog/modal-dialog.component';
import { AppTooltipDirective } from '../../../../shared/ui/app-tooltip/app-tooltip.directive';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-advances',
  standalone: true,
  imports: [CommonModule, FormsModule, ModalDialogComponent, AppTooltipDirective],
  templateUrl: './advances.component.html',
  styleUrls: ['./advances.component.scss']
})
export class AdvancesComponent implements OnInit {
  workforceService = inject(WorkforceService);
  private notificationService = inject(NotificationService);
  readonly i18n = inject(I18nService);
  private route = inject(ActivatedRoute);

  loading = signal(false);
  saving = signal(false);
  isModalOpen = false;
  policyModalOpen = signal(false);
  policyForm: AdvancePolicy = this.defaultPolicyForm();
  effectivePolicyPreview = signal<AdvancePolicy | null>(null);

  // Repayment modal state
  repayModalOpen = signal(false);
  repayTarget = signal<WorkforceAdvance | null>(null);
  repayForm: {
    repaymentType: 'PARTIAL' | 'FULL';
    amount: number;
    repaymentDate: string;
    paymentMethod: string;
    receiptRef: string;
    notes: string;
  } = this.defaultRepayForm();

  repayPreview = computed(() => {
    const adv = this.repayTarget();
    if (!adv) return null;
    const before = adv.remainingBalance;
    const amount = this.repayForm.amount || 0;
    const after = Math.max(0, before - amount);
    const willClose = after <= 0;
    const impact = willClose ? this.i18n.t('workforce.ui.advances.repayImpactClose') : this.i18n.t('workforce.ui.advances.repayImpactRemaining', { amount: after });
    return { before, amount, after, willClose, impact };
  });

  form: {
    recipientType: string; workerId: string; contractorId: string; employeeId: string;
    amount: number; termType: string; totalInstallments: number;
    installmentAmount: number; deductionFrequency: string;
    maxDeductionPercent: number; reason: string;
    firstInstallmentDate: string; deductionMode: string; deferralPeriods: number;
  } = this.defaultForm();

  totalGranted = () => this.workforceService.advances().reduce((s, a) => s + (a.amount ?? 0), 0);
  totalRemaining = () => this.workforceService.advances().reduce((s, a) => s + (a.remainingBalance ?? 0), 0);
  employeeCategories = computed(() => {
    const unique = new Map<string, string>();
    this.workforceService.employees().forEach(employee => unique.set(employee.categoryId, employee.categoryName));
    return Array.from(unique, ([id, name]) => ({ id, name }));
  });

  ngOnInit() {
    this.loading.set(true);
    this.workforceService.loadAdvances().subscribe({
      next: () => this.loading.set(false),
      error: () => this.loading.set(false)
    });
    this.workforceService.loadWorkers().subscribe();
    this.workforceService.loadContractors().subscribe();
    this.workforceService.loadEmployees().subscribe({
      next: () => {
        if (this.route.snapshot.queryParamMap.get('recipientType') === 'EMPLOYEE') {
          this.openCreateModal('EMPLOYEE');
        }
      }
    });
    this.workforceService.loadCategories().subscribe();
    this.workforceService.loadAdvancePolicies().subscribe();
  }

  openCreateModal(recipientType: 'WORKER' | 'CONTRACTOR' | 'EMPLOYEE' = 'WORKER') {
    this.form = this.defaultForm();
    this.form.recipientType = recipientType;
    const workers = this.workforceService.workers();
    const contractors = this.workforceService.contractors();
    const employees = this.workforceService.employees();
    if (recipientType === 'WORKER' && workers.length > 0) this.form.workerId = workers[0].id;
    if (recipientType === 'CONTRACTOR' && contractors.length > 0) this.form.contractorId = contractors[0].id;
    if (recipientType === 'EMPLOYEE' && employees.length > 0) this.form.employeeId = employees[0].id;
    this.applyAdvancePolicy();
    this.isModalOpen = true;
  }

  onRecipientTypeChange() {
    this.form.workerId = '';
    this.form.contractorId = '';
    this.form.employeeId = '';
    const workers = this.workforceService.workers();
    const contractors = this.workforceService.contractors();
    const employees = this.workforceService.employees();
    if (this.form.recipientType === 'WORKER' && workers.length > 0) this.form.workerId = workers[0].id;
    if (this.form.recipientType === 'CONTRACTOR' && contractors.length > 0) this.form.contractorId = contractors[0].id;
    if (this.form.recipientType === 'EMPLOYEE' && employees.length > 0) this.form.employeeId = employees[0].id;
    this.applyAdvancePolicy();
  }

  openPolicyModal(): void { this.policyForm = this.defaultPolicyForm(); this.policyModalOpen.set(true); }

  savePolicy(): void {
    if (this.policyForm.scopeType !== 'GLOBAL' && !this.policyForm.scopeId) { this.notificationService.warning(this.i18n.t('workforce.ui.advances.policySelectScope')); return; }
    if (!this.policyForm.effectiveFrom) { this.notificationService.warning(this.i18n.t('workforce.ui.advances.policyStartRequired')); return; }
    if (this.policyForm.effectiveTo && this.policyForm.effectiveTo < this.policyForm.effectiveFrom) { this.notificationService.warning(this.i18n.t('workforce.ui.advances.policyDateInvalid')); return; }
    this.saving.set(true);
    this.workforceService.saveAdvancePolicy(this.policyForm).subscribe({
      next: () => { this.saving.set(false); this.policyModalOpen.set(false); this.notificationService.success(this.i18n.t('workforce.ui.advances.policySaved')); },
      error: error => { this.saving.set(false); this.notificationService.error(apiErrorDetail(error, this.i18n.t('workforce.ui.advances.policySaveFailed'))); },
    });
  }

  applyAdvancePolicy(): void {
    const effectiveDate = this.form.firstInstallmentDate || new Date().toISOString().slice(0, 10);
    const policies = this.workforceService.advancePolicies().filter(policy => policy.active
      && policy.effectiveFrom <= effectiveDate && (!policy.effectiveTo || policy.effectiveTo >= effectiveDate));
    const worker = this.workforceService.workers().find(item => item.id === this.form.workerId);
    const employee = this.workforceService.employees().find(item => item.id === this.form.employeeId);
    const newest = (items: AdvancePolicy[]) => items.sort((a, b) => b.version - a.version)[0];
    const policy = (this.form.recipientType === 'EMPLOYEE'
      ? newest(policies.filter(item => item.scopeType === 'EMPLOYEE' && item.scopeId === employee?.id))
        ?? newest(policies.filter(item => item.scopeType === 'EMPLOYEE_CATEGORY' && item.scopeId === employee?.categoryId))
      : newest(policies.filter(item => item.scopeType === 'WORKER' && item.scopeId === worker?.id))
        ?? newest(policies.filter(item => item.scopeType === 'CATEGORY' && item.scopeId === worker?.categoryId)))
      ?? newest(policies.filter(item => item.scopeType === 'GLOBAL'));
    this.effectivePolicyPreview.set(policy ?? null);
    if (!policy) return;
    Object.assign(this.form, { deductionMode: policy.deductionMode, deductionFrequency: policy.deductionFrequency, maxDeductionPercent: policy.maxDeductionPercent, totalInstallments: policy.defaultInstallments, deferralPeriods: policy.deferralPeriods });
    this.recalcInstallment();
  }

  getPolicyScopeLabel(policy: AdvancePolicy): string {
    const keys: Record<AdvancePolicy['scopeType'], string> = { GLOBAL: 'workforce.ui.advances.policyScopeGlobal', CATEGORY: 'workforce.ui.advances.policyScopeWorkerCategory', WORKER: 'workforce.ui.advances.policyScopeWorker', EMPLOYEE_CATEGORY: 'workforce.ui.advances.policyScopeEmployeeCategory', EMPLOYEE: 'workforce.ui.advances.policyScopeEmployee' };
    return this.i18n.t(keys[policy.scopeType]);
  }

  recalcInstallment() {
    if (this.form.totalInstallments > 0 && this.form.amount > 0) {
      this.form.installmentAmount = Math.round((this.form.amount / this.form.totalInstallments) * 100) / 100;
    }
  }

  saveAdvance() {
    if (!this.form.amount || this.form.amount <= 0) {
      this.notificationService.warning(this.i18n.t('workforce.ui.advances.amountRequiredWarning'));
      return;
    }
    const selectedRecipientId = this.form.recipientType === 'WORKER'
      ? this.form.workerId : this.form.recipientType === 'CONTRACTOR'
        ? this.form.contractorId : this.form.employeeId;
    if (!selectedRecipientId) {
      this.notificationService.warning(this.i18n.t('workforce.ui.advances.recipientRequiredWarning'));
      return;
    }
    if (this.form.termType === 'LONG_TERM' && this.form.totalInstallments < 2) {
      this.notificationService.warning(this.i18n.t('workforce.ui.advances.installmentsMinimum'));
      return;
    }
    this.saving.set(true);
    this.workforceService.createAdvance(this.form).subscribe({
      next: () => {
        this.saving.set(false);
        this.isModalOpen = false;
        this.notificationService.success(this.i18n.t('workforce.ui.advances.createdSuccess'));
      },
      error: (e) => {
        this.saving.set(false);
        const msg = apiErrorDetail(e, e?.error?.message ?? e?.message ?? this.i18n.t('workforce.ui.unexpectedError'));
        this.notificationService.error(this.i18n.t('workforce.ui.advances.saveFailed', { detail: msg }));
      }
    });
  }

  confirmAction = signal<{ message: string; onConfirm: () => void } | null>(null);

  pauseAdvance(adv: WorkforceAdvance) {
    const msg = this.i18n.t('workforce.ui.advances.pauseConfirm', { name: this.recipientName(adv) });
    this.confirmAction.set({
      message: msg,
      onConfirm: () => {
        this.confirmAction.set(null);
        this.workforceService.pauseAdvance(adv.id).subscribe({
          next: () => {
            this.notificationService.success(this.i18n.t('workforce.ui.advances.pauseSuccess'));
            this.workforceService.loadAdvances().subscribe();
          },
          error: (e) => this.notificationService.error(this.i18n.t('workforce.ui.advances.pauseFailed', { detail: e?.error?.message ?? e?.message ?? '' }))
        });
      }
    });
  }

  resumeAdvance(adv: WorkforceAdvance) {
    this.confirmAction.set({
      message: this.i18n.t('workforce.ui.advances.resumeConfirm'),
      onConfirm: () => {
        this.confirmAction.set(null);
        this.workforceService.resumeAdvance(adv.id).subscribe({
          next: () => {
            this.notificationService.success(this.i18n.t('workforce.ui.advances.resumeSuccess'));
            this.workforceService.loadAdvances().subscribe();
          },
          error: (e) => this.notificationService.error(this.i18n.t('workforce.ui.advances.resumeFailed', { detail: e?.error?.message ?? e?.message ?? '' }))
        });
      }
    });
  }

  cancelAction() {
    this.confirmAction.set(null);
  }

  openRepayModal(adv: WorkforceAdvance) {
    this.repayTarget.set(adv);
    this.repayForm = this.defaultRepayForm();
    this.repayForm.amount = adv.remainingBalance;
    this.repayForm.repaymentDate = new Date().toISOString().slice(0, 10);
    this.repayModalOpen.set(true);
  }

  onRepayTypeChange() {
    const adv = this.repayTarget();
    if (!adv) return;
    if (this.repayForm.repaymentType === 'FULL') {
      this.repayForm.amount = adv.remainingBalance;
    }
  }

  confirmRepayment() {
    const adv = this.repayTarget();
    if (!adv) return;
    const amount = this.repayForm.amount;
    if (!amount || amount <= 0) {
      this.notificationService.warning(this.i18n.t('workforce.ui.advances.repaymentAmountInvalid'));
      return;
    }
    if (amount > adv.remainingBalance) {
      this.notificationService.warning(this.i18n.t('workforce.ui.advances.repaymentExceedsBalance'));
      return;
    }
    if (this.repayForm.repaymentType === 'FULL' && amount !== adv.remainingBalance) {
      this.notificationService.warning(this.i18n.t('workforce.ui.advances.fullRepaymentMismatch'));
      return;
    }

    this.saving.set(true);
    const payload: AdvanceRepayRequest = {
      amount,
      repaymentType: this.repayForm.repaymentType,
      repaymentDate: this.repayForm.repaymentDate || undefined,
      paymentMethod: this.repayForm.paymentMethod || undefined,
      receiptRef: this.repayForm.receiptRef || undefined,
      notes: this.repayForm.notes || undefined
    };
    this.workforceService.repayAdvance(adv.id, payload).subscribe({
      next: () => {
        this.saving.set(false);
        this.repayModalOpen.set(false);
        this.repayTarget.set(null);
        this.notificationService.success(this.i18n.t('workforce.ui.advances.repaymentSuccess', { amount }));
        this.workforceService.loadAdvances().subscribe();
      },
      error: (e) => {
        this.saving.set(false);
        const msg = apiErrorDetail(e, e?.error?.message ?? e?.message ?? this.i18n.t('workforce.ui.unexpectedError'));
        this.notificationService.error(this.i18n.t('workforce.ui.advances.repaymentFailed', { detail: msg }));
      }
    });
  }

  closeRepayModal() {
    this.repayModalOpen.set(false);
    this.repayTarget.set(null);
  }

  repayAdvance(adv: WorkforceAdvance) {
    this.openRepayModal(adv);
  }

  exportCsv(): void {
    const rows = this.workforceService.advances().map((adv) => ({
      recipient: this.recipientName(adv),
      type: this.recipientTypeLabel(adv.recipientType),
      amount: adv.amount,
      termType: this.getTermLabel(adv.termType),
      totalInstallments: adv.totalInstallments,
      installmentAmount: adv.installmentAmount,
      remainingBalance: adv.remainingBalance,
      deductionFrequency: this.getFrequencyLabel(adv.deductionFrequency),
      maxDeductionPercent: adv.maxDeductionPercent,
      status: this.getStatusLabel(adv.status),
    }));
    exportCsv(
      rows,
      [
        { key: 'recipient', label: this.i18n.t('workforce.ui.advances.recipient') },
        { key: 'type', label: this.i18n.t('workforce.ui.type') },
        { key: 'amount', label: this.i18n.t('workforce.ui.advances.totalAmount') },
        { key: 'termType', label: this.i18n.t('workforce.ui.advances.termType') },
        { key: 'totalInstallments', label: this.i18n.t('workforce.ui.advances.installments') },
        { key: 'installmentAmount', label: this.i18n.t('workforce.ui.advances.installmentAmount') },
        { key: 'remainingBalance', label: this.i18n.t('workforce.ui.advances.remainingBalance') },
        { key: 'deductionFrequency', label: this.i18n.t('workforce.ui.advances.frequency') },
        { key: 'maxDeductionPercent', label: this.i18n.t('workforce.ui.advances.maxDeduction') },
        { key: 'status', label: this.i18n.t('workforce.ui.status') },
      ],
      `advances-${new Date().toISOString().slice(0, 10)}.csv`,
    );
  }

  // --- Labels ---
  getTermLabel(term: string): string { return this.i18n.t(term === 'SHORT_TERM' ? 'workforce.ui.advances.shortTerm' : 'workforce.ui.advances.longTerm'); }

  getFrequencyLabel(freq: string): string { const keys:Record<string,string>={HALF_MONTH:'workforce.ui.advances.halfMonthly',MONTHLY:'workforce.ui.advances.monthly',WEEKLY:'workforce.ui.advances.weekly'}; return keys[freq] ? this.i18n.t(keys[freq]) : freq; }

  getStatusLabel(status: string): string { const keys:Record<string,string>={ACTIVE:'workforce.ui.advances.activeStatus',PAID_OFF:'workforce.ui.advances.paidOff',SUSPENDED:'workforce.ui.advances.suspended',PAUSED:'workforce.ui.advances.suspended'}; return keys[status] ? this.i18n.t(keys[status]) : status; }

  recipientName(advance: WorkforceAdvance): string {
    if (advance.recipientType === 'EMPLOYEE') return advance.employeeName ?? '—';
    if (advance.recipientType === 'CONTRACTOR') return advance.contractorName ?? '—';
    return advance.workerName ?? '—';
  }

  recipientTypeLabel(type: WorkforceAdvance['recipientType']): string { const key=type==='EMPLOYEE'?'workforce.ui.employee':type==='CONTRACTOR'?'workforce.ui.contractor':'workforce.ui.worker'; return this.i18n.t(key); }

  private defaultForm() {
    return {
      recipientType: 'WORKER', workerId: '', contractorId: '', employeeId: '',
      amount: 1000, termType: 'SHORT_TERM', totalInstallments: 1,
      installmentAmount: 1000, deductionFrequency: 'HALF_MONTH',
      maxDeductionPercent: 50, reason: '',
      firstInstallmentDate: '', deductionMode: 'AUTO', deferralPeriods: 0
    };
  }

  private defaultRepayForm() {
    return {
      repaymentType: 'FULL' as 'PARTIAL' | 'FULL',
      amount: 0,
      repaymentDate: '',
      paymentMethod: '',
      receiptRef: '',
      notes: ''
    };
  }

  private defaultPolicyForm(): AdvancePolicy {
    return { scopeType: 'GLOBAL', scopeId: '', deductionMode: 'AUTO', deductionFrequency: 'HALF_MONTH', maxDeductionPercent: 50, defaultInstallments: 1, deferralPeriods: 0, version: 1, effectiveFrom: new Date().toISOString().slice(0, 10), effectiveTo: '', active: true };
  }
}
