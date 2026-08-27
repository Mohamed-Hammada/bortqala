import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { vi } from 'vitest';
import { EmployeesPage, FORM_GROUPS } from './employees.page';
import { ConfirmDialogService } from '../../core/confirm-dialog.service';

describe('EmployeesPage drawer dirty-check', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EmployeesPage],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
  });

  function createPage() {
    const fixture = TestBed.createComponent(EmployeesPage);
    const component = fixture.componentInstance;
    const confirm = TestBed.inject(ConfirmDialogService);
    return { component, confirm };
  }

  it('closes immediately when the form is pristine', async () => {
    const { component, confirm } = createPage();
    component.drawerOpen.set(true);
    component.form.markAsPristine();
    await component.closeDrawer();
    expect(confirm.confirmState()).toBeNull();
    expect(component.drawerOpen()).toBe(false);
  });

  it('asks for confirmation before discarding dirty changes', async () => {
    const { component, confirm } = createPage();
    component.drawerOpen.set(true);
    component.form.controls.fullName.setValue('edited');
    component.form.markAsDirty();
    const promise = component.closeDrawer();
    const state = confirm.confirmState();
    expect(state).not.toBeNull();
    expect(state?.options.titleKey).toBe('common.unsavedTitle');
    expect(component.drawerOpen()).toBe(true);
    confirm.cancel();
    await promise;
  });

  it('closes only after the discard action is confirmed', async () => {
    const { component, confirm } = createPage();
    component.drawerOpen.set(true);
    component.form.controls.fullName.setValue('edited');
    component.form.markAsDirty();
    const promise = component.closeDrawer();
    confirm.proceed();
    await promise;
    expect(component.drawerOpen()).toBe(false);
  });

  it('keeps the drawer open when the confirm is cancelled', async () => {
    const { component, confirm } = createPage();
    component.drawerOpen.set(true);
    component.form.controls.fullName.setValue('edited');
    component.form.markAsDirty();
    const promise = component.closeDrawer();
    confirm.cancel();
    await promise;
    expect(component.drawerOpen()).toBe(true);
  });

  it('ignores re-entrant close calls while a confirm is showing', async () => {
    const { component, confirm } = createPage();
    component.drawerOpen.set(true);
    component.form.controls.fullName.setValue('edited');
    component.form.markAsDirty();
    const first = component.closeDrawer();
    const second = component.closeDrawer();
    confirm.cancel();
    await first;
    await second;
    expect(component.drawerOpen()).toBe(true);
  });

  it('blocks save when the end date precedes the start date', async () => {
    const { component } = createPage();
    component.form.patchValue({
      employeeCode: 'EMP-001',
      fullName: 'Test Employee',
      categoryId: 'cat-1',
      activeFrom: '2026-08-10',
      activeTo: '2026-08-01',
    });
    const notification = component.notification;
    vi.spyOn(notification, 'warning');
    await component.submit();
    expect(notification.warning).toHaveBeenCalledWith('employees.activeToBeforeActiveFrom');
  });

  it('opens contracts modal and loads contracts list', async () => {
    const { component } = createPage();
    const http = TestBed.inject(HttpTestingController);

    const emp: import('./employees.models').Employee = {
      id: 'emp-101',
      employeeCode: 'EMP-101',
      fullName: 'Ahmed Ali',
      deviceUserId: '101',
      categoryId: 'cat-1',
      categoryName: 'Engineering',
      employmentType: 'FIXED',
      baseSalary: 12000,
      activeFrom: 1000,
      activeTo: null,
      active: true,
      version: 1,
    };

    const promise = component.openContracts(emp);
    expect(component.contractsModalOpen()).toBe(true);

    const req = http.expectOne('/api/v1/employees/emp-101/contracts');
    req.flush([
      {
        id: 'cnt-1',
        contractNumber: 'CNT-2026-001',
        employeeId: 'emp-101',
        contractType: 'PERMANENT',
        status: 'ACTIVE',
        startDate: '2026-01-01',
        basicSalary: 10000,
        housingAllowance: 1500,
        transportationAllowance: 500,
        otherAllowances: 0,
        grossSalary: 12000,
        noticePeriodDays: 30,
        createdAt: 1000,
        updatedAt: 1000,
        version: 1,
      },
    ]);

    await promise;
    expect(component.contractsList().length).toBe(1);
    expect(component.contractsList()[0].contractNumber).toBe('CNT-2026-001');
  });

  it('shows the apply-deduction action only when a MANUAL policy resolves', async () => {
    const { component } = createPage();
    const http = TestBed.inject(HttpTestingController);
    expect(component.applyDeductionVisible()).toBe(false);

    const policiesReq = http.expectOne('/api/v1/workforce/advances/policies');
    policiesReq.flush([
      {
        scopeType: 'GLOBAL',
        deductionMode: 'MANUAL',
        deductionFrequency: 'MONTHLY',
        maxDeductionPercent: 50,
        defaultInstallments: 1,
        deferralPeriods: 0,
        version: 2,
        effectiveFrom: '2026-01-01',
        active: true,
      },
      {
        scopeType: 'EMPLOYEE_CATEGORY',
        scopeId: 'cat-auto',
        deductionMode: 'AUTO',
        deductionFrequency: 'MONTHLY',
        maxDeductionPercent: 50,
        defaultInstallments: 1,
        deferralPeriods: 0,
        version: 1,
        effectiveFrom: '2026-01-01',
        active: true,
      },
    ]);
    await Promise.resolve();

    expect(component.applyDeductionVisible()).toBe(true);
    const manualEmployee = {
      id: 'emp-m', employeeCode: 'EMP-M', fullName: 'Manual Emp', categoryId: 'cat-other',
      employmentType: 'FIXED', baseSalary: 5000, activeFrom: 1, activeTo: null, active: true, version: 1,
    } as import('./employees.models').Employee;
    const autoEmployee = {
      ...manualEmployee, id: 'emp-a', employeeCode: 'EMP-A', categoryId: 'cat-auto',
    } as import('./employees.models').Employee;
    component.store.items.set([manualEmployee, autoEmployee]);

    const affected = component.affectedEmployees().map((item) => item.id);
    expect(affected).toEqual(['emp-m']);
  });

  it('apply-deduction posts one request per affected employee and reports success', async () => {
    const { component, confirm } = createPage();
    const http = TestBed.inject(HttpTestingController);
    const policiesReq = http.expectOne('/api/v1/workforce/advances/policies');
    policiesReq.flush([
      {
        scopeType: 'GLOBAL',
        deductionMode: 'MANUAL',
        deductionFrequency: 'MONTHLY',
        maxDeductionPercent: 50,
        defaultInstallments: 1,
        deferralPeriods: 0,
        version: 1,
        effectiveFrom: '2026-01-01',
        active: true,
      },
    ]);
    await Promise.resolve();

    const employee = {
      id: 'emp-x', employeeCode: 'EMP-X', fullName: 'X', categoryId: 'cat-1',
      employmentType: 'FIXED', baseSalary: 5000, activeFrom: 1, activeTo: null, active: true, version: 1,
    } as import('./employees.models').Employee;
    component.store.items.set([employee]);
    vi.spyOn(component.notification, 'success');

    component.openApplyDeduction();
    const state = confirm.confirmState();
    expect(state).not.toBeNull();
    confirm.proceed();
    await Promise.resolve();
    await Promise.resolve();

    const year = new Date().getFullYear();
    const month = new Date().getMonth() + 1;
    const post = http.expectOne(`/api/v1/workforce/advances/apply-deduction`);
    expect(post.request.method).toBe('POST');
    expect(post.request.body).toEqual({ employeeId: 'emp-x', periodId: `${year}/${month}` });
    post.flush({
      employeeId: 'emp-x', periodId: `${year}/${month}`,
      appliedAmount: 250, duplicate: false, lines: [],
    });

    await Promise.resolve();
    await Promise.resolve();
    await Promise.resolve();
    expect(component.notification.success).toHaveBeenCalled();
  });
});

