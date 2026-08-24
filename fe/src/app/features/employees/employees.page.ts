import {
  ChangeDetectionStrategy,
  Component,
  HostListener,
  computed,
  inject,
  signal,
} from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { DecimalPipe } from '@angular/common';
import { dateInputToEpoch, epochToDateInput, formatDateReadable } from '../../core/date';
import { Employee, EmployeePayload, EmploymentType } from './employees.models';
import { EmployeesStore } from './employees.store';
import { I18nService } from '../../core/i18n.service';
import { NotificationService } from '../../core/notification.service';
import { TablePagination } from '../../shared/ui/table-pagination/pagination';
import { TablePaginationComponent } from '../../shared/ui/table-pagination/table-pagination.component';
import { SkeletonComponent } from '../../shared/ui/skeleton/skeleton.component';
import { EmptyStateComponent } from '../../shared/ui/empty-state/empty-state.component';
import { ModalDialogComponent } from '../../shared/ui/modal-dialog/modal-dialog.component';
import { AuthService } from '../../core/auth/auth.service';
import { ConfirmDialogService } from '../../core/confirm-dialog.service';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { Router } from '@angular/router';
import { apiErrorMessage } from '../../core/api-error';
import { WorkforceService } from '../workforce/data-access/workforce.service';
import { AdvancePolicy } from '../workforce/models/workforce.models';

