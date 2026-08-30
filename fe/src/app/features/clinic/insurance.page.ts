import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { I18nService } from '../../core/i18n.service';
import { NotificationService } from '../../core/notification.service';
import {
  CreateClaimBatchPayload,
  DecidePreAuthorizationPayload,
  InsuranceClaimBatch,
  InsuranceClaimLine,
  InsurancePayer,
  InsurancePlan,
  InsurancePreAuthorization,
  RequestPreAuthorizationPayload,
  ResubmitClaimLinePayload,
  SaveInsurancePayerPayload,
  SaveInsurancePlanPayload,
  SettleClaimBatchPayload,
} from './clinic.models';
import { ClinicService } from './clinic.service';

@Component({
  selector: 'app-insurance-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './insurance.page.html',
  styleUrls: ['./insurance.page.scss'],
})
export class InsurancePageComponent implements OnInit {
  private readonly clinicService = inject(ClinicService);
  private readonly notificationService = inject(NotificationService);
  readonly i18n = inject(I18nService);

  readonly activeTab = signal<'PAYERS' | 'PRE_AUTH' | 'CLAIMS'>('PAYERS');
  readonly loading = signal<boolean>(false);
  readonly saving = signal<boolean>(false);

  readonly payerTypes = ['HIO', 'PRIVATE', 'CORPORATE'] as const;
  readonly preAuthStatuses = ['REQUESTED', 'APPROVED', 'REJECTED', 'EXPIRED'] as const;

  // Payers & Plans
  readonly payers = signal<InsurancePayer[]>([]);
  readonly plans = signal<InsurancePlan[]>([]);
  readonly selectedPayerId = signal<string>('');

  readonly showPayerModal = signal<boolean>(false);
  payerForm: SaveInsurancePayerPayload = {
    name: '',
    type: 'PRIVATE',
    contactPhone: '',
    contactEmail: '',
    active: true,
  };

  readonly showPlanModal = signal<boolean>(false);
  planForm: SaveInsurancePlanPayload = {
    payerId: '',
    name: '',
    coveragePercent: 80,
    copayFlat: 50,
    annualLimit: 50000,
    exclusionsText: '',
    active: true,
  };

  // Pre-Authorizations
  readonly preAuthorizations = signal<InsurancePreAuthorization[]>([]);
  readonly showPreAuthModal = signal<boolean>(false);
  preAuthForm: RequestPreAuthorizationPayload = {
    payerId: '',
    patientId: '',
    visitId: '',
    procedureText: '',
    approvalCode: '',
    requestedAmount: 1000,
  };

  // Claims
  readonly claimBatches = signal<InsuranceClaimBatch[]>([]);
  readonly selectedBatch = signal<InsuranceClaimBatch | null>(null);
  readonly showCreateBatchModal = signal<boolean>(false);
  createBatchForm: CreateClaimBatchPayload = {
    payerId: '',
    period: new Date().toISOString().slice(0, 7),
    notes: '',
  };

  ngOnInit(): void {
    this.loadPayers();
    this.loadPlans();
  }

  setTab(tab: 'PAYERS' | 'PRE_AUTH' | 'CLAIMS'): void {
    this.activeTab.set(tab);
    if (tab === 'PAYERS') {
      this.loadPayers();
      this.loadPlans();
    } else if (tab === 'PRE_AUTH') {
      this.loadPreAuthorizations();
    } else if (tab === 'CLAIMS') {
      this.loadClaimBatches();
    }
  }

