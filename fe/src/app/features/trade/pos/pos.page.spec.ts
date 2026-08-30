import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { I18nService } from '../../../core/i18n.service';
import { NotificationService } from '../../../core/notification.service';
import { PosPage } from './pos.page';

describe('PosPage', () => {
  let fixture: ComponentFixture<PosPage>;
  let component: PosPage;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PosPage],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: I18nService, useValue: { t: (key: string) => key } },
        { provide: NotificationService, useValue: { success: vi.fn(), error: vi.fn() } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PosPage);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
    flushLoad();
    await fixture.whenStable();
  });

  afterEach(() => {
    try {
      http.verify();
    } finally {
      TestBed.resetTestingModule();
    }
  });

  it('initializes and loads summary, terminals, sessions, and transactions', () => {
    expect(component.summary()?.todaySales).toBe(1250);
    expect(component.terminals()).toHaveLength(1);
    expect(component.sessions()).toHaveLength(1);
    expect(component.transactions()).toHaveLength(1);
    expect(component.activeSession()?.sessionNumber).toBe('POS-SES-2026-001');
  });

  it('adds item to cart and computes subtotal, 14% VAT, and total', () => {
    component.addItemToCart({
      itemId: 'item-101',
      itemCode: 'ITM-COF-01',
      itemName: 'Espresso Roast 1KG',
      unitPrice: 100,
    });

    expect(component.cartLines()).toHaveLength(1);
    expect(component.cartSubtotal()).toBe(100);
    expect(component.cartTax()).toBe(14);
    expect(component.cartTotal()).toBe(114);
  });

  it('completes a cash sale and opens receipt modal', async () => {
    component.addItemToCart({
      itemId: 'item-101',
      itemCode: 'ITM-COF-01',
      itemName: 'Espresso Roast 1KG',
      unitPrice: 100,
    });

    component.cashTendered.set(150);
    expect(component.changeDue()).toBe(36);

    component.completeSale();

    const req = http.expectOne('/api/v1/trade/pos/transactions/sale');
    expect(req.request.method).toBe('POST');
    expect(req.request.body.paymentMethod).toBe('CASH');
    req.flush({
      id: 'txn-2',
      transactionNumber: 'POS-TXN-2026-0002',
      sessionId: 'ses-1',
      terminalId: 'term-1',
      subtotal: 100,
      taxAmount: 14,
      totalAmount: 114,
      cashTendered: 150,
      changeAmount: 36,
      status: 'COMPLETED',
      createdAt: Date.now(),
      lines: component.cartLines(),
    });

    await Promise.resolve();
    http.expectOne('/api/v1/trade/pos/summary').flush({
      todaySales: 1364,
      todayTransactionsCount: 11,
      activeShiftsCount: 1,
      totalVariance: 0,
    });
    http.expectOne('/api/v1/trade/pos/transactions').flush([]);

    expect(component.showReceiptModal()).toBe(true);
    expect(component.activeReceipt()?.transactionNumber).toBe('POS-TXN-2026-0002');
  });

  it('opens and closes shift', async () => {
    component.openCloseShiftModal();
    expect(component.showCloseShiftModal()).toBe(true);

    component.closeShiftForm.setValue({
      closingActualCash: 800,
      closingActualCard: 200,
      notes: 'Shift closed',
    });

    component.submitCloseShift();
    const req = http.expectOne('/api/v1/trade/pos/sessions/ses-1/close');
    expect(req.request.method).toBe('POST');
    req.flush({
      id: 'ses-1',
      sessionNumber: 'POS-SES-2026-001',
      status: 'CLOSED',
      closingActualCash: 800,
      closingActualCard: 200,
    });

    await Promise.resolve();
    http.expectOne('/api/v1/trade/pos/summary').flush({
      todaySales: 1250,
      todayTransactionsCount: 10,
      activeShiftsCount: 0,
      totalVariance: 0,
    });
    http.expectOne('/api/v1/trade/pos/sessions').flush([]);

    expect(component.activeSession()).toBeNull();
    expect(component.showCloseShiftModal()).toBe(false);
  });

  it('creates a new terminal', async () => {
    component.openTerminalModal();
    expect(component.showTerminalModal()).toBe(true);

    component.terminalForm.patchValue({
      terminalCode: 'POS-02',
      terminalName: 'Drive Thru Register',
      status: 'ACTIVE',
    });

    component.saveTerminal();
    const req = http.expectOne('/api/v1/trade/pos/terminals');
    expect(req.request.method).toBe('POST');
    expect(req.request.body.terminalCode).toBe('POS-02');
    req.flush({
      id: 'term-2',
      terminalCode: 'POS-02',
      terminalName: 'Drive Thru Register',
      status: 'ACTIVE',
    });

    await Promise.resolve();
    http.expectOne('/api/v1/trade/pos/terminals').flush([]);
    expect(component.showTerminalModal()).toBe(false);
  });

  function flushLoad() {
    http.expectOne('/api/v1/trade/pos/summary').flush({
      todaySales: 1250,
      todayTransactionsCount: 10,
      activeShiftsCount: 1,
      totalVariance: 0,
    });
    http.expectOne('/api/v1/trade/pos/terminals').flush([
      {
        id: 'term-1',
        terminalCode: 'POS-01',
        terminalName: 'Main Counter',
        status: 'ACTIVE',
        createdAt: Date.now(),
        updatedAt: Date.now(),
      },
    ]);
    http.expectOne('/api/v1/trade/pos/sessions/active?terminalId=term-1').flush({
      id: 'ses-1',
      sessionNumber: 'POS-SES-2026-001',
      terminalId: 'term-1',
      cashierUserId: 'cashier-1',
      openedAt: Date.now(),
      closedAt: null,
      openingFloat: 500,
      closingActualCash: null,
      closingCalculatedCash: 728,
      closingActualCard: null,
      closingCalculatedCard: 0,
      cashVariance: null,
      cardVariance: null,
      status: 'OPEN',
      notes: null,
      createdAt: Date.now(),
      updatedAt: Date.now(),
    });
    http.expectOne('/api/v1/trade/pos/sessions').flush([
      {
        id: 'ses-1',
        sessionNumber: 'POS-SES-2026-001',
        terminalId: 'term-1',
        cashierUserId: 'cashier-1',
        openedAt: Date.now(),
        closedAt: null,
        openingFloat: 500,
        closingActualCash: null,
        closingCalculatedCash: 728,
        closingActualCard: null,
        closingCalculatedCard: 0,
        cashVariance: null,
        cardVariance: null,
        status: 'OPEN',
        notes: null,
        createdAt: Date.now(),
        updatedAt: Date.now(),
      },
    ]);
    http.expectOne('/api/v1/trade/pos/transactions').flush([
      {
        id: 'txn-1',
        transactionNumber: 'POS-TXN-2026-0001',
        sessionId: 'ses-1',
        terminalId: 'term-1',
        cashierUserId: 'cashier-1',
        customerId: null,
        transactionType: 'SALE',
        paymentMethod: 'CASH',
        subtotal: 200,
        discountAmount: 0,
        taxAmount: 28,
        totalAmount: 228,
        cashTendered: 250,
        changeAmount: 22,
        status: 'COMPLETED',
        originalTransactionId: null,
        clientOfflineId: null,
        createdAt: Date.now(),
        lines: [],
      },
    ]);
  }
});