@Component({
  selector: 'app-employees-page',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    TablePaginationComponent,
    DecimalPipe,
    SkeletonComponent,
    EmptyStateComponent,
    ModalDialogComponent,
  ],
  providers: [EmployeesStore],
  templateUrl: './employees.page.html',
  styleUrl: './employees.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EmployeesPage {
  readonly auth = inject(AuthService);
  readonly store = inject(EmployeesStore);
  readonly i18n = inject(I18nService);
  readonly notification = inject(NotificationService);
  private readonly confirm = inject(ConfirmDialogService);
  private readonly router = inject(Router);
  private readonly http = inject(HttpClient);
  private readonly workforceService = inject(WorkforceService);
  readonly drawerOpen = signal(false);
  readonly submitAttempted = signal(false);
  readonly pagination = new TablePagination();
  readonly editingId = signal<string | null>(null);
  readonly search = signal('');
  private closing = false;
  private biometricReturnUrl: string | null = null;
  private biometricMonth: string | null = null;

  // Contracts Workbench State
  readonly contractsModalOpen = signal(false);
  readonly selectedEmployeeForContracts = signal<Employee | null>(null);
  readonly contractsList = signal<import('./employees.models').EmployeeContract[]>([]);
  readonly loadingContracts = signal(false);
  readonly contractDrawerOpen = signal(false);
  readonly contractDrawerMode = signal<'CREATE' | 'AMEND' | 'TERMINATE'>('CREATE');
  readonly selectedContractForAction = signal<import('./employees.models').EmployeeContract | null>(null);

  readonly contractForm = new FormGroup({
    contractNumber: new FormControl('', { nonNullable: true }),
    contractType: new FormControl<import('./employees.models').ContractType>('PERMANENT', { nonNullable: true, validators: [Validators.required] }),
    startDate: new FormControl(new Date().toISOString().slice(0, 10), { nonNullable: true, validators: [Validators.required] }),
    endDate: new FormControl('', { nonNullable: true }),
    probationEndDate: new FormControl('', { nonNullable: true }),
    noticePeriodDays: new FormControl(30, { nonNullable: true, validators: [Validators.required, Validators.min(0)] }),
    basicSalary: new FormControl(0, { nonNullable: true, validators: [Validators.required, Validators.min(0)] }),
    housingAllowance: new FormControl(0, { nonNullable: true }),
    transportationAllowance: new FormControl(0, { nonNullable: true }),
    otherAllowances: new FormControl(0, { nonNullable: true }),
    jobTitle: new FormControl('', { nonNullable: true }),
    notes: new FormControl('', { nonNullable: true }),
  });

  readonly amendForm = new FormGroup({
    newContractNumber: new FormControl('', { nonNullable: true }),
    basicSalary: new FormControl(0, { nonNullable: true, validators: [Validators.required, Validators.min(0)] }),
    housingAllowance: new FormControl(0, { nonNullable: true }),
    transportationAllowance: new FormControl(0, { nonNullable: true }),
    otherAllowances: new FormControl(0, { nonNullable: true }),
    jobTitle: new FormControl('', { nonNullable: true }),
    endDate: new FormControl('', { nonNullable: true }),
    amendmentReason: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
  });

  readonly terminateForm = new FormGroup({
    terminationDate: new FormControl(new Date().toISOString().slice(0, 10), { nonNullable: true, validators: [Validators.required] }),
    reason: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
  });

  openEmployeeAdvances(): void {
    void this.router.navigate(['/workforce/advances'], {
      queryParams: { recipientType: 'EMPLOYEE' },
    });
  }

  // WP-07: manual advance deduction, gated on the resolved MANUAL policy
  readonly applyDeductionVisible = signal(false);
  readonly applyDeductionBusy = signal(false);
  private globalManual = false;
  private categoryPolicyModes = new Map<string, boolean>();

  private async loadDeductionGate(): Promise<void> {
    try {
      const policies = await firstValueFrom(this.workforceService.loadAdvancePolicies());
      const latestByScope = new Map<string, AdvancePolicy>();
      for (const policy of policies) {
        const key = `${policy.scopeType}:${policy.scopeId ?? ''}`;
        const existing = latestByScope.get(key);
        if (!existing || policy.version >= existing.version) latestByScope.set(key, policy);
      }
      this.globalManual = latestByScope.get('GLOBAL:')?.deductionMode === 'MANUAL';
      this.categoryPolicyModes = new Map(
        [...latestByScope.values()]
          .filter((policy) => policy.scopeType === 'EMPLOYEE_CATEGORY' && policy.scopeId)
          .map((policy) => [policy.scopeId as string, policy.deductionMode === 'MANUAL']),
      );
      this.applyDeductionVisible.set(
        this.globalManual || [...this.categoryPolicyModes.values()].some((manual) => manual),
      );
    } catch {
      this.applyDeductionVisible.set(false);
    }
  }

  affectedEmployees(): Employee[] {
    return this.store.items().filter((item) =>
      this.categoryPolicyModes.has(item.categoryId)
        ? this.categoryPolicyModes.get(item.categoryId) === true
        : this.globalManual,
    );
  }

  openApplyDeduction(): void {
    if (this.applyDeductionBusy()) return;
    const targets = this.affectedEmployees();
    if (!targets.length) {
      this.notification.info(this.i18n.t('employees.applyDeductionNoTargets'));
      return;
    }
    void this.confirm.confirmAndRun(
      {
        titleKey: 'employees.applyDeductionAction',
        messageKey: 'employees.applyDeductionConfirmBody',
        params: { count: targets.length },
        confirmKey: 'common.confirm',
      },
      async () => {
        this.applyDeductionBusy.set(true);
        try {
          const now = new Date();
          const periodId = `${now.getFullYear()}/${now.getMonth() + 1}`;
          let applied = 0;
          let firstError: unknown = null;
          for (const target of targets) {
            try {
              await firstValueFrom(this.workforceService.applyManualDeduction(target.id, periodId));
              applied++;
            } catch (error) {
              if (!firstError) firstError = error;
            }
          }
          if (applied > 0) {
            this.notification.success(this.i18n.t('employees.applyDeductionSuccess', { count: applied }));
          }
          if (firstError) {
            this.notification.error(apiErrorMessage(firstError));
          }
        } finally {
          this.applyDeductionBusy.set(false);
        }
      },
    );
  }

  isBiometricCategorySelected(): boolean {
    const selectedId = this.form.controls.categoryId.value;
    const cat = this.store.categories().find((c) => c.id === selectedId);
    return cat?.attendanceMode === 'BIOMETRIC';
  }

  readonly filtered = computed(() => {
    const q = this.search().trim().toLowerCase();
    return this.store
      .items()
      .filter(
        (item) =>
          !q ||
          item.fullName.toLowerCase().includes(q) ||
          item.employeeCode.toLowerCase().includes(q) ||
          (item.deviceUserId ?? '').toLowerCase().includes(q),
      );
  });

  readonly paged = computed(() => this.pagination.slice(this.filtered()));

  readonly form = new FormGroup({
    employeeCode: new FormControl('', { nonNullable: true }),
    fullName: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    deviceUserId: new FormControl('', { nonNullable: true }),
    categoryId: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    employmentType: new FormControl<EmploymentType>('FIXED', { nonNullable: true }),
    baseSalary: new FormControl<number>(0, { nonNullable: true, validators: [Validators.min(0)] }),
    activeFrom: new FormControl(new Date().toISOString().slice(0, 10), {
      nonNullable: true,
      validators: [Validators.required],
    }),
    activeTo: new FormControl('', { nonNullable: true }),
    active: new FormControl(true, { nonNullable: true }),
    version: new FormControl<number | null>(null),
  });

  constructor() {
    void this.reloadAndApplyBiometricPrefill();
    void this.loadDeductionGate();
    this.form.controls.categoryId.valueChanges.subscribe((catId) => {
      const cat = this.store.categories().find((c) => c.id === catId);
      if (cat?.attendanceMode === 'BIOMETRIC') {
        this.form.controls.deviceUserId.setValidators([Validators.required]);
      } else {
        this.form.controls.deviceUserId.clearValidators();
      }
      this.form.controls.deviceUserId.updateValueAndValidity();
    });
  }

  private async reloadAndApplyBiometricPrefill(): Promise<void> {
    await this.store.load();
    const qp = this.router.routerState.snapshot.root.queryParamMap;
    if (qp.get('fromBiometric') !== '1') return;
    this.biometricReturnUrl = qp.get('returnUrl');
    this.biometricMonth = qp.get('month');
    this.submitAttempted.set(false);
    this.editingId.set(null);
    this.form.reset({
      employeeCode: qp.get('employeeCode') ?? '',
      fullName: qp.get('fullName') ?? '',
      deviceUserId: qp.get('deviceUserId') ?? '',
      categoryId: this.store.categories().find((c) => c.attendanceMode === 'BIOMETRIC')?.id ?? this.store.categories()[0]?.id ?? '',
      employmentType: 'FIXED',
      baseSalary: 0,
      activeFrom: new Date().toISOString().slice(0, 10),
      activeTo: '',
      active: true,
      version: null,
    });
    const catId = this.form.controls.categoryId.value;
    const cat = this.store.categories().find((c) => c.id === catId);
    if (cat?.attendanceMode === 'BIOMETRIC') {
      this.form.controls.deviceUserId.setValidators([Validators.required]);
      this.form.controls.deviceUserId.updateValueAndValidity();
    }
    this.drawerOpen.set(true);
  }

  async reload(): Promise<void> {
    await this.store.load();
  }

  openNew() {
    this.submitAttempted.set(false);
    this.editingId.set(null);
    this.form.reset({
      employeeCode: '',
      fullName: '',
      deviceUserId: '',
      categoryId: this.store.categories()[0]?.id ?? '',
      employmentType: 'FIXED',
      baseSalary: 0,
      activeFrom: new Date().toISOString().slice(0, 10),
      activeTo: '',
      active: true,
      version: null,
    });
    this.drawerOpen.set(true);
  }

  openEdit(item: Employee) {
    this.submitAttempted.set(false);
    this.editingId.set(item.id);
    this.form.reset({
      employeeCode: item.employeeCode,
      fullName: item.fullName,
      deviceUserId: item.deviceUserId ?? '',
      categoryId: item.categoryId,
      employmentType: item.employmentType,
      baseSalary: item.baseSalary ?? 0,
      activeFrom: epochToDateInput(item.activeFrom),
      activeTo: item.activeTo ? epochToDateInput(item.activeTo) : '',
      active: item.active,
      version: item.version,
    });
    this.drawerOpen.set(true);
  }

  async submit() {
    this.submitAttempted.set(true);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const raw = this.form.getRawValue();
    if (raw.activeTo && dateInputToEpoch(raw.activeTo) < dateInputToEpoch(raw.activeFrom)) {
      this.notification.warning(this.i18n.t('employees.activeToBeforeActiveFrom'));
      return;
    }
    const payload: EmployeePayload = {
      ...raw,
      deviceUserId: raw.deviceUserId.trim() || null,
      baseSalary: Number(raw.baseSalary) || 0,
      activeFrom: dateInputToEpoch(raw.activeFrom),
      activeTo: raw.activeTo ? dateInputToEpoch(raw.activeTo) : null,
    };
    if (await this.store.save(this.editingId(), payload)) {
      this.notification.success(this.i18n.t(this.editingId() ? 'employees.updateSuccess' : 'employees.createSuccess') + ' ✓');
      this.submitAttempted.set(false);
      const returnUrl = this.biometricReturnUrl;
      const month = this.biometricMonth;
      this.biometricReturnUrl = null;
      this.biometricMonth = null;
      this.form.reset({
        ...payload,
        deviceUserId: payload.deviceUserId ?? '',
        activeFrom: epochToDateInput(payload.activeFrom),
        activeTo: payload.activeTo ? epochToDateInput(payload.activeTo) : '',
      });
      this.drawerOpen.set(false);
      if (returnUrl) {
        if (month) {
          const monthDate = new Date(month + '-01');
          const ym = `${monthDate.getFullYear()}-${String(monthDate.getMonth() + 1).padStart(2, '0')}`;
          void firstValueFrom(this.http.post('/api/v1/reporting/attendance/recalculate', { yearMonth: ym }));
        }
        void this.router.navigateByUrl(returnUrl);
      }
    }
  }

  deactivate(item: Employee) {
    void this.confirm.confirmAndRun(
      {
        titleKey: 'employees.deactivateTitle',
        messageKey: 'employees.deactivateConfirm',
        params: { name: item.fullName },
        confirmKey: 'employees.deactivate',
        danger: true,
        dangerMessageKey: 'employees.deactivateDanger',
        details: [
          { label: this.i18n.t('employees.employeeCode'), value: item.employeeCode },
          { label: this.i18n.t('employees.category'), value: item.categoryName },
        ],
      },
      async () => {
        await this.store.deactivate(item.id);
        this.notification.success(this.i18n.t('employees.deactivateSuccess') + ' ✓');
      },
    );
  }

  reactivate(item: Employee) {
    void this.confirm.confirmAndRun(
      {
        titleKey: 'employees.reactivateTitle',
        messageKey: 'employees.reactivateConfirm',
        params: { name: item.fullName },
        confirmKey: 'employees.reactivate',
        details: [
          { label: this.i18n.t('employees.employeeCode'), value: item.employeeCode },
          { label: this.i18n.t('employees.category'), value: item.categoryName },
        ],
      },
      async () => {
        await this.store.reactivate(item.id, this.payloadFrom(item));
        this.notification.success(this.i18n.t('employees.reactivateSuccess') + ' ✓');
      },
    );
  }

  private payloadFrom(item: Employee): EmployeePayload {
    return {
      employeeCode: item.employeeCode,
      fullName: item.fullName,
      deviceUserId: item.deviceUserId,
      categoryId: item.categoryId,
      employmentType: item.employmentType,
      baseSalary: item.baseSalary ?? 0,
      activeFrom: item.activeFrom,
      activeTo: item.activeTo,
      active: true,
      version: item.version,
    };
  }

  async closeDrawer(): Promise<void> {
    if (this.closing) return;
    if (this.hasUnsavedChanges()) {
      this.closing = true;
      await this.confirm.confirmAndRun(
        {
          titleKey: 'common.unsavedTitle',
          messageKey: 'common.unsavedMessage',
          confirmKey: 'common.discard',
          danger: true,
        },
        async () => {
          this.drawerOpen.set(false);
          this.submitAttempted.set(false);
        },
      );
      this.closing = false;
      return;
    }
    this.drawerOpen.set(false);
    this.submitAttempted.set(false);
  }

  hasUnsavedChanges(): boolean {
    return this.form.dirty && this.drawerOpen();
  }

  @HostListener('document:keydown', ['$event']) onKeyDown(event: KeyboardEvent): void {
    if (event.key === 'Escape' && this.drawerOpen()) {
      event.preventDefault();
      void this.closeDrawer();
    } else if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 's') {
      if (this.drawerOpen()) {
        event.preventDefault();
        void this.submit();
      }
    }
  }

  // ─── Contracts Workbench Methods ─────────────────────────
  async openContracts(emp: Employee) {
    this.selectedEmployeeForContracts.set(emp);
    this.contractsModalOpen.set(true);
    await this.loadContracts(emp.id);
  }

  closeContractsModal() {
    this.contractsModalOpen.set(false);
    this.selectedEmployeeForContracts.set(null);
    this.contractDrawerOpen.set(false);
  }

  async loadContracts(employeeId: string) {
    this.loadingContracts.set(true);
    try {
      const data = await firstValueFrom(
        this.http.get<import('./employees.models').EmployeeContract[]>(`/api/v1/employees/${employeeId}/contracts`)
      );
      this.contractsList.set(data);
    } catch {
      this.contractsList.set([]);
    } finally {
      this.loadingContracts.set(false);
    }
  }

  openCreateContract() {
    const emp = this.selectedEmployeeForContracts();
    this.contractDrawerMode.set('CREATE');
    this.contractForm.reset({
      contractNumber: '',
      contractType: 'PERMANENT',
      startDate: new Date().toISOString().slice(0, 10),
      endDate: '',
      probationEndDate: '',
      noticePeriodDays: 30,
      basicSalary: emp?.baseSalary ?? 0,
      housingAllowance: 0,
      transportationAllowance: 0,
      otherAllowances: 0,
      jobTitle: '',
      notes: '',
    });
    this.contractDrawerOpen.set(true);
  }

  openAmendContract(cnt: import('./employees.models').EmployeeContract) {
    this.selectedContractForAction.set(cnt);
    this.contractDrawerMode.set('AMEND');
    this.amendForm.reset({
      newContractNumber: '',
      basicSalary: cnt.basicSalary,
      housingAllowance: cnt.housingAllowance,
      transportationAllowance: cnt.transportationAllowance,
      otherAllowances: cnt.otherAllowances,
      jobTitle: cnt.jobTitle ?? '',
      endDate: cnt.endDate ?? '',
      amendmentReason: '',
    });
    this.contractDrawerOpen.set(true);
  }

  openTerminateContract(cnt: import('./employees.models').EmployeeContract) {
    this.selectedContractForAction.set(cnt);
    this.contractDrawerMode.set('TERMINATE');
    this.terminateForm.reset({
      terminationDate: new Date().toISOString().slice(0, 10),
      reason: '',
    });
    this.contractDrawerOpen.set(true);
  }

  closeContractDrawer() {
    this.contractDrawerOpen.set(false);
    this.selectedContractForAction.set(null);
  }

  async submitContract() {
    if (this.contractForm.invalid) return;
    const emp = this.selectedEmployeeForContracts();
    if (!emp) return;
    try {
      const val = this.contractForm.getRawValue();
      await firstValueFrom(this.http.post(`/api/v1/employees/${emp.id}/contracts`, val));
      this.notification.success(this.i18n.t('contracts.saved'));
      this.closeContractDrawer();
      await this.loadContracts(emp.id);
    } catch (e) {
      this.notification.error(apiErrorMessage(e, this.i18n));
    }
  }

  async submitAmend() {
    if (this.amendForm.invalid) return;
    const cnt = this.selectedContractForAction();
    const emp = this.selectedEmployeeForContracts();
    if (!cnt || !emp) return;
    try {
      const val = this.amendForm.getRawValue();
      await firstValueFrom(this.http.post(`/api/v1/employees/contracts/${cnt.id}/amend`, val));
      this.notification.success(this.i18n.t('contracts.amended'));
      this.closeContractDrawer();
      await this.loadContracts(emp.id);
    } catch (e) {
      this.notification.error(apiErrorMessage(e, this.i18n));
    }
  }

  async submitTerminate() {
    if (this.terminateForm.invalid) return;
    const cnt = this.selectedContractForAction();
    const emp = this.selectedEmployeeForContracts();
    if (!cnt || !emp) return;
    try {
      const val = this.terminateForm.getRawValue();
      await firstValueFrom(this.http.post(`/api/v1/employees/contracts/${cnt.id}/terminate`, val));
      this.notification.success(this.i18n.t('contracts.terminated'));
      this.closeContractDrawer();
      await this.loadContracts(emp.id);
    } catch (e) {
      this.notification.error(apiErrorMessage(e, this.i18n));
    }
  }

  typeLabel(value: EmploymentType) {
    return this.i18n.t(value === 'FIXED' ? 'employment.fixed' : 'employment.daily');
  }

  contractTypeLabel(value: import('./employees.models').ContractType) {
    switch (value) {
      case 'PERMANENT': return this.i18n.t('contracts.permanent');
      case 'FIXED_TERM': return this.i18n.t('contracts.fixedTerm');
      case 'PROBATIONARY': return this.i18n.t('contracts.probationary');
      case 'PART_TIME': return this.i18n.t('contracts.partTime');
      case 'CONSULTANT': return this.i18n.t('contracts.consultant');
      case 'SEASONAL': return this.i18n.t('contracts.seasonal');
    }
  }

  contractStatusLabel(value: import('./employees.models').ContractStatus) {
    switch (value) {
      case 'ACTIVE': return this.i18n.t('contracts.statusActive');
      case 'AMENDED': return this.i18n.t('contracts.statusAmended');
      case 'EXPIRED': return this.i18n.t('contracts.statusExpired');
      case 'TERMINATED': return this.i18n.t('contracts.statusTerminated');
      default: return value;
    }
  }

  date(value: number) {
    return formatDateReadable(value, this.i18n.locale());
  }
}
