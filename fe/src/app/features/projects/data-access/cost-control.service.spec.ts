import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { CostControlService } from './cost-control.service';
import { CostControlSummary } from '../models/cost-control.models';

describe('CostControlService', () => {
  let service: CostControlService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        CostControlService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });

    service = TestBed.inject(CostControlService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('fetches project cost control summary', () => {
    const mockSummary: CostControlSummary = {
      projectId: 'p-1',
      projectName: 'Test Hospital',
      contractValue: 5000000,
      currencyCode: 'EGP',
      totalBudget: 4000000,
      totalCommitted: 1000000,
      totalActualCost: 1500000,
      totalRecognizedRevenue: 2000000,
      currentGrossProfit: 500000,
      currentGrossMarginPercent: 25,
      forecastEac: 3800000,
      forecastVac: 200000,
      forecastProfit: 1200000,
      forecastMarginPercent: 24,
      categoryBreakdowns: []
    };

    service.getSummary('p-1').subscribe(res => {
      expect(res).toEqual(mockSummary);
      expect(res.currentGrossProfit).toBe(500000);
    });

    const req = httpMock.expectOne('/api/v1/projects/p-1/cost-control/summary');
    expect(req.request.method).toBe('GET');
    req.flush(mockSummary);
  });

  it('approves a budget version', () => {
    service.approveBudgetVersion('p-1', 'ver-1').subscribe(res => {
      expect(res.status).toBe('APPROVED');
    });

    const req = httpMock.expectOne('/api/v1/projects/p-1/cost-control/budget-versions/ver-1/approve');
    expect(req.request.method).toBe('POST');
    req.flush({ id: 'ver-1', status: 'APPROVED' });
  });
});
