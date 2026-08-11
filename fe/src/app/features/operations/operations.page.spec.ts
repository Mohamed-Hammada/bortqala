import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { OperationsPage } from './operations.page';
import { OperationsStore } from './operations.store';
import { I18nService } from '../../core/i18n.service';
import { NotificationService } from '../../core/notification.service';
import { OperationsSnapshot } from './operations.models';

describe('OperationsPage REM-005 document references', () => {
  let page: OperationsPage;
  let transaction: ReturnType<typeof vi.fn>;
  let error: ReturnType<typeof vi.fn>;

  const empty: OperationsSnapshot = {
    items: [], movements: [], partyBalances: [], ledgerEntries: [], employeeAdvances: [],
  };

  beforeEach(async () => {
    transaction = vi.fn(async () => true);
    error = vi.fn();
    const mockStore = {
      loading: signal(false),
      error: signal<string | null>(null),
      snapshot: signal(empty),
      parties: signal([]),
      employees: signal([]),
      categories: signal([]),
      uoms: signal([]),
      negativeBalances: signal([]),
      load: vi.fn(async () => {}),
      transaction,
      export: vi.fn(async () => {}),
    };
    await TestBed.configureTestingModule({
      imports: [OperationsPage],
      providers: [
        { provide: I18nService, useValue: { t: (key: string) => key, locale: () => 'ar-EG' } },
        { provide: NotificationService, useValue: { success: vi.fn(), error } },
      ],
    }).compileComponents();
    TestBed.overrideComponent(OperationsPage, {
      set: { providers: [{ provide: OperationsStore, useValue: mockStore }] },
    });
    const fixture = TestBed.createComponent(OperationsPage);
    page = fixture.componentInstance;
  });

  afterEach(() => {
    TestBed.resetTestingModule();
  });

  it('sends the separate business-document references and attachment metadata on save', async () => {
    page.transactionForm.patchValue({
      itemId: 'item-1',
      operationType: 'SUPPLY_RECEIPT',
      quantityDelta: 10,
      referenceCode: 'REF-1',
      documentType: 'GOODS_RECEIPT',
      purchaseOrderNo: 'PO-77',
      receiptNo: 'GRN-77',
      deliveryNoteNo: '',
      invoiceNo: 'INV-77',
      voucherNo: '',
      externalRef: 'EXT-1',
      warehouse: 'Main',
      attachmentFile: new File(['x'], 'photo.jpg', { type: 'image/jpeg' }),
    });

    await page.saveTransaction();

    expect(transaction).toHaveBeenCalledTimes(1);
    const payload = transaction.mock.calls[0][0];
    expect(payload.purchaseOrderNo).toBe('PO-77');
    expect(payload.receiptNo).toBe('GRN-77');
    expect(payload.invoiceNo).toBe('INV-77');
    expect(payload.externalRef).toBe('EXT-1');
    expect(payload.warehouse).toBe('Main');
    expect(payload.attachmentName).toBe('photo.jpg');
    expect(payload.attachmentContentType).toBe('image/jpeg');
    expect(payload.attachmentSize).toBeGreaterThan(0);
    expect(payload).not.toHaveProperty('attachmentFile');
    expect(payload.deliveryNoteNo).toBeNull();
    expect(payload.voucherNo).toBeNull();
  });

  it('blocks a goods-receipt movement that misses its required receipt number', async () => {
    page.transactionForm.patchValue({
      itemId: 'item-1',
      operationType: 'SUPPLY_RECEIPT',
      quantityDelta: 5,
      receiptNo: '',
      purchaseOrderNo: 'PO-1',
    });

    await page.saveTransaction();

    expect(transaction).not.toHaveBeenCalled();
    expect(error).toHaveBeenCalledWith('OPS_MOVEMENT_RECEIPT_REQUIRED');
  });

  it('computes required references per operation type', () => {
    page.transactionForm.controls.operationType.setValue('SUPPLY_RECEIPT');
    expect(page.requiredReferences()).toEqual(['purchaseOrderNo', 'receiptNo']);
    page.transactionForm.controls.operationType.setValue('PROCESSING_DELIVERY');
    expect(page.requiredReferences()).toEqual(['deliveryNoteNo']);
    page.transactionForm.controls.operationType.setValue('ADJUSTMENT');
    expect(page.requiredReferences()).toEqual(['voucherNo']);
    page.transactionForm.controls.operationType.setValue('PAYMENT');
    expect(page.requiredReferences()).toEqual([]);
  });

  it('labels the delivery-note document type from i18n', () => {
    expect(page.documentTypeLabel('DELIVERY_NOTE')).toBe('operations.documentType.deliveryNote');
    expect(page.documentTypeLabel(null)).toBe('—');
  });

  it('prefers a business document number over the internal reference code', () => {
    expect(page.primaryReference({
      receiptNo: 'GRN-9', invoiceNo: null, deliveryNoteNo: null, purchaseOrderNo: null,
      voucherNo: null, externalRef: null, referenceCode: 'MOV-AB12CDEF34',
    } as never)).toBe('GRN-9');
    expect(page.primaryReference({
      receiptNo: null, invoiceNo: null, deliveryNoteNo: null, purchaseOrderNo: null,
      voucherNo: null, externalRef: null, referenceCode: 'MOV-AB12CDEF34',
    } as never)).toBe('MOV-AB12CDEF34');
  });

  it('rejects an oversized attachment and keeps the control empty', () => {
    const tooBig = new File(['x'], 'big.pdf', { type: 'application/pdf' });
    Object.defineProperty(tooBig, 'size', { value: 6 * 1024 * 1024 });
    page.onAttachmentSelected({ target: { files: [tooBig], value: '' } } as unknown as Event);
    expect(error).toHaveBeenCalledWith('operations.attachmentSizeError');
    expect(page.transactionForm.controls.attachmentFile.value).toBeNull();
  });

  it('rejects an unsupported attachment type', () => {
    const bad = new File(['x'], 'malware.exe', { type: 'application/x-msdownload' });
    page.onAttachmentSelected({ target: { files: [bad], value: '' } } as unknown as Event);
    expect(error).toHaveBeenCalledWith('operations.attachmentTypeError');
    expect(page.transactionForm.controls.attachmentFile.value).toBeNull();
  });
});
