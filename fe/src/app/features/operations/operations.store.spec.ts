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
    httpMock.expectOne('/api/v1/operations/transfers').flush([]);
    httpMock.expectOne('/api/v1/operations/analytics/aging').flush(null);
    httpMock.expectOne('/api/v1/operations/analytics/dead-stock').flush([]);
    httpMock.expectOne('/api/v1/operations/analytics/reorder-alerts').flush([]);

    expect(await promise).toBe(true);
    expect(store.valuation()?.policy.valuationMethod).toBe('FIFO');
  });

  it('creates and transitions a warehouse transfer while updating local state', async () => {
    const transfer = { id: 'tr-1', transferNumber: 'TR-001', sourceWarehouseId: 'wh-1', sourceWarehouseName: 'Main',
      targetWarehouseId: 'wh-2', targetWarehouseName: 'Secondary', transferDate: 1785628800000,
      status: 'DRAFT' as const, version: 0, lines: [] };
    const create = store.createTransfer({ transferNumber: 'TR-001', sourceWarehouseId: 'wh-1', targetWarehouseId: 'wh-2', transferDate: '2026-08-02' });
    httpMock.expectOne('/api/v1/operations/transfers').flush(transfer);
    expect((await create)?.id).toBe('tr-1');

    const ship = store.transitionTransfer('tr-1', 'ship');
    httpMock.expectOne('/api/v1/operations/transfers/tr-1/ship').flush({ ...transfer, status: 'SHIPPED', version: 1 });
    expect(await ship).toBe(true);
    expect(store.transfers()[0].status).toBe('SHIPPED');
  });

  it('creates a warehouse bin and updates the typed local collection', async () => {
    const promise = store.createBin('wh-1', { binCode: 'A-01', aisle: 'A', rack: '1', shelf: '2' });
    httpMock.expectOne('/api/v1/operations/warehouses/wh-1/bins').flush({
      id: 'bin-1', warehouseId: 'wh-1', binCode: 'A-01', aisle: 'A', rack: '1', shelf: '2', active: true, version: 0,
    });
    expect(await promise).toBe(true);
    expect(store.bins()[0].binCode).toBe('A-01');
  });

  it('looks up item by barcode', async () => {
    const promise = store.lookupBarcode('123456');
    httpMock.expectOne('/api/v1/operations/analytics/barcode-lookup?barcode=123456').flush({
      itemId: 'item-1', itemCode: 'ITM-01', itemName: 'Cement', matchedBarcode: '123456', matchType: 'PRIMARY',
    });
    const result = await promise;
    expect(result?.itemCode).toBe('ITM-01');
  });
});
