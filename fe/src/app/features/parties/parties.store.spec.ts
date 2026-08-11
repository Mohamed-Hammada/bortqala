import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { I18nService } from '../../core/i18n.service';
import { BusinessPartyPayload, Supplier360 } from './parties.models';
import { PartiesStore } from './parties.store';

describe('PartiesStore supplier onboarding', () => {
  let store: PartiesStore;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [
      PartiesStore, provideHttpClient(), provideHttpClientTesting(),
      { provide: I18nService, useValue: { t: (key: string) => key, locale: () => 'en-US' } },
    ] });
    store = TestBed.inject(PartiesStore);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    try { http.verify(); } finally { TestBed.resetTestingModule(); }
  });

  it('creates suppliers through the onboarding request contract', async () => {
    const payload = supplierPayload();
    const promise = store.save(null, payload);
    const request = http.expectOne('/api/v1/parties/supplier-requests');
    expect(request.request.body.partyType).toBeUndefined();
    expect(request.request.body.taxId).toBe('TAX12345');
    expect(request.request.body.supplierCategory).toBe('PACKAGING');
    request.flush({});
    await Promise.resolve();
    http.expectOne('/api/v1/parties').flush([]);
    expect(await promise).toBe(true);
  });

  it('passes duplicate-check parameters to the backend', async () => {
    const promise = store.checkDuplicates('TAX12345', 'EG123', 'supplier-1');
    const request = http.expectOne(req => req.url === '/api/v1/parties/supplier-duplicates');
    expect(request.request.params.get('taxId')).toBe('TAX12345');
    expect(request.request.params.get('iban')).toBe('EG123');
    expect(request.request.params.get('excludeSupplierId')).toBe('supplier-1');
    request.flush({ taxIdMatches: [], bankMatches: [], duplicateFound: false });
    expect((await promise).duplicateFound).toBe(false);
  });

  it('loads supplier 360 with compliance evidence', async () => {
    const promise = store.loadSupplier360('supplier-1');
    http.expectOne('/api/v1/parties/supplier-1/supplier-360').flush(view());
    expect(await promise).toBe(true);
    expect(store.supplier360()?.compliance[0].code).toBe('TAX_ID');
  });

  it('submits lifecycle transition and refreshes both 360 and directory', async () => {
    const promise = store.transition('supplier-1', 'submit');
    http.expectOne('/api/v1/parties/supplier-1/onboarding/submit').flush({});
    await Promise.resolve();
    http.expectOne('/api/v1/parties/supplier-1/supplier-360').flush(view());
    await Promise.resolve();
    await Promise.resolve();
    http.expectOne('/api/v1/parties').flush([]);
    expect(await promise).toBe(true);
  });

  function supplierPayload(): BusinessPartyPayload {
    return {
      code: 'SUP-1', name: 'Supplier', nameEn: null, partyType: 'SUPPLIER', contactPerson: null,
      phone: null, email: null, address: null, notes: null, managedType: 'DIRECT',
      responsiblePartyId: null, relationshipStartDate: null, relationshipEndDate: null,
      currencyCode: 'EGP', invoicePolicy: 'E_INVOICE', paymentTerms: 'NET_30', taxId: 'TAX12345',
      bankAccount: null, supplierCategory: 'PACKAGING', riskLevel: 'LOW', ownerUserId: null,
      active: false, version: null,
    };
  }

  function view(): Supplier360 {
    return {
      supplier: {} as Supplier360['supplier'], documents: [], bankAccounts: [],
      compliance: [{ code: 'TAX_ID', passed: true, explanation: 'ok' }], documentCount: 0,
      expiredDocumentCount: 0, verifiedBankCount: 0, procurementAllowed: false, paymentAllowed: false,
    };
  }
});
