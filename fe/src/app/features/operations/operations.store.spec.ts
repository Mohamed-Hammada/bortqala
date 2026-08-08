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
});
