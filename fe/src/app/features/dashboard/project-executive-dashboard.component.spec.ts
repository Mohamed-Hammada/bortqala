import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ProjectExecutiveDashboardComponent } from './project-executive-dashboard.component';
import { ProjectExecutiveDashboardService } from './project-executive-dashboard.service';
import { ProjectExecutiveDashboardResponse } from './project-executive-dashboard.models';

describe('ProjectExecutiveDashboardComponent', () => {
  let component: ProjectExecutiveDashboardComponent;
  let fixture: ComponentFixture<ProjectExecutiveDashboardComponent>;
  let service: ProjectExecutiveDashboardService;

  const mockData: ProjectExecutiveDashboardResponse = {
    totalProjects: 3,
    activeProjects: 2,
    totalContractValue: 60000000,
    totalBudget: 45000000,
    totalCommitted: 15000000,
    totalActualCost: 20000000,
    totalRevenue: 30000000,
    portfolioGrossProfit: 10000000,
    portfolioGrossMarginPercent: 33.33,
    totalReceivables: 5000000,
    totalRetentionHeld: 3000000,
    treasury: {
      totalBankBalance: 12000000,
      totalCashOnHand: 1000000,
      totalUnclearedCheques: 1500000,
      netLiquidCapital: 11500000
    },
    executionHealth: {
      averageProgressPercent: 50,
      delayedProjectsCount: 0,
      activeWorkforceHeadcount: 85,
      criticalTasksCount: 12
    },
    projects: [
      {
        projectId: 'p-1',
        projectName: 'Nile Tower',
        status: 'ACTIVE',
        contractValue: 50000000,
        budgetAmount: 40000000,
        committedAmount: 10000000,
        actualCost: 15000000,
        recognizedRevenue: 25000000,
        grossProfit: 10000000,
        grossMarginPercent: 40,
        progressPercent: 60,
        delayed: false
      }
    ],
    currencyCode: 'EGP',
    dataAsOf: 1771495200000
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProjectExecutiveDashboardComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        ProjectExecutiveDashboardService
      ]
    }).compileComponents();

    service = TestBed.inject(ProjectExecutiveDashboardService);
    vi.spyOn(service, 'getExecutiveDashboard').mockReturnValue(of(mockData));

    fixture = TestBed.createComponent(ProjectExecutiveDashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and load executive dashboard metrics', () => {
    expect(component).toBeTruthy();
    expect(component.data()).toEqual(mockData);
    expect(component.filteredProjects().length).toBe(1);
  });

  it('filters project list by search term', () => {
    component.searchTerm.set('Nile');
    expect(component.filteredProjects().length).toBe(1);

    component.searchTerm.set('Unknown');
    expect(component.filteredProjects().length).toBe(0);
  });
});
