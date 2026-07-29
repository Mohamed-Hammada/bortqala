import { calculatePurchaseOrderTotal, filterPayableInvoices } from './procurement.page';

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
