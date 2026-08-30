import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { AiIntelligencePageComponent } from './ai-intelligence.page';
import { AiIntelligenceService } from './ai-intelligence.service';
import { I18nService } from '../../core/i18n.service';

describe('AiIntelligencePageComponent', () => {
  let component: AiIntelligencePageComponent;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AiIntelligencePageComponent],
      providers: [
        AiIntelligenceService,
        I18nService,
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(AiIntelligencePageComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should initialize and load cash flow forecast by default', async () => {
    expect(component.activeTab()).toBe('cashflow');

    const loadPromise = component.loadData();

    const req = httpMock.expectOne('/api/v1/analytics/cashflow-forecast?months=3');
    expect(req.request.method).toBe('GET');
    req.flush({
      forecastMonths: 3,
      points: [
        {
          year: 2026,
          month: 9,
          periodLabel: '2026-09',
          projectedInflow: 45000,
          projectedOutflow: 32000,
          projectedNet: 13000,
          lowerBand: 11000,
          upperBand: 15000,
          historical: false,
        },
      ],
      totalProjectedNet: 13000,
      confidenceNote: 'Linear projection',
    });

    await loadPromise;
    expect(component.ai.forecast()?.points.length).toBe(1);
    expect(component.ai.forecast()?.totalProjectedNet).toBe(13000);
  });

  it('should switch to anomalies tab and load expense anomalies', async () => {
    const tabPromise = component.setTab('anomalies');
    expect(component.activeTab()).toBe('anomalies');

    const req = httpMock.expectOne('/api/v1/analytics/expense-anomalies');
    expect(req.request.method).toBe('GET');
    req.flush([
      {
        vendorId: 'SUP-1',
        vendorName: 'Acme Corp',
        expenseCategory: 'PROCUREMENT',
        currentAmount: 10000,
        sixMonthMean: 1000,
        standardDeviation: 10,
        zScore: 3.5,
        flaggedReason: 'Invoice amount exceeds 6-month historical baseline',
        transactionTimestamp: 1000,
      },
    ]);

    await tabPromise;
    expect(component.ai.anomalies().length).toBe(1);
    expect(component.ai.anomalies()[0].zScore).toBe(3.5);
  });

  it('should ask natural language query and store result', async () => {
    component.nlQuestion.set('ما هو إجمالي المبيعات؟');
    const queryPromise = component.askQuestion();

    const req = httpMock.expectOne('/api/v1/analytics/nl-query');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ question: 'ما هو إجمالي المبيعات؟' });
    req.flush({
      question: 'ما هو إجمالي المبيعات؟',
      targetDataset: 'SALES_REVENUE',
      interpretedIntent: 'TOTAL_SALES_SUMMARY',
      appliedFilters: ['status=CONFIRMED'],
      records: [{ value: 285400 }],
      totalMatchingRows: 1,
      summaryAnswer: 'إجمالي المبيعات 285,400.00 ج.م',
      success: true,
    });

    await queryPromise;
    expect(component.ai.nlQueryResult()?.success).toBe(true);
    expect(component.ai.nlQueryResult()?.targetDataset).toBe('SALES_REVENUE');
  });
});
