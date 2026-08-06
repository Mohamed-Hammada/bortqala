import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { vi } from 'vitest';
import { ProcurementPage, calculatePurchaseOrderTotal, filterPayableInvoices } from './procurement.page';

describe('purchase order totals', () => {
  it('recalculates the document total from edited line values', () => {
    const lines = [{ quantity: 10, unitPrice: 1000 }];
    expect(calculatePurchaseOrderTotal(lines)).toBe(10_000);
    lines[0].quantity = 12;
    expect(calculatePurchaseOrderTotal(lines)).toBe(12_000);
  });

  it('treats invalid numeric input as zero', () => {
    expect(calculatePurchaseOrderTotal([{ quantity: Number.NaN, unitPrice: 1000 }])).toBe(0);
  });
});

describe('filterPayableInvoices', () => {
  it('returns only open invoices belonging to the selected supplier', () => {
    const invoices = [
      { id: 'a-open', supplierId: 'supplier-a', status: 'UNPAID' },
      { id: 'a-partial', supplierId: 'supplier-a', status: 'PARTIALLY_PAID' },
      { id: 'a-paid', supplierId: 'supplier-a', status: 'PAID' },
      { id: 'b-open', supplierId: 'supplier-b', status: 'UNPAID' },
    ];

    expect(filterPayableInvoices(invoices, 'supplier-a').map(invoice => invoice.id))
      .toEqual(['a-open', 'a-partial']);
  });
});

describe('ProcurementPage invoice adjustments', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProcurementPage],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
  });

  function createPage() {
    const fixture = TestBed.createComponent(ProcurementPage);
    return fixture.componentInstance;
  }

  it('rejects a negative discount amount via the form validator', () => {
    const page = createPage();
    page.invForm.controls.discountAmount.setValue(-100);
    expect(page.invForm.controls.discountAmount.invalid).toBe(true);
    expect(page.invForm.controls.discountAmount.errors?.['min']).toBeTruthy();
  });

  it('rejects a negative tax amount via the form validator', () => {
    const page = createPage();
    page.invForm.controls.taxAmount.setValue(-50);
    expect(page.invForm.controls.taxAmount.invalid).toBe(true);
  });

  it('matches the backend net amount formula total - discount + tax', () => {
    const page = createPage();
    page.invForm.patchValue({ totalAmount: 1000, discountAmount: 200, taxAmount: 50 });
    expect(page.invNetAmount()).toBe(850);
  });

  it('shows a negative net preview matching the backend when discount exceeds total', () => {
    const page = createPage();
    page.invForm.patchValue({ totalAmount: 500, discountAmount: 700, taxAmount: 0 });
    expect(page.invNetAmount()).toBe(-200);
  });

  it('blocks submit when the discount exceeds the total', async () => {
    const page = createPage();
    page.invForm.patchValue({
      hasSupplierInvoice: true,
      invoiceNumber: 'INV-1',
      supplierId: 's1',
      invoiceDate: '2026-08-06',
      totalAmount: 500,
      discountAmount: 700,
      taxAmount: 0,
      exchangeRateOverrideReason: 'manual',
    });
    const notification = page.notification;
    vi.spyOn(notification, 'warning');
    page.submitting.set(false);
    await page.submitInvoice();
    expect(notification.warning).toHaveBeenCalledWith('procurement.invoiceDiscountExceedsTotal');
  });

  it('blocks submit when the due date precedes the invoice date', async () => {
    const page = createPage();
    page.invForm.patchValue({
      hasSupplierInvoice: true,
      invoiceNumber: 'INV-2',
      supplierId: 's1',
      invoiceDate: '2026-08-10',
      dueDate: '2026-08-01',
      totalAmount: 500,
      discountAmount: 0,
      taxAmount: 0,
      exchangeRateOverrideReason: 'manual',
    });
    const notification = page.notification;
    vi.spyOn(notification, 'warning');
    page.submitting.set(false);
    await page.submitInvoice();
    expect(notification.warning).toHaveBeenCalledWith('procurement.invoiceDueDateBeforeInvoiceDate');
  });

  it('requires the PO number when automatic numbering is disabled', () => {
    const page = createPage();
    page.automaticNumbering.set(false);
    page.applyNumberingValidators();
    expect(page.poForm.controls.poNumber.hasError('required')).toBe(true);
    page.poForm.controls.poNumber.setValue('PO-123');
    expect(page.poForm.controls.poNumber.valid).toBe(true);
    page.automaticNumbering.set(true);
    page.applyNumberingValidators();
    expect(page.poForm.controls.poNumber.valid).toBe(true);
  });

  it('requires the GRN number when automatic numbering is disabled', () => {
    const page = createPage();
    page.automaticNumbering.set(false);
    page.applyNumberingValidators();
    expect(page.grnForm.controls.grnNumber.hasError('required')).toBe(true);
    page.automaticNumbering.set(true);
    page.applyNumberingValidators();
    expect(page.grnForm.controls.grnNumber.valid).toBe(true);
  });
});
