import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { I18nService } from '../../../core/i18n.service';
import { AccountsPage } from './accounts.page';

describe('AccountsPage', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        AccountsPage,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: I18nService, useValue: { t: (key: string) => key, locale: () => 'en-US' } },
      ],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    try {
      http.verify();
    } finally {
      TestBed.resetTestingModule();
    }
  });

  it('loads accounts and cost centers on init', async () => {
    const page = TestBed.inject(AccountsPage);

    const r1 = http.expectOne('/api/v1/finance/accounts');
    const r2 = http.expectOne('/api/v1/finance/cost-centers');
    r1.flush([
      { id: 'acc-1', code: '1010', name: 'Cash', type: 'ASSET', isHeader: false, currency: 'EGP', active: true },
    ]);
    r2.flush([
      { id: 'cc-1', code: 'CC-01', name: 'Engineering', isHeader: false, active: true, createdAt: 1000, updatedAt: 1000 },
    ]);

    await Promise.resolve();
    await Promise.resolve();
    await Promise.resolve();

    expect(page.accounts().length).toBe(1);
    expect(page.costCenters().length).toBe(1);
  });

  it('switches to statements tab and loads cash flow statement', async () => {
    const page = TestBed.inject(AccountsPage);
    http.expectOne('/api/v1/finance/accounts').flush([]);
    http.expectOne('/api/v1/finance/cost-centers').flush([]);

    page.setTab('STATEMENTS');
    http.expectOne(req => req.url.includes('/api/v1/finance/reports/balance-sheet')).flush({
      totalAssets: 1000, totalLiabilities: 500, totalEquity: 500, netIncome: 0, balanced: true,
    });

    page.statementType.set('CASH_FLOW');
    const promise = page.loadStatement();

    http.expectOne(req => req.url.includes('/api/v1/finance/reports/cash-flow')).flush({
      operatingCashFlow: 3000,
      investingCashFlow: -1000,
      financingCashFlow: 0,
      netCashFlow: 2000,
      openingCashBalance: 1000,
      closingCashBalance: 3000,
    });

    await promise;
    expect(page.cashFlow()?.netCashFlow).toBe(2000);
    expect(page.cashFlow()?.closingCashBalance).toBe(3000);
  });
});
