import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { OperationsStore } from './operations.store';
import { I18nService } from '../../core/i18n.service';
import { OperationsSnapshot } from './operations.models';

describe('OperationsStore', () => {
  let store: OperationsStore;
  let httpMock: HttpTestingController;

  const snapshot: OperationsSnapshot = {
    items: [], movements: [], partyBalances: [], ledgerEntries: [], employeeAdvances: [],
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        OperationsStore,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: I18nService, useValue: { t: (key: string) => key, locale: () => 'ar-EG' } },
      ],
    });
    store = TestBed.inject(OperationsStore);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('posts the source document type and reason on transactions', async () => {
    const promise = store.transaction({
      itemId: 'item-1',
      partyId: null,
      quantityDelta: 10,
      documentType: 'GOODS_RECEIPT',
      reason: 'استلام بضاعة بموجب إذن استلام رقم GRN-42',
      referenceCode: 'GRN-42',
      occurredAt: 1785600000000,
    });
    const request = httpMock.expectOne('/api/v1/operations/transactions');
    expect(request.request.body.documentType).toBe('GOODS_RECEIPT');
    expect(request.request.body.reason).toContain('GRN-42');
    request.flush(snapshot);
    await promise;
    expect(store.loading()).toBe(false);
  });

  it('keeps documentType null when no source document is selected', async () => {
    const promise = store.transaction({
      itemId: 'item-1',
      partyId: null,
      quantityDelta: -5,
      documentType: null,
      reason: 'جرد مستودع',
      referenceCode: 'ADJ-1',
      occurredAt: 1785600000000,
    });
    const request = httpMock.expectOne('/api/v1/operations/transactions');
    expect(request.request.body.documentType).toBeNull();
    request.flush(snapshot);
    await promise;
    expect(store.loading()).toBe(false);
  });

  it('updates valuation settings and refreshes the valuation report', async () => {
    store.valuation.set({
      policy: { id: null, valuationMethod: 'WEIGHTED_AVERAGE', inventoryAccountId: null,
        receiptOffsetAccountId: null, cogsAccountId: null, adjustmentAccountId: null,
        glPostingEnabled: false, allowBackdatedPosting: false, version: 0, createdAt: null, updatedAt: null },
      totalInventoryValue: 0, items: [], movementCosts: [],
    });
    const promise = store.saveValuationPolicy({ valuationMethod: 'FIFO', version: 0 });
    httpMock.expectOne('/api/v1/operations/valuation/settings').flush({
      ...store.valuation()!.policy, valuationMethod: 'FIFO', version: 1,
    });
    await Promise.resolve();
    httpMock.expectOne('/api/v1/operations').flush(snapshot);
    httpMock.expectOne('/api/v1/parties').flush([]);
    httpMock.expectOne('/api/v1/employees').flush([]);
    httpMock.expectOne('/api/v1/operations/item-categories').flush([]);
    httpMock.expectOne('/api/v1/operations/uoms').flush([]);
    httpMock.expectOne('/api/v1/operations/negative-balances').flush([]);
    httpMock.expectOne('/api/v1/operations/valuation/report').flush({
      ...store.valuation()!, policy: { ...store.valuation()!.policy, valuationMethod: 'FIFO', version: 1 },
    });
    httpMock.expectOne('/api/v1/finance/accounts').flush([]);
    httpMock.expectOne('/api/v1/operations/reorder-alerts').flush([]);
    httpMock.expectOne('/api/v1/operations/cycle-counts').flush([]);
    httpMock.expectOne('/api/v1/inventory/warehouses').flush([]);

    expect(await promise).toBe(true);
    expect(store.valuation()?.policy.valuationMethod).toBe('FIFO');
  });
});
