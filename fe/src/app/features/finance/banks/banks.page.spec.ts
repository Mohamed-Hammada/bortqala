import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { I18nService } from '../../../core/i18n.service';
import { NotificationService } from '../../../core/notification.service';
import { BanksPage } from './banks.page';

describe('BanksPage reconciliation', () => {
  let fixture: ComponentFixture<BanksPage>;
  let component: BanksPage;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [BanksPage], providers: [
      provideHttpClient(), provideHttpClientTesting(),
      { provide: I18nService, useValue: { t: (key: string) => key, locale: () => 'en-US' } },
      { provide: NotificationService, useValue: { success: vi.fn(), error: vi.fn() } },
    ] }).compileComponents();
    fixture = TestBed.createComponent(BanksPage); component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController); flushLoad(); await fixture.whenStable(); fixture.detectChanges();
  });

  afterEach(() => { try { http.verify(); } finally { TestBed.resetTestingModule(); } });

  it('loads accounts, statements and cash position together', () => {
    expect(component.banks().length).toBe(1);
    expect(component.statements()[0].statementReference).toBe('ST-1');
    expect(component.cashPosition()?.totalsByCurrency['EGP']).toBe(895);
  });

  it('imports a real CSV multipart statement with balance parameters', async () => {
    component.openImport();
    component.importForm.setValue({ bankAccountId: 'bank-1', statementReference: 'ST-2', openingBalance: 1000, closingBalance: 900 });
    component.importFile.set(new File(['date,description,amount\n2026-08-10,Payment,-100'], 'statement.csv', { type: 'text/csv' }));
    const promise = component.importStatement();
    const request = http.expectOne(req => req.url === '/api/v1/finance/bank-reconciliation/statements/import');
    expect(request.request.body instanceof FormData).toBe(true);
    expect(request.request.params.get('statementReference')).toBe('ST-2');
    request.flush(workbench());
    await Promise.resolve(); await Promise.resolve(); flushLoad(); await promise;
    expect(component.workbench()?.statement.id).toBe('statement-1');
  });

  it('runs idempotent auto-match and refreshes the page data', async () => {
    component.workbench.set(workbench() as never);
    const promise = component.autoMatch();
    const request = http.expectOne('/api/v1/finance/bank-reconciliation/statements/statement-1/auto-match');
    expect(request.request.body.operationId).toBeTruthy();
    request.flush(workbench());
    await Promise.resolve(); await Promise.resolve(); flushLoad(); await promise;
  });

  it('prefills a suggested partial allocation without reimplementing matching rules', () => {
    const view = workbench(); const line = view.lines[0]; const candidate = line.suggestions[0];
    component.openMatch(line as never, candidate as never);
    expect(component.matchForm.controls.journalEntryId.value).toBe('journal-1');
    expect(component.matchForm.controls.amount.value).toBe(100);
  });

  function flushLoad(): void {
    http.expectOne('/api/v1/finance/banks').flush([{ id: 'bank-1', bankName: 'Bank', accountNumber: '123', currencyCode: 'EGP', active: true }]);
    http.expectOne('/api/v1/finance/accounts').flush([]);
    http.expectOne('/api/v1/finance/bank-reconciliation/statements').flush([workbench().statement]);
    http.expectOne('/api/v1/finance/bank-reconciliation/cash-position').flush({ accounts: [], totalsByCurrency: { EGP: 895 } });
  }

  function workbench() {
    return { statement: { id: 'statement-1', bankAccountId: 'bank-1', statementReference: 'ST-1', periodStart: 1,
      periodEnd: 2, openingBalance: 1000, closingBalance: 895, currencyCode: 'EGP', status: 'IN_PROGRESS',
      lineCount: 1, unmatchedCount: 1 }, lines: [{ id: 'line-1', transactionDate: 1, description: 'Payment',
      amount: -105, status: 'PARTIAL', matchedAmount: 0, remainingAmount: 105, matches: [], suggestions: [{
        journalEntryId: 'journal-1', entryNumber: 'JV-1', entryDate: 1, description: 'Payment', availableAmount: 100,
        score: 80, reason: 'AMOUNT_DATE_REFERENCE' }] }] };
  }
});
