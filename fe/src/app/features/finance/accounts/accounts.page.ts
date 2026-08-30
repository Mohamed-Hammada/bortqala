import { ChangeDetectionStrategy, Component, HostListener, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { I18nService } from '../../../core/i18n.service';
import { NotificationService } from '../../../core/notification.service';
import { apiErrorMessage } from '../../../core/api-error';
import { DecimalPipe } from '@angular/common';
import { ModalDialogComponent } from '../../../shared/ui/modal-dialog/modal-dialog.component';

export interface Account {
  id: string;
  code: string;
  name: string;
  type: string;
  parentId?: string;
  isHeader: boolean;
  currency: string;
  active: boolean;
}

export interface CostCenter {
  id: string;
  code: string;
  name: string;
  parentId?: string | null;
  managerUserId?: string | null;
  isHeader: boolean;
  active: boolean;
  effectiveStartDate?: number | null;
  effectiveEndDate?: number | null;
  glAllocationRule?: string | null;
  createdAt: number;
  updatedAt: number;
}

export interface BalanceSheetReport {
  totalAssets: number;
  totalLiabilities: number;
  totalEquity: number;
  netIncome: number;
  balanced: boolean;
}

export interface IncomeStatementReport {
  totalRevenue: number;
  totalExpenses: number;
  netIncome: number;
}

export interface CashFlowPeriodComparison {
  startDate: string;
  endDate: string;
  operatingCashFlow: number;
  investingCashFlow: number;
  financingCashFlow: number;
  netCashFlow: number;
}

export interface CashFlowReport {
  operatingCashFlow: number;
  investingCashFlow: number;
  financingCashFlow: number;
  netCashFlow: number;
  openingCashBalance: number;
  closingCashBalance: number;
  reconciled: boolean;
  comparative: CashFlowPeriodComparison | null;
}

@Component({
  selector: 'app-accounts-page',
  imports: [ReactiveFormsModule, DecimalPipe, ModalDialogComponent],
  templateUrl: './accounts.page.html',
  styleUrl: './accounts.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AccountsPage {
  readonly http = inject(HttpClient);
  readonly i18n = inject(I18nService);
  readonly notification = inject(NotificationService);

  readonly activeTab = signal<'COA' | 'COST_CENTERS' | 'STATEMENTS'>('COA');
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly accounts = signal<Account[]>([]);
  readonly costCenters = signal<CostCenter[]>([]);
  readonly drawerOpen = signal(false);
  readonly costCenterModalOpen = signal(false);
  readonly editingId = signal<string | null>(null);
  readonly editingCostCenterId = signal<string | null>(null);

  // Statements
  readonly statementType = signal<'BALANCE_SHEET' | 'INCOME_STATEMENT' | 'CASH_FLOW'>('BALANCE_SHEET');
  readonly asOfDate = signal<string>(new Date().toISOString().substring(0, 10));
  readonly startDate = signal<string>(new Date(new Date().getFullYear(), 0, 1).toISOString().substring(0, 10));
  readonly endDate = signal<string>(new Date().toISOString().substring(0, 10));
  readonly balanceSheet = signal<BalanceSheetReport | null>(null);
  readonly incomeStatement = signal<IncomeStatementReport | null>(null);
  readonly cashFlow = signal<CashFlowReport | null>(null);

  readonly form = new FormGroup({
    code: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    name: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    type: new FormControl('ASSET', { nonNullable: true, validators: [Validators.required] }),
    parentId: new FormControl('', { nonNullable: true }),
    isHeader: new FormControl(false, { nonNullable: true }),
    currency: new FormControl('EGP', { nonNullable: true }),
    active: new FormControl(true, { nonNullable: true }),
  });

  readonly costCenterForm = new FormGroup({
    code: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    name: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    parentId: new FormControl('', { nonNullable: true }),
    managerUserId: new FormControl('', { nonNullable: true }),
    isHeader: new FormControl(false, { nonNullable: true }),
    active: new FormControl(true, { nonNullable: true }),
    glAllocationRule: new FormControl('', { nonNullable: true }),
  });

  constructor() {
    void this.load();
  }

  async load() {
    this.loading.set(true);
    this.error.set(null);
    try {
      const [accs, ccs] = await Promise.all([
        firstValueFrom(this.http.get<Account[]>('/api/v1/finance/accounts')),
        firstValueFrom(this.http.get<CostCenter[]>('/api/v1/finance/cost-centers')),
      ]);
      this.accounts.set(accs);
      this.costCenters.set(ccs);
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    } finally {
      this.loading.set(false);
    }
  }

  setTab(tab: 'COA' | 'COST_CENTERS' | 'STATEMENTS') {
    this.activeTab.set(tab);
    if (tab === 'STATEMENTS') {
      void this.loadStatement();
    }
  }

  async loadStatement() {
    try {
      if (this.statementType() === 'BALANCE_SHEET') {
        const res = await firstValueFrom(
          this.http.get<BalanceSheetReport>('/api/v1/finance/reports/balance-sheet', {
            params: { asOfDate: this.asOfDate() },
          }),
        );
        this.balanceSheet.set(res);
      } else if (this.statementType() === 'INCOME_STATEMENT') {
        const res = await firstValueFrom(
          this.http.get<IncomeStatementReport>('/api/v1/finance/reports/income-statement', {
            params: { startDate: this.startDate(), endDate: this.endDate() },
          }),
        );
        this.incomeStatement.set(res);
      } else {
        const res = await firstValueFrom(
          this.http.get<CashFlowReport>('/api/v1/finance/reports/cash-flow', {
            params: { startDate: this.startDate(), endDate: this.endDate() },
          }),
        );
        this.cashFlow.set(res);
      }
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    }
  }

  openNew() {
    this.editingId.set(null);
    this.form.reset({ code: '', name: '', type: 'ASSET', parentId: '', isHeader: false, currency: 'EGP', active: true });
    this.drawerOpen.set(true);
  }

  closeDrawer() {
    this.drawerOpen.set(false);
  }

  openNewCostCenter() {
    this.editingCostCenterId.set(null);
    this.costCenterForm.reset({
      code: '',
      name: '',
      parentId: '',
      managerUserId: '',
      isHeader: false,
      active: true,
      glAllocationRule: '',
    });
    this.costCenterModalOpen.set(true);
  }

  openEditCostCenter(cc: CostCenter) {
    this.editingCostCenterId.set(cc.id);
    this.costCenterForm.reset({
      code: cc.code,
      name: cc.name,
      parentId: cc.parentId ?? '',
      managerUserId: cc.managerUserId ?? '',
      isHeader: cc.isHeader,
      active: cc.active,
      glAllocationRule: cc.glAllocationRule ?? '',
    });
    this.costCenterModalOpen.set(true);
  }

  async submitCostCenter() {
    if (this.costCenterForm.invalid) return;
    try {
      const payload = this.costCenterForm.getRawValue();
      const id = this.editingCostCenterId();
      await firstValueFrom(
        id
          ? this.http.put(`/api/v1/finance/cost-centers/${id}`, payload)
          : this.http.post('/api/v1/finance/cost-centers', payload),
      );
      this.notification.success(this.i18n.t('common.save') + ' ✓');
      this.costCenterModalOpen.set(false);
      const ccs = await firstValueFrom(this.http.get<CostCenter[]>('/api/v1/finance/cost-centers'));
      this.costCenters.set(ccs);
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    }
  }

  async deleteCostCenter(id: string) {
    try {
      await firstValueFrom(this.http.delete(`/api/v1/finance/cost-centers/${id}`));
      this.notification.success(this.i18n.t('common.delete') + ' ✓');
      const ccs = await firstValueFrom(this.http.get<CostCenter[]>('/api/v1/finance/cost-centers'));
      this.costCenters.set(ccs);
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    }
  }

  @HostListener('document:keydown.escape', ['$event'])
  onEscapeKey(event: Event): void {
    if (!this.drawerOpen()) return;
    event.preventDefault();
    event.stopPropagation();
    this.closeDrawer();
  }

  async submit() {
    if (this.form.invalid) return;
    try {
      const payload = this.form.getRawValue();
      const id = this.editingId();
      await firstValueFrom(
        id
          ? this.http.put(`/api/v1/finance/accounts/${id}`, payload)
          : this.http.post('/api/v1/finance/accounts', payload),
      );
      this.notification.success(this.i18n.t('common.save') + ' ✓');
      this.drawerOpen.set(false);
      await this.load();
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    }
  }
}
