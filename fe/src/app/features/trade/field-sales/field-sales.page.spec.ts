import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { I18nService } from '../../../core/i18n.service';
import { FieldSalesPage } from './field-sales.page';
import { FieldSalesOfflineService } from './field-sales-offline.service';

describe('FieldSalesPage', () => {
  let fixture: ComponentFixture<FieldSalesPage>;
  let component: FieldSalesPage;
  let http: HttpTestingController;

  const mockBundle = {
    customers: [
      {
        id: 'cust-1',
        code: 'CUST-001',
        name: 'Al-Ahram Trading',
        creditLimit: 50000,
        currentBalance: 1200,
        creditHold: false,
        paymentTermsDays: 30,
      },
    ],
    products: [
      {
        id: 'prod-1',
        itemCode: 'ITEM-001',
        itemName: 'Premium Juice 1L',
        unitOfMeasure: 'PCS',
        basePrice: 25,
        taxRate: 14,
        availableStock: 100,
      },
    ],
    warehouses: [
      {
        id: 'wh-1',
        warehouseCode: 'WH-VAN-01',
        warehouseName: 'Van #1',
      },
    ],
    salesRepUserId: 'sales_rep_1',
    salesRepName: 'Ahmed Rep',
    serverTimestamp: Date.now(),
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FieldSalesPage],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: I18nService, useValue: { t: (key: string) => key } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(FieldSalesPage);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();

    // Flush initial bundle and history requests
    const bundleReq = http.expectOne('/api/v1/trade/field-sales/offline-bundle');
    bundleReq.flush(mockBundle);

    const historyReq = http.expectOne('/api/v1/trade/field-sales/history');
    historyReq.flush([]);

    await fixture.whenStable();
  });

  afterEach(() => {
    try {
      http.verify();
    } finally {
      TestBed.resetTestingModule();
    }
  });

  it('initializes with loaded bundle customers, products, and default invoice type', () => {
    expect(component.customers()).toHaveLength(1);
    expect(component.customers()[0].code).toBe('CUST-001');
    expect(component.products()).toHaveLength(1);
    expect(component.products()[0].itemCode).toBe('ITEM-001');
    expect(component.warehouses()).toHaveLength(1);
    expect(component.docType()).toBe('INVOICE');
    expect(component.activeTab()).toBe('new_sale');
  });

  it('selects customer and calculates financial balance correctly', () => {
    component.selectCustomer(component.customers()[0]);
    expect(component.selectedCustomerId()).toBe('cust-1');
    expect(component.selectedCustomer()?.name).toBe('Al-Ahram Trading');
  });

  it('adds items to cart and computes subtotal, 14% tax, and total', () => {
    const product = component.products()[0];
    component.addItemToCart(product);

    expect(component.cartLines()).toHaveLength(1);
    expect(component.cartLines()[0].quantity).toBe(1);
    expect(component.cartLines()[0].unitPrice).toBe(25);
    expect(component.subtotal()).toBe(25);
    expect(component.taxAmount()).toBe(3.5);
    expect(component.totalAmount()).toBe(28.5);

    // Add same product again (increments quantity)
    component.addItemToCart(product);
    expect(component.cartLines()[0].quantity).toBe(2);
    expect(component.subtotal()).toBe(50);
    expect(component.taxAmount()).toBe(7);
    expect(component.totalAmount()).toBe(57);
  });

  it('updates quantity and removes line item', () => {
    const product = component.products()[0];
    component.addItemToCart(product);
    component.updateLineQty(0, 2); // 1 + 2 = 3
    expect(component.cartLines()[0].quantity).toBe(3);

    component.updateLineQty(0, -3); // 3 - 3 = 0, removes line
    expect(component.cartLines()).toHaveLength(0);
  });

  it('switches document type to RECEIPT and zeroes tax amount', () => {
    const product = component.products()[0];
    component.addItemToCart(product);
    component.setDocType('RECEIPT');

    expect(component.docType()).toBe('RECEIPT');
    expect(component.cartLines()).toHaveLength(0);
    expect(component.taxAmount()).toBe(0);
  });

  it('captures GPS coordinates fallback', () => {
    component.captureLocation();
    expect(component.gpsLocation()).toBe('30.044420,31.235712');
  });

  it('saves and queues offline transaction into outbox', async () => {
    component.selectedCustomerId.set('cust-1');
    component.addItemToCart(component.products()[0]);
    component.signeeName.set('Mohamed Ahmed');

    await component.saveAndQueueOffline();

    expect(component.cartLines()).toHaveLength(0);
    expect(component.signeeName()).toBe('');
  });

  it('switches tabs between new_sale, outbox, customers, and history', () => {
    component.setTab('customers');
    expect(component.activeTab()).toBe('customers');

    component.setTab('outbox');
    expect(component.activeTab()).toBe('outbox');

    component.setTab('history');
    expect(component.activeTab()).toBe('history');

    // Trigger history refresh
    const historyReq = http.expectOne('/api/v1/trade/field-sales/history');
    historyReq.flush([]);
  });
});
