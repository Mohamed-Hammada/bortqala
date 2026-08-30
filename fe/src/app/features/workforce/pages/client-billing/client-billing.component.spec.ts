import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ClientBillingLine, ClientBillingPeriodModel } from '../../models/workforce.models';
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { ClientBillingComponent } from './client-billing.component';
import { I18nService } from '../../../../core/i18n.service';
import { NotificationService } from '../../../../core/notification.service';
import { AuthService } from '../../../../core/auth/auth.service';

describe('ClientBillingComponent', () => {
  let httpMock: HttpTestingController;
  let component: ClientBillingComponent;
  const notify = { success: vi.fn(), error: vi.fn(), warning: vi.fn() };

  const client = { id: 'party-c1', code: 'C1', name: 'Client One', partyType: 'CUSTOMER', active: true };
  const supplier = { id: 'party-s1', code: 'S1', name: 'Supplier One', partyType: 'SUPPLIER', active: true };
  const category = { id: 'cat-1', code: 'LAB', name: 'Laborer', description: null, defaultDailyRate: 200, standardDailyHours: 8, defaultSettlementCycle: 'MONTHLY', status: 'ACTIVE', scope: 'WORKER', active: true, createdAt: 0, updatedAt: 0 };
  const rate = { id: 'rate-1', clientPartyId: 'party-c1', workerCategoryId: 'cat-1', categoryName: 'Laborer', dayRate: 220, effectiveFrom: '2026-06-01', effectiveTo: null, version: 1 };
  const period: ClientBillingPeriodModel = { id: 'bp-1', clientPartyId: 'party-c1', period: '2026-08', status: 'OPEN', invoiceId: null, invoiceNumber: null, totalAmount: 440 };
  const line: ClientBillingLine = { id: 'dl-1', workerId: 'w-1', workerCode: 'WRK-1', fullName: 'Ahmed Hassan', categoryId: 'cat-1', categoryName: 'Laborer', approvedDays: 2, dayRate: 220, amount: 440, wageCost: 360, varianceAmount: 50, lineStatus: 'BILLABLE', reason: null };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ClientBillingComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: I18nService, useValue: { t: (key: string) => key } },
        { provide: NotificationService, useValue: notify },
        { provide: AuthService, useValue: { hasPermission: () => true, hasAnyPermission: () => true } },
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(ClientBillingComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    flushInitialLoad();
  });

  afterEach(() => {
    TestBed.resetTestingModule();
    httpMock.verify();
  });

  function flushInitialLoad() {
    httpMock.expectOne((req) => req.url === '/api/v1/parties').flush([client, supplier]);
    httpMock.expectOne((req) => req.url === '/api/v1/workforce/categories').flush([category]);
  }

  it('loads clients and filters suppliers and inactive parties', () => {
    expect(component.workforceService.clients()).toHaveLength(1);
    expect(component.workforceService.clients()[0].id).toBe('party-c1');
  });

  it('loads rates scoped to the selected client', () => {
    component.onClientChange('party-c1');
    httpMock.expectOne((req) => req.url === '/api/v1/workforce/client-billing/rates' && req.params.get('clientPartyId') === 'party-c1')
      .flush([rate]);
    expect(component.workforceService.clientRates()).toHaveLength(1);
  });

  it('generates a billing draft and surfaces the review', async () => {
    component.selectedClientId.set('party-c1');
    component.selectedPeriod.set('2026-08');
    component.generate();
    const request = httpMock.expectOne('/api/v1/workforce/client-billing/generate');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ clientPartyId: 'party-c1', period: '2026-08' });
    request.flush({ period, lines: [line], totalApprovedDays: 2, totalBilledAmount: 440, totalWageCost: 360 });
    await Promise.resolve();
    expect(component.review()?.lines).toHaveLength(1);
    expect(component.review()?.totalBilledAmount).toBe(440);
  });

  it('guards generate when no client is selected', () => {
    component.selectedClientId.set('');
    component.selectedPeriod.set('2026-08');
    expect(component.canPrepare()).toBe(false);
    component.generate();
    expect(notify.error).toHaveBeenCalled();
  });

  it('confirms an open billing period into a single invoice', async () => {
    component.selectedClientId.set('party-c1');
    component.selectedPeriod.set('2026-08');
    component.review.set({ period, lines: [line], totalApprovedDays: 2, totalBilledAmount: 440, totalWageCost: 360 });
    component.confirm();
    const request = httpMock.expectOne('/api/v1/workforce/client-billing/party-c1/2026-08/confirm');
    expect(request.request.method).toBe('POST');
    request.flush({ id: 'bp-1', clientPartyId: 'party-c1', period: '2026-08', status: 'INVOICED', invoiceId: 'inv-1', invoiceNumber: 'CLB-202608-PARTY-', totalAmount: 440 });
    await Promise.resolve();
    expect(component.review()?.period.status).toBe('INVOICED');
    expect(component.review()?.period.invoiceNumber).toBe('CLB-202608-PARTY-');
  });

  it('flags missing-rate lines so confirmation is blocked', () => {
    const missing: ClientBillingLine = { ...line, lineStatus: 'MISSING_RATE', amount: 0, reason: 'No effective client rate' };
    component.review.set({ period, lines: [missing], totalApprovedDays: 2, totalBilledAmount: 0, totalWageCost: 360 });
    expect(component.hasMissingRates(component.review())).toBe(true);
    expect(component.reviewingLocked(component.review())).toBe(false);
  });

  it('exports the margin workbook as a blob', async () => {
    component.selectedClientId.set('party-c1');
    component.selectedPeriod.set('2026-08');
    component.exportMargin();
    const request = httpMock.expectOne('/api/v1/workforce/client-billing/party-c1/2026-08/margin/export.xlsx');
    expect(request.request.responseType).toBe('blob');
    request.flush(new Blob(['PK\u0003\u0004'], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' }));
    await Promise.resolve();
  });
});