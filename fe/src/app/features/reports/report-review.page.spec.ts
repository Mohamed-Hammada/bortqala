import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute } from '@angular/router';
import { ReportReviewPage } from './report-review.page';
import { ReportsStore } from './reports.store';
import { I18nService } from '../../core/i18n.service';
import { NotificationService } from '../../core/notification.service';
import { AuthService } from '../../core/auth/auth.service';
import { ReportDetails, DailyResult } from './reports.models';

describe('ReportReviewPage', () => {
  let page: ReportReviewPage;
  let httpMock: HttpTestingController;
  let notification: NotificationService;

  const row: DailyResult = {
    id: 'row-1',
    employeeId: 'emp-1',
    employeeCode: 'QA-EMP-0807',
    employeeName: 'موظف اختبار',
    categoryId: 'cat-1',
    categoryName: 'فئة',
    workDate: 1785600000000,
    firstPunch: null,
    lastPunch: null,
    punchCount: 0,
    expectedMinutes: 480,
    workedMinutes: 0,
    manualWorkedMinutes: null,
    effectiveWorkedMinutes: 0,
    lateMinutes: 0,
    earlyLeaveMinutes: 0,
    overtimeMinutes: 0,
    status: 'MANUAL_ENTRY',
    warning: 'Manual attendance confirmation is required.',
    decision: null,
    decisionNote: null,
    decidedBy: null,
    decidedAt: null,
    ruleVersion: 'v1',
    version: 1,
  };

  function details(overrides: Partial<DailyResult> = {}): ReportDetails {
    return {
      report: {
        id: 'report-1', periodStart: 1785600000000, periodEnd: 1786896000000, payCycle: 'HALF_MONTHLY',
        status: 'IN_REVIEW', unresolvedCount: 1, createdBy: 'system', createdAt: 1785600000000,
        approvedBy: null, approvedAt: null, exportedAt: null, version: 1, generationHash: null,
      },
      categories: [],
      dailyResults: [{ ...row, ...overrides }],
      holidayProposals: [],
      dayAnomalies: [],
      allowedActions: ['DECIDE', 'BULK_DECISION'],
    };
  }
  const emptyWorkbench = { summary: { total: 0, open: 0, critical: 0, resolved: 0, affectedEmployees: 0 }, exceptions: [] };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        ReportsStore,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: I18nService, useValue: { t: (key: string) => key, locale: () => 'ar-EG' } },
        { provide: NotificationService, useValue: { success: vi.fn(), error: vi.fn(), warning: vi.fn() } },
        { provide: AuthService, useValue: { hasAnyRole: () => true } },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => 'report-1' } } } },
      ],
    });
    httpMock = TestBed.inject(HttpTestingController);
    notification = TestBed.inject(NotificationService);
    page = TestBed.runInInjectionContext(() => new ReportReviewPage());
    httpMock.expectOne('/api/v1/reports/report-1').flush(details());
    httpMock.expectOne('/api/v1/reports/report-1/attendance-exceptions').flush(emptyWorkbench);
  });

  afterEach(() => {
    httpMock.verify();
  });

  const flushAsync = () => new Promise<void>(resolve => setTimeout(resolve));

  it('keeps the note prompt open and surfaces the store error when the decision fails', async () => {
    page.decide(row, 'NORMAL_DAY');
    page.promptState()!.onConfirm('480');
    page.promptState()!.onConfirm('done');

    expect(page.savingRowId()).toBe('row-1');
    const put = httpMock.expectOne('/api/v1/reports/report-1/daily-results/row-1/decision');
    put.flush({ message: 'conflict' }, { status: 409, statusText: 'Conflict' });
    await flushAsync();

    expect(page.savingRowId()).toBeNull();
    expect(page.promptState()).not.toBeNull();
    expect(page.promptState()!.error).not.toBeNull();
    expect(notification.error).toHaveBeenCalled();
  });

  it('does not record a second decision while a row decision is in flight', async () => {
    page.decide(row, 'NORMAL_DAY');
    page.promptState()!.onConfirm('480');
    page.promptState()!.onConfirm('done');
    expect(page.savingRowId()).toBe('row-1');

    page.decide(row, 'DEDUCT');
    expect(page.savingRowId()).toBe('row-1');
    const put = httpMock.expectOne('/api/v1/reports/report-1/daily-results/row-1/decision');
    expect(put.request.body.decision).toBe('NORMAL_DAY');
    httpMock.expectNone('/api/v1/reports/report-1/daily-results/row-1/decision');
    put.flush({ message: 'conflict' }, { status: 409, statusText: 'Conflict' });
    await flushAsync();
  });

  it('closes the prompt only after the fresh GET confirms the persisted decision', async () => {
    page.decide(row, 'NORMAL_DAY');
    page.promptState()!.onConfirm('480');
    page.promptState()!.onConfirm('done');

    const put = httpMock.expectOne('/api/v1/reports/report-1/daily-results/row-1/decision');
    put.flush(details({ decision: 'NORMAL_DAY', manualWorkedMinutes: 480, decidedBy: 'reviewer' }));
    await flushAsync();
    const persisted = details({ decision: 'NORMAL_DAY', manualWorkedMinutes: 480, decidedBy: 'reviewer' });
    persisted.report.unresolvedCount = 0;
    httpMock.expectOne('/api/v1/reports/report-1').flush(persisted);
    httpMock.expectOne('/api/v1/reports/report-1/attendance-exceptions').flush(emptyWorkbench);
    await flushAsync();

    expect(page.savingRowId()).toBeNull();
    expect(page.promptState()).toBeNull();
    expect(notification.success).toHaveBeenCalled();
  });

  it('renders a retry-labeled confirm button after the decision fails', async () => {
    const fixture = TestBed.createComponent(ReportReviewPage);
    const fixturePage = fixture.componentInstance;
    httpMock.expectOne('/api/v1/reports/report-1').flush(details());
    httpMock.expectOne('/api/v1/reports/report-1/attendance-exceptions').flush(emptyWorkbench);
    fixture.detectChanges();
    fixturePage.decide(row, 'NORMAL_DAY');
    fixturePage.promptState()!.onConfirm('480');
    fixturePage.promptState()!.onConfirm('done');
    fixture.detectChanges();

    expect(fixturePage.savingRowId()).toBe('row-1');
    const put = httpMock.expectOne('/api/v1/reports/report-1/daily-results/row-1/decision');
    put.flush({ message: 'conflict' }, { status: 409, statusText: 'Conflict' });
    await flushAsync();
    fixture.detectChanges();

    expect(fixturePage.promptState()).not.toBeNull();
    expect(fixturePage.promptState()!.error).not.toBeNull();
    const button = fixture.nativeElement.querySelector('.modal-footer .button.gold') as HTMLButtonElement;
    expect(button).not.toBeNull();
    expect(button.disabled).toBe(false);
    expect(button.textContent?.trim()).toBe('common.retry');
  });

  it('filters critical exceptions and exposes their policy explanation', () => {
    page.store.exceptionWorkbench.set({ summary: { total: 2, open: 2, critical: 1, resolved: 0, affectedEmployees: 1 }, exceptions: [
      { id: 'ex-1', reportId: 'report-1', dailyResultId: 'row-1', employeeId: 'emp-1', employeeName: 'Employee', categoryId: 'cat-1', categoryName: 'Category', workDate: 1, exceptionType: 'NO_PUNCH', score: 100, metricMinutes: 480, explanationKey: 'attendance.exception.noPunch', policyName: 'Employee policy', policyVersion: 1, policySnapshotJson: '{}', policyScope: 'EMPLOYEE', payrollBlocking: true, status: 'OPEN', version: 0 },
      { id: 'ex-2', reportId: 'report-1', dailyResultId: 'row-1', employeeId: 'emp-1', employeeName: 'Employee', categoryId: 'cat-1', categoryName: 'Category', workDate: 1, exceptionType: 'LATE', score: 35, metricMinutes: 20, explanationKey: 'attendance.exception.late', policyName: 'Tenant policy', policyVersion: 1, policySnapshotJson: '{}', policyScope: 'TENANT', payrollBlocking: false, status: 'OPEN', version: 0 },
    ] });
    page.exceptionFilter.set('CRITICAL');
    expect(page.attendanceExceptions().map(item => item.id)).toEqual(['ex-1']);
    expect(page.exceptionSummary().critical).toBe(1);
    expect(page.exceptionTypeLabel(page.attendanceExceptions()[0])).toBe('attendance.exception.noPunch');
  });

  it('previews a bulk override without changing attendance data', async () => {
    page.selectedExceptionIds.set(['ex-1']);
    page.exceptionReason.set('device outage confirmed');
    page.exceptionResolution.set('MARK_PRESENT');
    const before = page.store.details()?.dailyResults[0].decision;
    const promise = page.previewExceptionBulk();
    const request = httpMock.expectOne('/api/v1/reports/report-1/attendance-exceptions/bulk-preview');
    expect(request.request.body).toEqual(expect.objectContaining({ exceptionIds: ['ex-1'], resolution: 'MARK_PRESENT', reason: 'device outage confirmed' }));
    request.flush({ selected: 1, editable: 1, alreadyClosed: 0, payrollBlockersCleared: 1, excludedIds: [] });
    await promise;
    expect(page.exceptionPreview()?.editable).toBe(1);
    expect(page.store.details()?.dailyResults[0].decision).toBe(before);
  });
});
