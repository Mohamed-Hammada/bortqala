import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { EssPageComponent } from './ess.page';
import { EssService } from './ess.service';
import { I18nService } from '../../core/i18n.service';

describe('EssPageComponent', () => {
  let component: EssPageComponent;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EssPageComponent],
      providers: [
        EssService,
        I18nService,
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(EssPageComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should initialize and load employee profile by default', async () => {
    expect(component.activeTab()).toBe('overview');

    const loadPromise = component.loadData();

    const reqProfile = httpMock.expectOne('/api/v1/ess/profile');
    expect(reqProfile.request.method).toBe('GET');
    reqProfile.flush({
      employeeId: 'emp-1',
      employeeCode: 'EMP-001',
      fullName: 'Ahmed Ali',
      categoryId: 'CAT-1',
      categoryName: 'Engineering',
      employmentType: 'FIXED',
      activeFrom: '2025-01-01',
      baseSalary: 10000,
      annualLeaveRemainingDays: 21.0,
      pendingLeavesCount: 1,
      pendingAdvancesCount: 0,
      currentMonthPunchesCount: 15,
      lastPunchTime: '2026-08-30 08:00',
      lastPunchType: 'SELFIE',
    });

    await loadPromise;
    expect(component.ess.profile()?.fullName).toBe('Ahmed Ali');
    expect(component.ess.profile()?.annualLeaveRemainingDays).toBe(21.0);
  });

  it('should switch to payslips tab and load payslips', async () => {
    const tabPromise = component.setTab('payslips');
    expect(component.activeTab()).toBe('payslips');

    const reqPayslips = httpMock.expectOne('/api/v1/ess/payslips');
    expect(reqPayslips.request.method).toBe('GET');
    reqPayslips.flush([
      {
        paymentId: 'pay-1',
        periodYear: 2026,
        periodMonth: 8,
        periodKind: 'MONTHLY',
        periodStart: '2026-08-01',
        periodEnd: '2026-08-31',
        grossTotal: 10000,
        totalDeductions: 500,
        netPay: 9500,
        paymentStatus: 'PAID',
      },
    ]);

    await tabPromise;
    expect(component.ess.payslips().length).toBe(1);
    expect(component.ess.payslips()[0].netPay).toBe(9500);
  });

  it('should switch to advances tab and load advance plans', async () => {
    const tabPromise = component.setTab('advances');
    expect(component.activeTab()).toBe('advances');

    const reqAdvances = httpMock.expectOne('/api/v1/ess/advances');
    expect(reqAdvances.request.method).toBe('GET');
    reqAdvances.flush([
      {
        id: 'adv-1',
        amount: 3000,
        totalInstallments: 3,
        installmentAmount: 1000,
        remainingBalance: 2000,
        status: 'ACTIVE',
        firstInstallmentDate: '2026-09-01',
        createdAt: 1000,
      },
    ]);

    await tabPromise;
    expect(component.ess.advances().length).toBe(1);
    expect(component.ess.advances()[0].installmentAmount).toBe(1000);
  });
});
