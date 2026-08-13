import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { DecimalPipe } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { firstValueFrom } from 'rxjs';
import { I18nService } from '../../../core/i18n.service';
import { NotificationService } from '../../../core/notification.service';
import { apiErrorMessage } from '../../../core/api-error';
import { formatDate } from '../../../core/date';
import { downloadBlob } from '../../../core/download';
import { ConfirmDialogService } from '../../../core/confirm-dialog.service';
import { ModalDialogComponent } from '../../../shared/ui/modal-dialog/modal-dialog.component';
import { BudgetService } from './budget.service';
import {
  BudgetPayload,
  BudgetPeriodType,
  BudgetResponse,
  BudgetRevision,
  BudgetStatusResponse,
  Currency,
  Department,
  EncumbranceResponse,
} from './budget.models';

@Component({
  selector: 'app-budgets-page',
  imports: [ReactiveFormsModule, DecimalPipe, ModalDialogComponent],
  templateUrl: './budgets.page.html',
  styleUrl: './budgets.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BudgetsPage {
  readonly http = inject(HttpClient);
  readonly i18n = inject(I18nService);
  readonly notification = inject(NotificationService);
  readonly budgetService = inject(BudgetService);
  private readonly confirm = inject(ConfirmDialogService);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly submitting = signal(false);
  readonly activeTab = signal<'budgets' | 'status' | 'encumbrances'>('budgets');
  readonly yearFilter = signal<number | null>(null);
  readonly drawerOpen = signal(false);
  readonly editingId = signal<string | null>(null);
  readonly revisionBudget = signal<BudgetResponse | null>(null);
  readonly revisions = signal<BudgetRevision[]>([]);
  readonly revisionOpen = signal(false);

  readonly budgets = signal<BudgetResponse[]>([]);
  readonly status = signal<BudgetStatusResponse[]>([]);
  readonly encumbrances = signal<EncumbranceResponse[]>([]);
  readonly departments = signal<Department[]>([]);
  readonly currencies = signal<Currency[]>([]);

  readonly budgetForm = new FormGroup({
    fiscalYear: new FormControl(new Date().getFullYear(), { nonNullable: true, validators: [Validators.required, Validators.min(2000)] }),
    periodType: new FormControl<BudgetPeriodType>('ANNUAL', { nonNullable: true }),
    periodMonth: new FormControl<number | null>(null, { nonNullable: true }),
    departmentId: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    plannedAmount: new FormControl(0, { nonNullable: true, validators: [Validators.required, Validators.min(0)] }),
    currencyCode: new FormControl('EGP', { nonNullable: true, validators: [Validators.required] }),
    blocking: new FormControl(true, { nonNullable: true }),
    active: new FormControl(true, { nonNullable: true }),
    revisionApprovalRequired: new FormControl(true, { nonNullable: true }),
  });

  readonly revisionForm = new FormGroup({
    newAmount: new FormControl(0, { nonNullable: true, validators: [Validators.required, Validators.min(0)] }),
    reason: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
  });

  readonly yearOptions = computed(() => {
    const years = new Set(this.budgets().map((budget) => budget.fiscalYear));
    return [...years].sort((a, b) => b - a);
  });

  readonly months = computed(() => Array.from({ length: 12 }, (_, index) => index + 1));

  readonly filteredStatus = computed(() => {
    const year = this.yearFilter();
    return this.status().filter((item) => year === null || item.fiscalYear === year);
  });

  constructor() {
    this.budgetForm.controls.periodType.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.onPeriodTypeChanged());
    void this.load();
  }

  async load() {
    this.loading.set(true);
    this.error.set(null);
    try {
      await Promise.all([
        this.loadBudgets(),
        this.loadStatus(),
        this.loadEncumbrances(),
        this.loadDepartments(),
        this.loadCurrencies(),
      ]);
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    } finally {
      this.loading.set(false);
    }
  }

  async loadBudgets() {
    this.budgets.set(await this.budgetService.listBudgets() ?? []);
  }

  async loadStatus() {
    this.status.set(await this.budgetService.budgetStatus() ?? []);
  }

  async loadEncumbrances() {
    this.encumbrances.set(await this.budgetService.listEncumbrances() ?? []);
  }

  async loadDepartments() {
    const departments = await this.budgetService.listDepartments() ?? [];
    this.departments.set(departments.filter((department) => department.active));
  }

  async loadCurrencies() {
    const currencies = await firstValueFrom(this.http.get<Currency[]>('/api/v1/finance/currencies')) ?? [];
    this.currencies.set(currencies.filter((currency) => currency.active));
    const base = currencies.find((currency) => currency.isBase);
    if (base) this.budgetForm.controls.currencyCode.setValue(base.code);
  }

  openNew() {
    this.editingId.set(null);
    this.budgetForm.reset({
      fiscalYear: new Date().getFullYear(),
      periodType: 'ANNUAL',
      periodMonth: null,
      departmentId: this.departments()[0]?.id ?? '',
      plannedAmount: 0,
      currencyCode: this.currencies().find((currency) => currency.isBase)?.code ?? 'EGP',
      blocking: true,
      active: true,
      revisionApprovalRequired: true,
    });
    this.drawerOpen.set(true);
  }

  openEdit(budget: BudgetResponse) {
    this.editingId.set(budget.id);
    this.budgetForm.reset({
      fiscalYear: budget.fiscalYear,
      periodType: budget.periodType,
      periodMonth: budget.periodMonth,
      departmentId: budget.departmentId,
      plannedAmount: budget.plannedAmount,
      currencyCode: budget.currencyCode,
      blocking: budget.blocking,
      active: budget.active,
      revisionApprovalRequired: budget.revisionApprovalRequired ?? true,
    });
    this.drawerOpen.set(true);
  }

  closeDrawer() {
    this.drawerOpen.set(false);
  }

  onPeriodTypeChanged(): void {
    const type = this.budgetForm.controls.periodType.value;
    if (type === 'MONTHLY') {
      if (!this.budgetForm.controls.periodMonth.value) {
        this.budgetForm.controls.periodMonth.setValue(new Date().getMonth() + 1);
      }
    } else {
      this.budgetForm.controls.periodMonth.setValue(null);
    }
  }

  async submitBudget() {
    if (this.submitting()) return;
    if (this.budgetForm.invalid) {
      this.budgetForm.markAllAsTouched();
      this.notification.warning(this.i18n.t('budget.fiscalYear') + ' *');
      return;
    }
    const value = this.budgetForm.getRawValue();
    if (value.periodType === 'MONTHLY' && !value.periodMonth) {
      this.notification.warning(this.i18n.t('budget.periodMonth'));
      return;
    }
    const payload: BudgetPayload = {
      fiscalYear: value.fiscalYear,
      periodType: value.periodType,
      periodMonth: value.periodType === 'MONTHLY' ? value.periodMonth : null,
      departmentId: value.departmentId,
      plannedAmount: value.plannedAmount,
      currencyCode: value.currencyCode,
      blocking: value.blocking,
      active: value.active,
      revisionApprovalRequired: value.revisionApprovalRequired,
    };
    this.submitting.set(true);
    try {
      const editingId = this.editingId();
      if (editingId) {
        await this.budgetService.updateBudget(editingId, payload);
        this.notification.success(this.i18n.t('budget.updateSuccess'));
      } else {
        await this.budgetService.createBudget(payload);
        this.notification.success(this.i18n.t('budget.createSuccess'));
      }
      this.drawerOpen.set(false);
      await this.load();
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    } finally {
      this.submitting.set(false);
    }
  }

  deleteBudget(budget: BudgetResponse) {
    void this.confirm.confirmAndRun(
      {
        titleKey: 'budget.delete',
        messageKey: 'budget.confirmDelete',
        params: { department: budget.departmentName ?? budget.departmentId },
        confirmKey: 'budget.delete',
        danger: true,
        details: [
          { label: this.i18n.t('budget.fiscalYear'), value: String(budget.fiscalYear) },
          { label: this.i18n.t('budget.department'), value: budget.departmentName ?? budget.departmentId },
          { label: this.i18n.t('budget.plannedAmount'), value: `${budget.plannedAmount} ${budget.currencyCode}` },
        ],
      },
      async () => {
        try {
          await this.budgetService.deleteBudget(budget.id);
          this.notification.success(this.i18n.t('budget.deleteSuccess'));
          await this.load();
        } catch (e) {
          this.error.set(apiErrorMessage(e, this.i18n));
          throw e;
        }
      },
    );
  }

  async openRevisions(budget: BudgetResponse) {
    this.revisionBudget.set(budget);
    this.revisionForm.reset({ newAmount: budget.plannedAmount, reason: '' });
    this.revisionOpen.set(true);
    try { this.revisions.set(await this.budgetService.listRevisions(budget.id) ?? []); }
    catch (e) { this.notification.error(apiErrorMessage(e, this.i18n)); }
  }

  async requestRevision() {
    const budget = this.revisionBudget();
    if (!budget || this.revisionForm.invalid || this.submitting()) return;
    this.submitting.set(true);
    try {
      const value = this.revisionForm.getRawValue();
      await this.budgetService.requestRevision(budget.id, value.newAmount, value.reason);
      this.notification.success(this.i18n.t('budget.revisionRequested'));
      this.revisions.set(await this.budgetService.listRevisions(budget.id));
      await Promise.all([this.loadBudgets(), this.loadStatus()]);
    } catch (e) { this.notification.error(apiErrorMessage(e, this.i18n)); }
    finally { this.submitting.set(false); }
  }

  async decideRevision(revision: BudgetRevision, approve: boolean) {
    const budget = this.revisionBudget();
    if (!budget || this.submitting()) return;
    this.submitting.set(true);
    try {
      if (approve) await this.budgetService.approveRevision(budget.id, revision.id);
      else await this.budgetService.rejectRevision(budget.id, revision.id);
      this.notification.success(this.i18n.t(approve ? 'budget.revisionApproved' : 'budget.revisionRejected'));
      this.revisions.set(await this.budgetService.listRevisions(budget.id));
      await Promise.all([this.loadBudgets(), this.loadStatus()]);
    } catch (e) { this.notification.error(apiErrorMessage(e, this.i18n)); }
    finally { this.submitting.set(false); }
  }

  async exportExcel(): Promise<void> {
    try {
      const blob = await firstValueFrom(this.http.get('/api/v1/budget/export.xlsx', { responseType: 'blob' }));
      downloadBlob(blob, `budgets-${new Date().toISOString().slice(0, 10)}.xlsx`);
    } catch (e) {
      this.notification.error(apiErrorMessage(e, this.i18n));
    }
  }

  periodLabel(budget: Pick<BudgetResponse, 'periodType' | 'periodMonth'>): string {
    return budget.periodType === 'MONTHLY'
      ? `${this.i18n.t('budget.periodMonthly')} · ${budget.periodMonth ?? ''}`
      : this.i18n.t('budget.periodAnnual');
  }

  statusPeriodLabel(item: Pick<BudgetStatusResponse, 'periodType' | 'periodMonth'>): string {
    return item.periodType === 'MONTHLY'
      ? `${this.i18n.t('budget.periodMonthly')} · ${item.periodMonth ?? ''}`
      : this.i18n.t('budget.periodAnnual');
  }

  departmentName(id: string, name: string | null): string {
    return name ?? this.departments().find((department) => department.id === id)?.name ?? id;
  }

  encumbranceStatusLabel(status: string): string {
    return status === 'ACTIVE' ? this.i18n.t('budget.encumbranceActive') : this.i18n.t('budget.encumbranceReleasedStatus');
  }

  yesNo(value: boolean): string {
    return value ? this.i18n.t('export.value.yes') : this.i18n.t('export.value.no');
  }

  date(ms: number): string {
    return formatDate(ms);
  }

  formatAmount(value: number): string {
    return new Intl.NumberFormat('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(value);
  }
}
