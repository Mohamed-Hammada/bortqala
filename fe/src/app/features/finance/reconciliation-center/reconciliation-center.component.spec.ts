import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ReconciliationCenterComponent } from './reconciliation-center.component';
import { I18nService } from '../../../core/i18n.service';

describe('ReconciliationCenterComponent', () => {
  let component: ReconciliationCenterComponent;
  let fixture: ComponentFixture<ReconciliationCenterComponent>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReconciliationCenterComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: I18nService,
          useValue: {
            t: (key: string) => key,
            locale: () => 'ar-EG',
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ReconciliationCenterComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('initializes and loads reconciliation domain overview', async () => {
    fixture.detectChanges();
    const req = httpMock.expectOne('/api/v1/finance/reconciliation-center/overview');
    expect(req.request.method).toBe('GET');
    req.flush([
      {
        domainKey: 'inventory',
        subledgerType: 'INVENTORY',
        subledgerBalance: 2450000,
        glBalance: 2450000,
        varianceAmount: 0,
        isBalanced: true,
        discrepancyCount: 0,
        status: 'BALANCED',
      },
      {
        domainKey: 'bank',
        subledgerType: 'BANK',
        subledgerBalance: 4700000,
        glBalance: 4825000,
        varianceAmount: 125000,
        isBalanced: false,
        discrepancyCount: 2,
        status: 'VARIANCE',
      },
    ]);

    await fixture.whenStable();

    expect(component.summaries().length).toBe(2);
    expect(component.summaries()[0].isBalanced).toBe(true);
    expect(component.summaries()[1].isBalanced).toBe(false);
  });

  it('fetches discrepancy drilldown when opened', async () => {
    fixture.detectChanges();
    const initReq = httpMock.expectOne('/api/v1/finance/reconciliation-center/overview');
    initReq.flush([]);
    await fixture.whenStable();

    const domain = {
      domainKey: 'bank',
      subledgerType: 'BANK',
      subledgerBalance: 4700000,
      glBalance: 4825000,
      varianceAmount: 125000,
      isBalanced: false,
      discrepancyCount: 1,
      status: 'VARIANCE',
    };

    const drilldownPromise = component.openDrilldown(domain);

    const drillReq = httpMock.expectOne('/api/v1/finance/reconciliation-center/drilldown?subledgerType=BANK');
    expect(drillReq.request.method).toBe('GET');
    drillReq.flush([
      {
        documentId: 'chk-1',
        documentNumber: 'CHK-9921',
        subledgerAmount: 125000,
        glAmount: 0,
        variance: 125000,
        discrepancyReason: 'UNPOSTED_SUBLEDGER_DOCUMENT',
      },
    ]);

    await drilldownPromise;
    await fixture.whenStable();

    expect(component.discrepancies().length).toBe(1);
    expect(component.discrepancies()[0].documentNumber).toBe('CHK-9921');
  });
});