describe('EmployeesPage WP-20 form groups', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EmployeesPage],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
  });

  function createPage() {
    const fixture = TestBed.createComponent(EmployeesPage);
    const component = fixture.componentInstance;
    fixture.detectChanges();
    return { component, fixture };
  }

  it('FORM_GROUPS defines 7 groups', () => {
    expect(FORM_GROUPS.length).toBe(7);
    expect(FORM_GROUPS.map((g) => g.key)).toEqual([
      'identity', 'job', 'schedule', 'salary', 'contracts', 'biometric', 'dates',
    ]);
  });

  it('first two groups are expanded by default, rest collapsed', () => {
    const { component } = createPage();
    expect(component.isGroupExpanded('identity')).toBe(true);
    expect(component.isGroupExpanded('job')).toBe(true);
    expect(component.isGroupExpanded('schedule')).toBe(false);
    expect(component.isGroupExpanded('salary')).toBe(false);
    expect(component.isGroupExpanded('contracts')).toBe(false);
    expect(component.isGroupExpanded('biometric')).toBe(false);
    expect(component.isGroupExpanded('dates')).toBe(false);
  });

  it('toggleGroup flips the collapsed state', () => {
    const { component } = createPage();
    expect(component.isGroupExpanded('salary')).toBe(false);
    component.toggleGroup('salary');
    expect(component.isGroupExpanded('salary')).toBe(true);
    component.toggleGroup('salary');
    expect(component.isGroupExpanded('salary')).toBe(false);
  });

  it('requiredFieldCount returns correct counts', () => {
    const { component } = createPage();
    expect(component.requiredFieldCount('identity')).toBe(1);
    expect(component.requiredFieldCount('job')).toBe(1);
    expect(component.requiredFieldCount('dates')).toBe(1);
    expect(component.requiredFieldCount('schedule')).toBe(0);
  });

  it('previewOpen signal toggles the preview drawer', () => {
    const { component } = createPage();
    expect(component.previewOpen()).toBe(false);
    component.previewOpen.set(true);
    expect(component.previewOpen()).toBe(true);
  });
});
