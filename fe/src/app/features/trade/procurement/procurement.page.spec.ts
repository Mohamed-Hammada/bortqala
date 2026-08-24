import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
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
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();
  });

  function createPage() {
    const fixture = TestBed.createComponent(ProcurementPage);
    return fixture.componentInstance;
  }

  it('keeps procurement save states independent', () => {
    const page = createPage();

    expect(page.submitting()).toBe(false);

    page.savingPo.set(true);
    expect(page.submitting()).toBe(true);
    expect(page.savingGrn()).toBe(false);
    expect(page.savingInvoice()).toBe(false);
    expect(page.savingPayment()).toBe(false);

    page.savingPo.set(false);
    page.savingInvoice.set(true);
    expect(page.submitting()).toBe(true);
    expect(page.savingPo()).toBe(false);
    expect(page.savingGrn()).toBe(false);
    expect(page.savingPayment()).toBe(false);

    page.savingInvoice.set(false);
    expect(page.submitting()).toBe(false);
  });

  it('includes three-way-match resolution in the aggregate busy state', () => {
    const page = createPage();
    page.resolvingMatch.set(true);
    expect(page.submitting()).toBe(true);
    page.resolvingMatch.set(false);
    expect(page.submitting()).toBe(false);
  });

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
    page.savingInvoice.set(false);
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
    page.savingInvoice.set(false);
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

  it('builds a multi-invoice proposal only for one supplier and currency', () => {
    const page = createPage();
    const base = {
      invoiceNumber: 'INV-1', internalReference: 'REF-1', currencyCode: 'EGP', baseCurrencyCode: 'EGP',
      exchangeRate: 1, exchangeRateDate: 0, exchangeRateSource: 'BASE', baseNetAmount: 100,
      supplierId: 'supplier-a', invoiceDate: 0, totalAmount: 100, netAmount: 100,
      paidAmount: 0, outstandingAmount: 100, status: 'UNPAID', createdAt: 0, updatedAt: 0,
    };
    const first = { ...base, id: 'invoice-a', outstandingAmount: 60 };
    const second = { ...base, id: 'invoice-b', invoiceNumber: 'INV-2', outstandingAmount: 40 };
    const otherSupplier = { ...base, id: 'invoice-c', supplierId: 'supplier-b' };
    page.invoices.set([first, second, otherSupplier]);
    vi.spyOn(page.notification, 'warning');

    page.toggleProposalInvoice(first, true);
    page.toggleProposalInvoice(second, true);

    expect(page.proposalDraftAllocations()).toEqual([
      { invoiceId: 'invoice-a', amount: 60 }, { invoiceId: 'invoice-b', amount: 40 },
    ]);
    expect(page.proposalDraftTotal()).toBe(100);

    page.toggleProposalInvoice(otherSupplier, true);
    expect(page.proposalDraftAllocations()).toHaveLength(2);
    expect(page.notification.warning).toHaveBeenCalledWith('procurement.proposalSameSupplierCurrency');
  });

  it('updates a selected proposal allocation without frontend financial recalculation', () => {
    const page = createPage();
    page.proposalDraftAllocations.set([{ invoiceId: 'invoice-a', amount: 60 }]);
    page.updateProposalAllocation('invoice-a', '25.50');
    expect(page.proposalDraftAllocations()).toEqual([{ invoiceId: 'invoice-a', amount: 25.5 }]);
    expect(page.proposalDraftTotal()).toBe(25.5);
  });

  it('resolves project name when PO is linked to a project', () => {
    const page = createPage();
    page.projects.set([{ id: 'proj-1', code: 'PRJ-100', name: 'Al-Noor Tower' }]);

    const linkedPo: any = { id: 'po-1', projectId: 'proj-1' };
    const unlinkedPo: any = { id: 'po-2', projectId: null };

    expect(page.poProjectName(linkedPo)).toBe('PRJ-100 - Al-Noor Tower');
    expect(page.poProjectName(unlinkedPo)).toBe('—');
  });

  it('maps scorecard ratings to semantic status badge classes', () => {
    const page = createPage();
    expect(page.scorecardRatingClass('EXCELLENT')).toBe('success');
    expect(page.scorecardRatingClass('GOOD')).toBe('info');
    expect(page.scorecardRatingClass('FAIR')).toBe('warning');
    expect(page.scorecardRatingClass('AT_RISK')).toBe('danger');
  });

  it('rejects installment plans outside the 2-60 range via the form validator', () => {
    const page = createPage();
    page.planForm.controls.installmentCount.setValue(1);
    expect(page.planForm.controls.installmentCount.invalid).toBe(true);
    page.planForm.controls.installmentCount.setValue(61);
    expect(page.planForm.controls.installmentCount.invalid).toBe(true);
    page.planForm.controls.installmentCount.setValue(6);
    expect(page.planForm.controls.installmentCount.valid).toBe(true);
    page.planForm.controls.firstDueDate.setValue('');
    expect(page.planForm.invalid).toBe(true);
  });

  it('blocks plan submission when the form is invalid', async () => {
    const page = createPage();
    vi.spyOn(page.notification, 'error');
    page.planInvoice.set({ id: 'inv-1' } as any);
    page.planForm.controls.firstDueDate.setValue('');
    await page.submitPaymentPlan();
    expect(page.savingPlan()).toBe(false);
  });

  it('blocks purchase-request submission when a line misses an item or quantity', async () => {
    const page = createPage();
    vi.spyOn(page.notification, 'error');
    const http = TestBed.inject(HttpClient);
    const post = vi.spyOn(http, 'post');
    page.openNewPr();
    page.prForm.controls.requestedBy.setValue('merl');
    await page.submitPr();
    expect(page.savingPr()).toBe(false);
    expect(post).not.toHaveBeenCalled();

    page.prLines.set([{ itemId: '', itemName: '', quantity: 0, unitOfMeasure: '', estimatedUnitPrice: 0 }]);
    await page.submitPr();
    expect(post).not.toHaveBeenCalled();
  });

  it('submits a valid purchase request with epoch neededBy and reloads the list', async () => {
    const page = createPage();
    const http = TestBed.inject(HttpClient);
    const post = vi.spyOn(http, 'post').mockReturnValue(of({}));
    vi.spyOn(page, 'loadPurchaseRequests').mockResolvedValue(undefined);
    page.openNewPr();
    page.prForm.controls.requestedBy.setValue('merl');
    page.prLines.set([{ itemId: 'item-1', itemName: 'Steel', quantity: 4, unitOfMeasure: 'TON', estimatedUnitPrice: 120 }]);
    await page.submitPr();
    expect(post).toHaveBeenCalledWith('/api/v1/purchase-requests', expect.objectContaining({
      requestedBy: 'merl',
      lines: [expect.objectContaining({ itemId: 'item-1', quantity: 4 })],
    }));
    expect(typeof post.mock.calls[0][1].neededBy).toBe('number');
    expect(page.prModalOpen()).toBe(false);
  });

  it('sends convert calls to the request-scoped convert endpoint with the chosen supplier', async () => {
    const page = createPage();
    const http = TestBed.inject(HttpClient);
    const post = vi.spyOn(http, 'post').mockReturnValue(of({}));
    vi.spyOn(page, 'loadPurchaseRequests').mockResolvedValue(undefined);
    vi.spyOn(page, 'loadOrders').mockResolvedValue(undefined);
    page.convertingPr.set({
      id: 'pr-9', requestNumber: 'PRQ-2026-00001', requestedBy: 'merl', status: 'APPROVED',
      lines: [], createdAt: 0, updatedAt: 0,
    } as any);
    page.convertForm.controls.supplierId.setValue('sup-1');
    await page.confirmConvert();
    expect(post).toHaveBeenCalledWith('/api/v1/purchase-requests/pr-9/convert', { supplierId: 'sup-1' });
    expect(page.convertingPr()).toBeNull();
  });
});

