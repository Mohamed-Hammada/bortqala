import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { vi } from 'vitest';
import { EmployeesPage } from './employees.page';
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
});
