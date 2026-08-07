import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { BudgetsPage } from './budgets.page';
import { I18nService } from '../../../core/i18n.service';
import { NotificationService } from '../../../core/notification.service';
import { ConfirmDialogService } from '../../../core/confirm-dialog.service';
import { BudgetResponse, BudgetStatusResponse, EncumbranceResponse } from './budget.models';

describe('BudgetsPage', () => {
  let httpMock: HttpTestingController;
  let page: BudgetsPage;

  const budget = (overrides: Partial<BudgetResponse> = {}): BudgetResponse => ({
    id: 'budget-1',
    fiscalYear: 2026,
    periodType: 'ANNUAL',
    periodMonth: null,
    departmentId: 'dept-1',
    departmentName: 'الإدارة المالية',
    plannedAmount: 100000,
    currencyCode: 'EGP',
    blocking: true,
    active: true,
    createdAt: 1700000000000,
    updatedAt: 1700000000000,
    ...overrides,
  });

  const statusItem = (overrides: Partial<BudgetStatusResponse> = {}): BudgetStatusResponse => ({
    budgetId: 'budget-1',
    fiscalYear: 2026,
    periodType: 'ANNUAL',
    periodMonth: null,
    departmentId: 'dept-1',
    departmentName: 'الإدارة المالية',
    plannedAmount: 100000,
    committedAmount: 20000,
    actualAmount: 5000,
    availableAmount: 75000,
    utilizationPercent: 25,
    blocking: true,
    currencyCode: 'EGP',
    ...overrides,
  });

  const encumbrance = (overrides: Partial<EncumbranceResponse> = {}): EncumbranceResponse => ({
    id: 'enc-1',
    budgetId: 'budget-1',
    purchaseOrderId: 'po-1',
    purchaseOrderNumber: 'PO-2026-0001',
    documentType: 'PURCHASE_ORDER',
    status: 'ACTIVE',
    committedAmount: 20000,
    liquidatedAmount: 0,
    releasedAmount: 0,
    currencyCode: 'EGP',
    committedAt: 1700000000000,
    releasedAt: null,
    ...overrides,
  });

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BudgetsPage],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: I18nService,
          useValue: { t: (key: string) => key },
        },
        {
          provide: NotificationService,
          useValue: { success: () => undefined, error: () => undefined, warning: () => undefined },
        },
        ConfirmDialogService,
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(BudgetsPage);
    page = fixture.componentInstance;

    flushInitialLoad();
  });

  afterEach(() => {
    TestBed.resetTestingModule();
    httpMock.verify();
  });

  function flushInitialLoad() {
    httpMock.expectOne((req) => req.url === '/api/v1/budget/budgets').flush([budget()]);
    httpMock.expectOne((req) => req.url === '/api/v1/budget/status').flush([statusItem()]);
    httpMock.expectOne((req) => req.url === '/api/v1/budget/encumbrances').flush([encumbrance()]);
    httpMock.expectOne((req) => req.url === '/api/v1/organization/departments').flush([
      { id: 'dept-1', companyId: 'c-1', code: 'FIN', name: 'الإدارة المالية', managerId: null, active: true, createdAt: 0, updatedAt: 0 },
    ]);
    httpMock.expectOne((req) => req.url === '/api/v1/finance/currencies').flush([
      { id: 'cur-1', code: 'EGP', name: 'جنيه مصري', symbol: 'ج.م', isBase: true, exchangeRate: 1, active: true },
    ]);
  }

  async function yieldMicrotasks(): Promise<void> {
    await Promise.resolve();
    await Promise.resolve();
  }

  it('loads budgets, status, encumbrances, departments and currencies on init', () => {
    expect(page.budgets()).toHaveLength(1);
    expect(page.status()).toHaveLength(1);
    expect(page.encumbrances()).toHaveLength(1);
    expect(page.departments()).toHaveLength(1);
    expect(page.currencies()).toHaveLength(1);
    expect(page.loading()).toBe(false);
  });

  it('defaults the currency to the base currency after loading', () => {
    expect(page.budgetForm.controls.currencyCode.value).toBe('EGP');
  });

  it('creates an annual budget with the expected payload', async () => {
    page.budgetForm.patchValue({
      fiscalYear: 2027,
      periodType: 'ANNUAL',
      periodMonth: null,
      departmentId: 'dept-1',
      plannedAmount: 50000,
      currencyCode: 'EGP',
      blocking: true,
      active: true,
    });
    const submitting = page.submitBudget();

    const req = httpMock.expectOne((r) => r.url === '/api/v1/budget/budgets' && r.method === 'POST');
    expect(req.request.body).toEqual({
      fiscalYear: 2027,
      periodType: 'ANNUAL',
      periodMonth: null,
      departmentId: 'dept-1',
      plannedAmount: 50000,
      currencyCode: 'EGP',
      blocking: true,
      active: true,
    });
    req.flush(budget({ id: 'budget-2', fiscalYear: 2027, plannedAmount: 50000 }));
    await yieldMicrotasks();
    flushInitialLoad();
    await submitting;

    expect(page.drawerOpen()).toBe(false);
    expect(page.budgets()).toHaveLength(1);
  });

  it('sends a periodMonth for monthly budgets', async () => {
    page.budgetForm.patchValue({
      fiscalYear: 2027,
      periodType: 'MONTHLY',
      periodMonth: 3,
      departmentId: 'dept-1',
      plannedAmount: 10000,
      currencyCode: 'EGP',
      blocking: false,
      active: true,
    });
    const submitting = page.submitBudget();

    const req = httpMock.expectOne((r) => r.url === '/api/v1/budget/budgets' && r.method === 'POST');
    expect(req.request.body).toMatchObject({ periodType: 'MONTHLY', periodMonth: 3, blocking: false });
    req.flush(budget({ id: 'budget-3', periodType: 'MONTHLY', periodMonth: 3 }));
    await yieldMicrotasks();
    flushInitialLoad();
    await submitting;
  });

  it('switching to MONTHLY prefills the current month when unset', () => {
    page.budgetForm.controls.periodType.setValue('MONTHLY');
    expect(page.budgetForm.controls.periodMonth.value).toBe(new Date().getMonth() + 1);
  });

  it('switching back to ANNUAL clears the month', () => {
    page.budgetForm.controls.periodType.setValue('MONTHLY');
    page.budgetForm.controls.periodType.setValue('ANNUAL');
    expect(page.budgetForm.controls.periodMonth.value).toBeNull();
  });

  it('openEdit prefills the form from an existing budget', () => {
    page.openEdit(budget({ periodType: 'MONTHLY', periodMonth: 6, plannedAmount: 70000, blocking: false }));
    expect(page.editingId()).toBe('budget-1');
    expect(page.budgetForm.controls.fiscalYear.value).toBe(2026);
    expect(page.budgetForm.controls.periodType.value).toBe('MONTHLY');
    expect(page.budgetForm.controls.periodMonth.value).toBe(6);
    expect(page.budgetForm.controls.plannedAmount.value).toBe(70000);
    expect(page.budgetForm.controls.blocking.value).toBe(false);
  });

  it('deleteBudget confirms then sends DELETE and reloads', async () => {
    page.deleteBudget(budget());
    const confirmed = TestBed.inject(ConfirmDialogService);
    confirmed.proceed();
    await yieldMicrotasks();

    const req = httpMock.expectOne((r) => r.url === '/api/v1/budget/budgets/budget-1' && r.method === 'DELETE');
    req.flush(null);
    await yieldMicrotasks();
    flushInitialLoad();
    await yieldMicrotasks();
  });

  it('encumbrance status label resolves ACTIVE and RELEASED', () => {
    expect(page.encumbranceStatusLabel('ACTIVE')).toBe('budget.encumbranceActive');
    expect(page.encumbranceStatusLabel('RELEASED')).toBe('budget.encumbranceReleasedStatus');
  });

  it('yearOptions and filteredStatus respect the selected year', () => {
    page.budgets.set([budget({ fiscalYear: 2026 }), budget({ id: 'b2', fiscalYear: 2025 })]);
    page.status.set([statusItem({ budgetId: 'budget-1', fiscalYear: 2026 }), statusItem({ budgetId: 'b2', fiscalYear: 2025 })]);
    expect(page.yearOptions()).toEqual([2026, 2025]);

    page.yearFilter.set(2025);
    expect(page.filteredStatus()).toHaveLength(1);
    expect(page.filteredStatus()[0].fiscalYear).toBe(2025);

    page.yearFilter.set(null);
    expect(page.filteredStatus()).toHaveLength(2);
  });

  it('yesNo maps booleans to translated values', () => {
    expect(page.yesNo(true)).toBe('export.value.yes');
    expect(page.yesNo(false)).toBe('export.value.no');
  });
});