  loadPayers(): void {
    this.loading.set(true);
    this.clinicService.getAllPayers().subscribe({
      next: (data) => {
        this.payers.set(data);
        if (data.length > 0 && !this.selectedPayerId()) {
          this.selectedPayerId.set(data[0].id);
        }
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  loadPlans(): void {
    this.clinicService.getPlansByPayer(this.selectedPayerId() || undefined).subscribe({
      next: (data) => this.plans.set(data),
    });
  }

  loadPreAuthorizations(): void {
    this.loading.set(true);
    this.clinicService.getPreAuthorizations().subscribe({
      next: (data) => {
        this.preAuthorizations.set(data);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  loadClaimBatches(): void {
    this.loading.set(true);
    this.clinicService.getAllClaimBatches(this.selectedPayerId() || undefined).subscribe({
      next: (data) => {
        this.claimBatches.set(data);
        if (this.selectedBatch()) {
          const updated = data.find((b) => b.id === this.selectedBatch()!.id) || null;
          this.selectedBatch.set(updated);
        }
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  openAddPayerModal(): void {
    this.payerForm = {
      name: '',
      type: 'PRIVATE',
      contactPhone: '',
      contactEmail: '',
      active: true,
    };
    this.showPayerModal.set(true);
  }

  savePayer(): void {
    if (!this.payerForm.name) return;
    this.saving.set(true);
    this.clinicService.savePayer(this.payerForm).subscribe({
      next: () => {
        this.saving.set(false);
        this.showPayerModal.set(false);
        this.loadPayers();
        this.notificationService.success(this.i18n.t('clinic.resultSuccess'));
      },
      error: () => this.saving.set(false),
    });
  }

  openAddPlanModal(): void {
    this.planForm = {
      payerId: this.selectedPayerId() || (this.payers()[0]?.id ?? ''),
      name: '',
      coveragePercent: 80,
      copayFlat: 50,
      annualLimit: 50000,
      exclusionsText: '',
      active: true,
    };
    this.showPlanModal.set(true);
  }

  savePlan(): void {
    if (!this.planForm.name || !this.planForm.payerId) return;
    this.saving.set(true);
    this.clinicService.savePlan(this.planForm).subscribe({
      next: () => {
        this.saving.set(false);
        this.showPlanModal.set(false);
        this.loadPlans();
        this.notificationService.success(this.i18n.t('clinic.resultSuccess'));
      },
      error: () => this.saving.set(false),
    });
  }

  openAddPreAuthModal(): void {
    this.preAuthForm = {
      payerId: this.selectedPayerId() || (this.payers()[0]?.id ?? ''),
      patientId: '',
      visitId: '',
      procedureText: '',
      approvalCode: 'AUTH-' + Math.floor(Math.random() * 90000 + 10000),
      requestedAmount: 1500,
    };
    this.showPreAuthModal.set(true);
  }

  submitPreAuth(): void {
    if (!this.preAuthForm.patientId || !this.preAuthForm.procedureText || !this.preAuthForm.approvalCode) return;
    this.saving.set(true);
    this.clinicService.requestPreAuthorization(this.preAuthForm).subscribe({
      next: () => {
        this.saving.set(false);
        this.showPreAuthModal.set(false);
        this.loadPreAuthorizations();
        this.notificationService.success(this.i18n.t('clinic.resultSuccess'));
      },
      error: () => this.saving.set(false),
    });
  }

  approvePreAuth(auth: InsurancePreAuthorization): void {
    this.clinicService.decidePreAuthorization(auth.id, {
      status: 'APPROVED',
      approvedAmount: auth.requestedAmount,
    }).subscribe({
      next: () => {
        this.loadPreAuthorizations();
        this.notificationService.success(this.i18n.t('clinic.resultSuccess'));
      },
    });
  }

  rejectPreAuth(auth: InsurancePreAuthorization): void {
    this.clinicService.decidePreAuthorization(auth.id, {
      status: 'REJECTED',
      approvedAmount: 0,
    }).subscribe({
      next: () => {
        this.loadPreAuthorizations();
        this.notificationService.success(this.i18n.t('clinic.resultSuccess'));
      },
    });
  }

  openCreateBatchModal(): void {
    this.createBatchForm = {
      payerId: this.selectedPayerId() || (this.payers()[0]?.id ?? ''),
      period: new Date().toISOString().slice(0, 7),
      notes: '',
    };
    this.showCreateBatchModal.set(true);
  }

  submitCreateBatch(): void {
    if (!this.createBatchForm.payerId || !this.createBatchForm.period) return;
    this.saving.set(true);
    this.clinicService.createClaimBatch(this.createBatchForm).subscribe({
      next: () => {
        this.saving.set(false);
        this.showCreateBatchModal.set(false);
        this.loadClaimBatches();
        this.notificationService.success(this.i18n.t('clinic.resultSuccess'));
      },
      error: () => this.saving.set(false),
    });
  }

  submitBatch(batch: InsuranceClaimBatch): void {
    this.clinicService.submitClaimBatch(batch.id).subscribe({
      next: () => {
        this.loadClaimBatches();
        this.notificationService.success(this.i18n.t('clinic.resultSuccess'));
      },
    });
  }

  viewBatch(batch: InsuranceClaimBatch): void {
    this.selectedBatch.set(batch);
  }

  approveLine(batch: InsuranceClaimBatch, line: InsuranceClaimLine): void {
    const payload: SettleClaimBatchPayload = {
      lineDecisions: [{ lineId: line.id, decision: 'APPROVED' }],
    };
    this.clinicService.settleClaimBatch(batch.id, payload).subscribe({
      next: () => {
        this.loadClaimBatches();
        this.notificationService.success(this.i18n.t('clinic.resultSuccess'));
      },
    });
  }

  rejectLine(batch: InsuranceClaimBatch, line: InsuranceClaimLine): void {
    const reason = 'Documentation required';
    const payload: SettleClaimBatchPayload = {
      lineDecisions: [{ lineId: line.id, decision: 'REJECTED', rejectionReason: reason }],
    };
    this.clinicService.settleClaimBatch(batch.id, payload).subscribe({
      next: () => {
        this.loadClaimBatches();
        this.notificationService.success(this.i18n.t('clinic.resultSuccess'));
      },
    });
  }

  resubmitLine(line: InsuranceClaimLine): void {
    // Finds or creates a draft batch to resubmit into
    const draft = this.claimBatches().find((b) => b.status === 'DRAFT');
    if (!draft) {
      this.notificationService.error(this.i18n.t('clinic.createClaimBatch'));
      return;
    }

    const payload: ResubmitClaimLinePayload = {
      originalLineId: line.id,
      newBatchId: draft.id,
      adjustedInsurerShare: line.insurerShare,
      notes: 'Resubmitted with supporting documentation',
    };

    this.clinicService.resubmitClaimLine(payload).subscribe({
      next: () => {
        this.loadClaimBatches();
        this.notificationService.success(this.i18n.t('clinic.resultSuccess'));
      },
    });
  }
}
