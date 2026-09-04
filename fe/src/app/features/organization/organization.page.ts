import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { I18nService } from '../../core/i18n.service';
import { NotificationService } from '../../core/notification.service';
import { apiErrorMessage } from '../../core/api-error';
import {
  Branch,
  BranchControlSummary,
  Company,
  ConsolidatedGroupReport,
  ConsolidatedOrganizationSummary,
  Department,
  DiscrepancyResolutionStatus,
  EliminationResult,
  IntercompanyStatus,
  IntercompanyTransaction,
  IntercompanyType,
  OrganizationHierarchy,
  ReceiveTransferPayload,
  StockTransferDiscrepancyItem,
  StockTransferItem,
  Warehouse,
} from './organization.models';

import { DecimalPipe } from '@angular/common';
import { ModalDialogComponent } from '../../shared/ui/modal-dialog/modal-dialog.component';

export interface InspectionLineState {
  lineId: string;
  itemCode: string;
  itemName: string;
  shippedQty: number;
  receivedQuantity: number;
  damagedQuantity: number;
  lostQuantity: number;
  discrepancyReason: string;
  discrepancyNotes: string;
}

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
  readonly activeTab = signal<'companies' | 'branches' | 'warehouses' | 'departments' | 'transfers' | 'consolidation' | 'intercompany'>('companies');

  readonly companies = signal<Company[]>([]);
  readonly branches = signal<Branch[]>([]);
  readonly permittedBranches = signal<Branch[]>([]);
  readonly warehouses = signal<Warehouse[]>([]);
  readonly departments = signal<Department[]>([]);

  readonly consolidationSummary = signal<ConsolidatedOrganizationSummary | null>(null);
  readonly intercompanyTransactions = signal<IntercompanyTransaction[]>([]);

  // Branch Control Center state
  readonly selectedBranchSummary = signal<BranchControlSummary | null>(null);
  readonly summaryModalOpen = signal(false);
  readonly loadingSummary = signal(false);

  // Transfers & In-Transit state
  readonly transfers = signal<StockTransferItem[]>([]);
  readonly discrepancies = signal<StockTransferDiscrepancyItem[]>([]);
  readonly loadingTransfers = signal(false);
  readonly dispatchModalOpen = signal(false);
  readonly selectedTransferForDispatch = signal<StockTransferItem | null>(null);
  readonly dispatching = signal(false);
  readonly inspectModalOpen = signal(false);
  readonly selectedTransferForInspect = signal<StockTransferItem | null>(null);
  readonly receiving = signal(false);
  readonly inspectionLines = signal<InspectionLineState[]>([]);
  readonly inspectNotes = new FormControl('', { nonNullable: true });
  readonly resolveModalOpen = signal(false);
  readonly selectedDiscrepancy = signal<StockTransferDiscrepancyItem | null>(null);
  readonly resolving = signal(false);

  // Consolidated Group Report state
  readonly selectedCompanyFilter = signal<string>('');
  readonly selectedBranchFilter = signal<string>('');
  readonly selectedPeriod = signal<string>('2026-Q1');
  readonly groupReport = signal<ConsolidatedGroupReport | null>(null);
  readonly loadingReport = signal(false);

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
    isMainBranch: new FormControl(false, { nonNullable: true }),
    phone: new FormControl('', { nonNullable: true }),
    email: new FormControl('', { nonNullable: true }),
    taxNumber: new FormControl('', { nonNullable: true }),
    commercialRegistry: new FormControl('', { nonNullable: true }),
    defaultWarehouseId: new FormControl('', { nonNullable: true }),
    defaultCashboxId: new FormControl('', { nonNullable: true }),
    defaultBankAccountId: new FormControl('', { nonNullable: true }),
    defaultPosTerminalId: new FormControl('', { nonNullable: true }),
    documentCodePrefix: new FormControl('', { nonNullable: true }),
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

  readonly dispatchForm = new FormGroup({
    carrierName: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    driverName: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    driverPhone: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    vehiclePlate: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    waybillNumber: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    notes: new FormControl('', { nonNullable: true }),
  });

  readonly resolveForm = new FormGroup({
    resolutionStatus: new FormControl<DiscrepancyResolutionStatus>('RESOLVED', { nonNullable: true, validators: [Validators.required] }),
    resolutionNotes: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
  });

  constructor() {
    void this.load();
  }

  async load() {
    this.loading.set(true);
    this.error.set(null);
    try {
      const [data, summary, txs, permitted] = await Promise.all([
        firstValueFrom(this.http.get<OrganizationHierarchy>('/api/v1/organization')),
        firstValueFrom(this.http.get<ConsolidatedOrganizationSummary>('/api/v1/organization/consolidation/summary')),
        firstValueFrom(this.http.get<IntercompanyTransaction[]>('/api/v1/organization/intercompany')),
        firstValueFrom(this.http.get<Branch[]>('/api/v1/organization/branches/permitted')).catch(() => []),
      ]);
      this.companies.set(data.companies);
      this.branches.set(data.branches);
      this.permittedBranches.set(permitted && permitted.length > 0 ? permitted : data.branches);
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

  switchTab(tab: 'companies' | 'branches' | 'warehouses' | 'departments' | 'transfers' | 'consolidation' | 'intercompany') {
    this.activeTab.set(tab);
    if (tab === 'transfers') {
      void this.loadTransfers();
    } else if (tab === 'consolidation') {
      void this.loadGroupReport();
    }
  }

  openNew() {
    this.editingId.set(null);
    const tab = this.activeTab();
    if (tab === 'companies') {
      this.companyForm.reset({ code: '', name: '', taxNumber: '', commercialRegistry: '', active: true });
    } else if (tab === 'branches') {
      this.branchForm.reset({
        companyId: this.companies()[0]?.id ?? '',
        code: '',
        name: '',
        location: '',
        isMainBranch: false,
        phone: '',
        email: '',
        taxNumber: '',
        commercialRegistry: '',
        defaultWarehouseId: '',
        defaultCashboxId: '',
        defaultBankAccountId: '',
        defaultPosTerminalId: '',
        documentCodePrefix: '',
        active: true,
      });
    } else if (tab === 'warehouses') {
      this.warehouseForm.reset({ branchId: this.branches()[0]?.id ?? '', code: '', name: '', location: '', active: true });
    } else if (tab === 'departments') {
      this.departmentForm.reset({ companyId: this.companies()[0]?.id ?? '', code: '', name: '', managerId: '', active: true });
    }
    this.drawerOpen.set(true);
  }

  editBranch(b: Branch) {
    this.editingId.set(b.id);
    this.branchForm.reset({
      companyId: b.companyId,
      code: b.code,
      name: b.name,
      location: b.location ?? '',
      isMainBranch: b.isMainBranch ?? false,
      phone: b.phone ?? '',
      email: b.email ?? '',
      taxNumber: b.taxNumber ?? '',
      commercialRegistry: b.commercialRegistry ?? '',
      defaultWarehouseId: b.defaultWarehouseId ?? '',
      defaultCashboxId: b.defaultCashboxId ?? '',
      defaultBankAccountId: b.defaultBankAccountId ?? '',
      defaultPosTerminalId: b.defaultPosTerminalId ?? '',
      documentCodePrefix: b.documentCodePrefix ?? '',
      active: b.active,
    });
    this.drawerOpen.set(true);
  }

  closeDrawer() {
    this.drawerOpen.set(false);
  }

  async viewBranchSummary(branchId: string) {
    this.loadingSummary.set(true);
    this.selectedBranchSummary.set(null);
    this.summaryModalOpen.set(true);
    try {
      const summary = await firstValueFrom(
        this.http.get<BranchControlSummary>(`/api/v1/organization/branches/${branchId}/control-summary`)
      );
      this.selectedBranchSummary.set(summary);
    } catch (e) {
      this.notification.error(apiErrorMessage(e, this.i18n));
    } finally {
      this.loadingSummary.set(false);
    }
  }

  closeSummaryModal() {
    this.summaryModalOpen.set(false);
  }

  // --- Transfers & In-Transit Logic ---
  async loadTransfers() {
    this.loadingTransfers.set(true);
    try {
      const [trfs, discs] = await Promise.all([
        firstValueFrom(this.http.get<StockTransferItem[]>('/api/v1/operations/transfers')).catch(() => []),
        firstValueFrom(this.http.get<StockTransferDiscrepancyItem[]>('/api/v1/operations/transfers/discrepancies')).catch(() => []),
      ]);
      this.transfers.set(trfs);
      this.discrepancies.set(discs);
    } catch (e) {
      this.notification.error(apiErrorMessage(e, this.i18n));
    } finally {
      this.loadingTransfers.set(false);
    }
  }

  openDispatchModal(transfer: StockTransferItem) {
    this.selectedTransferForDispatch.set(transfer);
    this.dispatchForm.reset({
      carrierName: transfer.carrierName || '',
      driverName: transfer.driverName || '',
      driverPhone: transfer.driverPhone || '',
      vehiclePlate: transfer.vehiclePlate || '',
      waybillNumber: transfer.waybillNumber || '',
      notes: transfer.notes || '',
    });
    this.dispatchModalOpen.set(true);
  }

  closeDispatchModal() {
    this.dispatchModalOpen.set(false);
  }

  async submitDispatch() {
    if (this.dispatchForm.invalid) return;
    const transfer = this.selectedTransferForDispatch();
    if (!transfer) return;
    this.dispatching.set(true);
    try {
      await firstValueFrom(
        this.http.post(`/api/v1/operations/transfers/${transfer.id}/dispatch`, this.dispatchForm.getRawValue())
      );
      this.notification.success(this.i18n.t('branchControl.dispatchedSuccess'));
      this.dispatchModalOpen.set(false);
      await this.loadTransfers();
    } catch (e) {
      this.notification.error(apiErrorMessage(e, this.i18n));
    } finally {
      this.dispatching.set(false);
    }
  }

  openInspectModal(transfer: StockTransferItem) {
    this.selectedTransferForInspect.set(transfer);
    const lines: InspectionLineState[] = (transfer.lines || []).map(l => ({
      lineId: l.id,
      itemCode: l.itemCode,
      itemName: l.itemName,
      shippedQty: l.shippedQuantity ?? l.quantity,
      receivedQuantity: l.shippedQuantity ?? l.quantity,
      damagedQuantity: 0,
      lostQuantity: 0,
      discrepancyReason: '',
      discrepancyNotes: '',
    }));
    this.inspectionLines.set(lines);
    this.inspectNotes.setValue('');
    this.inspectModalOpen.set(true);
  }

  closeInspectModal() {
    this.inspectModalOpen.set(false);
  }

  updateInspectionLine(index: number, field: keyof InspectionLineState, value: string | number) {
    const lines = [...this.inspectionLines()];
    lines[index] = { ...lines[index], [field]: value as never };
    this.inspectionLines.set(lines);
  }

  async submitReceiveInspection() {
    const transfer = this.selectedTransferForInspect();
    if (!transfer) return;
    this.receiving.set(true);
    try {
      const payload: ReceiveTransferPayload = {
        inspectionLines: this.inspectionLines().map(l => ({
          lineId: l.lineId,
          receivedQuantity: Number(l.receivedQuantity) || 0,
          damagedQuantity: Number(l.damagedQuantity) || 0,
          lostQuantity: Number(l.lostQuantity) || 0,
          discrepancyReason: l.discrepancyReason,
          discrepancyNotes: l.discrepancyNotes,
        })),
        notes: this.inspectNotes.value,
      };
      await firstValueFrom(
        this.http.post(`/api/v1/operations/transfers/${transfer.id}/receive-inspection`, payload)
      );
      this.notification.success(this.i18n.t('branchControl.receivedSuccess'));
      this.inspectModalOpen.set(false);
      await this.loadTransfers();
    } catch (e) {
      this.notification.error(apiErrorMessage(e, this.i18n));
    } finally {
      this.receiving.set(false);
    }
  }

  openResolveModal(discrepancy: StockTransferDiscrepancyItem) {
    this.selectedDiscrepancy.set(discrepancy);
    this.resolveForm.reset({
      resolutionStatus: 'RESOLVED',
      resolutionNotes: '',
    });
    this.resolveModalOpen.set(true);
  }

  closeResolveModal() {
    this.resolveModalOpen.set(false);
  }

  async submitResolve() {
    if (this.resolveForm.invalid) return;
    const disc = this.selectedDiscrepancy();
    if (!disc) return;
    this.resolving.set(true);
    try {
      await firstValueFrom(
        this.http.post(`/api/v1/operations/transfers/discrepancies/${disc.id}/resolve`, this.resolveForm.getRawValue())
      );
      this.notification.success(this.i18n.t('branchControl.resolvedSuccess'));
      this.resolveModalOpen.set(false);
      await this.loadTransfers();
    } catch (e) {
      this.notification.error(apiErrorMessage(e, this.i18n));
    } finally {
      this.resolving.set(false);
    }
  }

  // --- Consolidated Group Reporting Logic ---
  async loadGroupReport() {
    this.loadingReport.set(true);
    try {
      let params = `period=${encodeURIComponent(this.selectedPeriod())}`;
      if (this.selectedCompanyFilter()) {
        params += `&companyId=${encodeURIComponent(this.selectedCompanyFilter())}`;
      }
      if (this.selectedBranchFilter()) {
        params += `&branchId=${encodeURIComponent(this.selectedBranchFilter())}`;
      }
      const report = await firstValueFrom(
        this.http.get<ConsolidatedGroupReport>(`/api/v1/organization/consolidation/group-report?${params}`)
      );
      this.groupReport.set(report);
    } catch (e) {
      this.notification.error(apiErrorMessage(e, this.i18n));
    } finally {
      this.loadingReport.set(false);
    }
  }

  async exportGroupReport() {
    try {
      let params = `period=${encodeURIComponent(this.selectedPeriod())}&locale=${this.i18n.locale()}`;
      if (this.selectedCompanyFilter()) {
        params += `&companyId=${encodeURIComponent(this.selectedCompanyFilter())}`;
      }
      if (this.selectedBranchFilter()) {
        params += `&branchId=${encodeURIComponent(this.selectedBranchFilter())}`;
      }
      const blob = await firstValueFrom(
        this.http.get(`/api/v1/organization/consolidation/group-report/export?${params}`, { responseType: 'blob' })
      );
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `consolidated-group-report-${this.selectedPeriod()}.xlsx`;
      a.click();
      window.URL.revokeObjectURL(url);
    } catch (e) {
      this.notification.error(apiErrorMessage(e, this.i18n));
    }
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
      this.notification.success(this.i18n.t('org.companySaved')); 
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
      this.notification.success(this.i18n.t('org.branchSaved')); 
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
      this.notification.success(this.i18n.t('org.warehouseSaved')); 
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
      this.notification.success(this.i18n.t('org.departmentSaved')); 
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
