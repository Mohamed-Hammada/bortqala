import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { PayrollPage } from './payroll.page';
import { PayrollStore } from './payroll.store';
import { PayrollRow } from './payroll.models';

describe('PayrollPage', () => {
  let store: PayrollStore;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PayrollPage],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        PayrollStore,
      ],
    }).compileComponents();

    store = TestBed.inject(PayrollStore);
  });

  it('initializes and computes payroll metrics', () => {
    const fixture = TestBed.createComponent(PayrollPage);
    const component = fixture.componentInstance;

    const mockRow: PayrollRow = {
      id: 'row-1',
      employeeId: 'emp-1',
      employeeCode: 'EMP-01',
      employeeName: 'Ahmed Ali',
      categoryId: 'cat-1',
      categoryName: 'Admin',
      employmentType: 'FIXED',
      reportId: null,
      periodYear: 2026,
      periodMonth: 8,
      periodKind: 'MONTHLY',
      periodStart: '2026-08-01',
      periodEnd: '2026-08-31',
      baseSalary: 8000,
      attendanceBonus: 0,
      attendanceDeduction: 0,
      activeAdvancesBalance: 0,
      grossAmount: 10000,
      advancesDeducted: 0,
      otherDeductions: 500,
      bonuses: 2000,
      netAmount: 9500,
      paymentStatus: 'PENDING',
      paidAt: null,
      paymentMethod: 'BANK_TRANSFER',
      referenceCode: null,
      note: null,
      incompleteProfile: false,
      createdBy: 'ADMIN',
      createdAt: '2026-08-01',
      paidBy: null,
      reversedBy: null,
      reversedAt: null,
      reversalReason: null,
      version: 1,
    };

    component.store.data.set({
      periodYear: 2026,
      periodMonth: 8,
      periodStatus: 'DRAFT',
      summary: {
        totalEmployees: 1,
        paidCount: 0,
        pendingCount: 1,
        totalGrossAmount: 10000,
        totalPaidAmount: 0,
        totalPendingAmount: 9500,
        totalAdvancesDeducted: 0,
      },
      rows: [mockRow],
    });

    expect(component.store.data()?.summary.totalEmployees).toBe(1);
    expect(component.store.data()?.summary.pendingCount).toBe(1);
    expect(component.store.data()?.summary.totalPendingAmount).toBe(9500);
  });

  it('changes period correctly', () => {
    const fixture = TestBed.createComponent(PayrollPage);
    const component = fixture.componentInstance;

    vi.spyOn(component.store, 'load').mockResolvedValue();

    component.changePeriod('2026', '9');
    expect(component.year()).toBe(2026);
    expect(component.month()).toBe(9);
  });
});
