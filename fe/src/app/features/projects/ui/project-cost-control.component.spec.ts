import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { of } from 'rxjs';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ProjectCostControlComponent } from './project-cost-control.component';
import { CostControlService } from '../data-access/cost-control.service';
import { CostControlSummary } from '../models/cost-control.models';

describe('ProjectCostControlComponent', () => {
  let component: ProjectCostControlComponent;
  let fixture: ComponentFixture<ProjectCostControlComponent>;
  let costControlService: CostControlService;

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

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProjectCostControlComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        CostControlService
      ]
    }).compileComponents();

    costControlService = TestBed.inject(CostControlService);
    vi.spyOn(costControlService, 'getSummary').mockReturnValue(of(mockSummary));
    vi.spyOn(costControlService, 'listBudgetVersions').mockReturnValue(of([]));
    vi.spyOn(costControlService, 'listCostLedgerEntries').mockReturnValue(of([]));
    vi.spyOn(costControlService, 'listForecastEac').mockReturnValue(of([]));

    fixture = TestBed.createComponent(ProjectCostControlComponent);
    component = fixture.componentInstance;
    component.projectId = 'p-1';
    fixture.detectChanges();
  });

  it('should create and load cost control summary', () => {
    expect(component).toBeTruthy();
    expect(component.summary()).toEqual(mockSummary);
  });

  it('switches tabs smoothly', () => {
    component.activeTab.set('forecastEac');
    expect(component.activeTab()).toBe('forecastEac');

    component.activeTab.set('costLedger');
    expect(component.activeTab()).toBe('costLedger');
  });
});
