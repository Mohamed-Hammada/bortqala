import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { I18nService } from '../../core/i18n.service';
import { NotificationService } from '../../core/notification.service';
import { apiErrorMessage } from '../../core/api-error';
import {
  Branch,
  Company,
  ConsolidatedOrganizationSummary,
  Department,
  EliminationResult,
  IntercompanyStatus,
  IntercompanyTransaction,
  IntercompanyType,
  OrganizationHierarchy,
  Warehouse,
} from './organization.models';

import { DecimalPipe } from '@angular/common';
import { ModalDialogComponent } from '../../shared/ui/modal-dialog/modal-dialog.component';

@Component({
  selector: 'app-organization-page',
  imports: [ReactiveFormsModule, ModalDialogComponent, DecimalPipe],
  templateUrl: './organization.page.html',
  styleUrl: './organization.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OrganizationPage {
  readonly http = inject(HttpClient);
  readonly i18n = inject(I18nService);
  readonly notification = inject(NotificationService);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly activeTab = signal<'companies' | 'branches' | 'warehouses' | 'departments' | 'consolidation' | 'intercompany'>('companies');

  readonly companies = signal<Company[]>([]);
  readonly branches = signal<Branch[]>([]);
  readonly warehouses = signal<Warehouse[]>([]);
  readonly departments = signal<Department[]>([]);

  readonly consolidationSummary = signal<ConsolidatedOrganizationSummary | null>(null);
  readonly intercompanyTransactions = signal<IntercompanyTransaction[]>([]);

  readonly drawerOpen = signal(false);
  readonly intercompanyModalOpen = signal(false);
  readonly eliminationModalOpen = signal(false);
  readonly editingId = signal<string | null>(null);
  readonly savingIntercompany = signal(false);
  readonly runningElimination = signal(false);

  readonly companyForm = new FormGroup({
    code: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    name: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    taxNumber: new FormControl('', { nonNullable: true }),
    commercialRegistry: new FormControl('', { nonNullable: true }),
    active: new FormControl(true, { nonNullable: true }),
  });

  readonly branchForm = new FormGroup({
    companyId: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    code: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    name: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    location: new FormControl('', { nonNullable: true }),
    active: new FormControl(true, { nonNullable: true }),
  });

  readonly warehouseForm = new FormGroup({
    branchId: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    code: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    name: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    location: new FormControl('', { nonNullable: true }),
    active: new FormControl(true, { nonNullable: true }),
  });

  readonly departmentForm = new FormGroup({
    companyId: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    code: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    name: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    managerId: new FormControl('', { nonNullable: true }),
    active: new FormControl(true, { nonNullable: true }),
  });

  readonly intercompanyForm = new FormGroup({
    fromCompanyId: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    fromBranchId: new FormControl('', { nonNullable: true }),
    toCompanyId: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    toBranchId: new FormControl('', { nonNullable: true }),
    transactionType: new FormControl<IntercompanyType>('INVENTORY_TRANSFER', { nonNullable: true, validators: [Validators.required] }),
    amount: new FormControl<number>(10000, { nonNullable: true, validators: [Validators.required, Validators.min(0.01)] }),
    currency: new FormControl('EGP', { nonNullable: true }),
    description: new FormControl('', { nonNullable: true }),
    dueToAccountId: new FormControl('', { nonNullable: true }),
    dueFromAccountId: new FormControl('', { nonNullable: true }),
  });

  readonly eliminationForm = new FormGroup({
    period: new FormControl('2026-Q3', { nonNullable: true, validators: [Validators.required] }),
  });

  constructor() {
    void this.load();
  }

  async load() {
    this.loading.set(true);
    this.error.set(null);
    try {
      const [data, summary, txs] = await Promise.all([
        firstValueFrom(this.http.get<OrganizationHierarchy>('/api/v1/organization')),
        firstValueFrom(this.http.get<ConsolidatedOrganizationSummary>('/api/v1/organization/consolidation/summary')),
        firstValueFrom(this.http.get<IntercompanyTransaction[]>('/api/v1/organization/intercompany')),
      ]);
      this.companies.set(data.companies);
      this.branches.set(data.branches);
      this.warehouses.set(data.warehouses);
      this.departments.set(data.departments);
      this.consolidationSummary.set(summary);
      this.intercompanyTransactions.set(txs);
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    } finally {
      this.loading.set(false);
    }
  }

  openNew() {
    this.editingId.set(null);
    const tab = this.activeTab();
    if (tab === 'companies') {
      this.companyForm.reset({ code: '', name: '', taxNumber: '', commercialRegistry: '', active: true });
    } else if (tab === 'branches') {
      this.branchForm.reset({ companyId: this.companies()[0]?.id ?? '', code: '', name: '', location: '', active: true });
    } else if (tab === 'warehouses') {
      this.warehouseForm.reset({ branchId: this.branches()[0]?.id ?? '', code: '', name: '', location: '', active: true });
    } else if (tab === 'departments') {
      this.departmentForm.reset({ companyId: this.companies()[0]?.id ?? '', code: '', name: '', managerId: '', active: true });
    }
    this.drawerOpen.set(true);
  }

  closeDrawer() {
    this.drawerOpen.set(false);
  }

  openNewIntercompany() {
    const fromComp = this.companies()[0]?.id ?? '';
    const toComp = this.companies()[1]?.id ?? this.companies()[0]?.id ?? '';
    this.intercompanyForm.reset({
      fromCompanyId: fromComp,
      fromBranchId: '',
      toCompanyId: toComp,
      toBranchId: '',
      transactionType: 'INVENTORY_TRANSFER',
      amount: 10000,
      currency: 'EGP',
      description: '',
      dueToAccountId: '',
      dueFromAccountId: '',
    });
    this.intercompanyModalOpen.set(true);
  }

  closeIntercompanyModal() {
    this.intercompanyModalOpen.set(false);
  }

  openEliminationModal() {
    this.eliminationForm.reset({ period: '2026-Q3' });
    this.eliminationModalOpen.set(true);
  }

  closeEliminationModal() {
    this.eliminationModalOpen.set(false);
  }

  async submitCompany() {
    if (this.companyForm.invalid) return;
    try {
      const payload = this.companyForm.getRawValue();
      const id = this.editingId();
      await firstValueFrom(
        id
          ? this.http.put(`/api/v1/organization/companies/${id}`, payload)
          : this.http.post('/api/v1/organization/companies', payload),
      );
      this.notification.success(this.i18n.t('common.save') + ' ✓');
      this.drawerOpen.set(false);
      await this.load();
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    }
  }

  async submitBranch() {
    if (this.branchForm.invalid) return;
    try {
      const payload = this.branchForm.getRawValue();
      const id = this.editingId();
      await firstValueFrom(
        id
          ? this.http.put(`/api/v1/organization/branches/${id}`, payload)
          : this.http.post('/api/v1/organization/branches', payload),
      );
      this.notification.success(this.i18n.t('common.save') + ' ✓');
      this.drawerOpen.set(false);
      await this.load();
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    }
  }

  async submitWarehouse() {
    if (this.warehouseForm.invalid) return;
    try {
      const payload = this.warehouseForm.getRawValue();
      const id = this.editingId();
      await firstValueFrom(
        id
          ? this.http.put(`/api/v1/organization/warehouses/${id}`, payload)
          : this.http.post('/api/v1/organization/warehouses', payload),
      );
      this.notification.success(this.i18n.t('common.save') + ' ✓');
      this.drawerOpen.set(false);
      await this.load();
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    }
  }

  async submitDepartment() {
    if (this.departmentForm.invalid) return;
    try {
      const payload = this.departmentForm.getRawValue();
      const id = this.editingId();
      await firstValueFrom(
        id
          ? this.http.put(`/api/v1/organization/departments/${id}`, payload)
          : this.http.post('/api/v1/organization/departments', payload),
      );
      this.notification.success(this.i18n.t('common.save') + ' ✓');
      this.drawerOpen.set(false);
      await this.load();
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    }
  }

  async submitIntercompany() {
    if (this.intercompanyForm.invalid) return;
    this.savingIntercompany.set(true);
    try {
      const payload = this.intercompanyForm.getRawValue();
      await firstValueFrom(
        this.http.post('/api/v1/organization/intercompany', payload),
      );
      this.notification.success(this.i18n.t('org.createSuccess'));
      this.intercompanyModalOpen.set(false);
      await this.load();
    } catch (e) {
      this.notification.error(apiErrorMessage(e, this.i18n));
    } finally {
      this.savingIntercompany.set(false);
    }
  }

  async approveIntercompany(id: string) {
    try {
      await firstValueFrom(
        this.http.post(`/api/v1/organization/intercompany/${id}/approve`, {}),
      );
      this.notification.success(this.i18n.t('org.approveSuccess'));
      await this.load();
    } catch (e) {
      this.notification.error(apiErrorMessage(e, this.i18n));
    }
  }

  async settleIntercompany(id: string) {
    try {
      await firstValueFrom(
        this.http.post(`/api/v1/organization/intercompany/${id}/settle`, {}),
      );
      this.notification.success(this.i18n.t('org.settleSuccess'));
      await this.load();
    } catch (e) {
      this.notification.error(apiErrorMessage(e, this.i18n));
    }
  }

  async submitElimination() {
    if (this.eliminationForm.invalid) return;
    this.runningElimination.set(true);
    try {
      const payload = this.eliminationForm.getRawValue();
      const res = await firstValueFrom(
        this.http.post<EliminationResult>('/api/v1/organization/intercompany/eliminate', payload),
      );
      this.notification.success(this.i18n.t('org.eliminationSuccess'));
      this.eliminationModalOpen.set(false);
      await this.load();
    } catch (e) {
      this.notification.error(apiErrorMessage(e, this.i18n));
    } finally {
      this.runningElimination.set(false);
    }
  }

  getTypeLabel(type: IntercompanyType): string {
    switch (type) {
      case 'INVENTORY_TRANSFER':
        return this.i18n.t('org.typeInventoryTransfer');
      case 'EXPENSE_ALLOCATION':
        return this.i18n.t('org.typeExpenseAllocation');
      case 'MANAGEMENT_FEE':
        return this.i18n.t('org.typeManagementFee');
      case 'LOAN_ADVANCE':
        return this.i18n.t('org.typeLoanAdvance');
      default:
        return type;
    }
  }

  getStatusLabel(status: IntercompanyStatus): string {
    switch (status) {
      case 'DRAFT':
        return this.i18n.t('org.statusDraft');
      case 'PENDING_APPROVAL':
        return this.i18n.t('org.statusPendingApproval');
      case 'APPROVED':
        return this.i18n.t('org.statusApproved');
      case 'SETTLED':
        return this.i18n.t('org.statusSettled');
      case 'ELIMINATED':
        return this.i18n.t('org.statusEliminated');
      default:
        return status;
    }
  }
}
