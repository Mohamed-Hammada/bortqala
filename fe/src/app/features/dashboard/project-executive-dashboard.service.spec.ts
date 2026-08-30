import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { ProjectExecutiveDashboardService } from './project-executive-dashboard.service';
import { ProjectExecutiveDashboardResponse } from './project-executive-dashboard.models';

describe('ProjectExecutiveDashboardService', () => {
  let service: ProjectExecutiveDashboardService;
  let httpMock: HttpTestingController;

  const mockResponse: ProjectExecutiveDashboardResponse = {
    totalProjects: 5,
    activeProjects: 4,
    totalContractValue: 100000000,
    totalBudget: 80000000,
    totalCommitted: 20000000,
    totalActualCost: 35000000,
    totalRevenue: 50000000,
    portfolioGrossProfit: 15000000,
    portfolioGrossMarginPercent: 30,
    totalReceivables: 10000000,
    totalRetentionHeld: 5000000,
    treasury: {
      totalBankBalance: 20000000,
      totalCashOnHand: 1750000,
      totalUnclearedCheques: 2000000,
      netLiquidCapital: 19750000
    },
    executionHealth: {
      averageProgressPercent: 45.5,
      delayedProjectsCount: 1,
      activeWorkforceHeadcount: 120,
      criticalTasksCount: 15
    },
    projects: [],
    currencyCode: 'EGP',
    dataAsOf: 1771495200000
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        ProjectExecutiveDashboardService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(ProjectExecutiveDashboardService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should fetch executive dashboard without filters', () => {
    service.getExecutiveDashboard().subscribe(res => {
      expect(res).toEqual(mockResponse);
      expect(res.totalProjects).toBe(5);
      expect(res.portfolioGrossMarginPercent).toBe(30);
    });

    const req = httpMock.expectOne('/api/v1/executive-dashboard/projects');
    expect(req.request.method).toBe('GET');
    req.flush(mockResponse);
  });

  it('should pass companyId and branchId query params', () => {
    service.getExecutiveDashboard('c-101', 'b-202').subscribe(res => {
      expect(res).toEqual(mockResponse);
    });

    const req = httpMock.expectOne('/api/v1/executive-dashboard/projects?companyId=c-101&branchId=b-202');
    expect(req.request.method).toBe('GET');
    req.flush(mockResponse);
  });
});
