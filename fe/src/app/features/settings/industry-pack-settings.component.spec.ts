import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { I18nService } from '../../core/i18n.service';
import { NotificationService } from '../../core/notification.service';
import { IndustryPackSettingsComponent } from './industry-pack-settings.component';

describe('IndustryPackSettingsComponent', () => {
  let fixture: ComponentFixture<IndustryPackSettingsComponent>;
  let component: IndustryPackSettingsComponent;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [IndustryPackSettingsComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: I18nService, useValue: { t: (k: string) => k } },
        { provide: NotificationService, useValue: { success: vi.fn() } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(IndustryPackSettingsComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    http.expectOne('/api/v1/platform/industry-packs').flush([pack(), foodPack()]);
    await Promise.resolve();
    await Promise.resolve();
    http.expectOne('/api/v1/platform/industry-packs/CONTRACTOR_WORKFORCE_EG/kpis').flush([
      { key: 'contractorFillRate', labelKey: 'kpi.contractorFillRate', value: 92.5, unit: '%', status: 'HEALTHY' }
    ]);
    await fixture.whenStable();
  });

  afterEach(() => {
    http.verify();
  });

  it('loads version modules kpis and onboarding evidence', () => {
    expect(component.packs()[0].availableVersion).toBe(1);
    expect(component.packs()[0].kpis).toContain('contractorFillRate');
    expect(component.packs()[0].steps[0].status).toBe('READY');
    expect(component.packs()[0].roleReadiness?.[0].status).toBe('ASSIGNED');
    expect(component.kpisByPack()['CONTRACTOR_WORKFORCE_EG']?.[0].value).toBe(92.5);
  });

  it('renders the second vertical from backend-owned metadata', () => {
    const food = component.packs().find((item) => item.code === 'FOOD_DISTRIBUTION_EG');
    expect(food?.kpis).toContain('expiryRiskValue');
    expect(food?.defaultRoles).toContain('INVENTORY_MANAGER');
    expect(food?.importTemplates).toContain('opening-stock.xlsx');
  });

  it('installs with a retry-safe operation id then reloads', async () => {
    const p = { ...pack(), installedVersion: undefined };
    component.packs.set([p]);
    const promise = component.install(p as never);
    const request = http.expectOne('/api/v1/platform/industry-packs/CONTRACTOR_WORKFORCE_EG/install');
    expect(request.request.body.operationId).toBeTruthy();
    request.flush(pack());
    await Promise.resolve();
    await Promise.resolve();
    http.expectOne('/api/v1/platform/industry-packs').flush([pack()]);
    await Promise.resolve();
    await Promise.resolve();
    http.expectOne('/api/v1/platform/industry-packs/CONTRACTOR_WORKFORCE_EG/kpis').flush([]);
    await promise;
  });

  it('reconciles pack state on demand then reloads', async () => {
    const p = pack();
    const promise = component.reconcile(p as never);
    const request = http.expectOne('/api/v1/platform/industry-packs/CONTRACTOR_WORKFORCE_EG/reconcile');
    expect(request.request.body.operationId).toBeTruthy();
    request.flush(pack());
    await Promise.resolve();
    await Promise.resolve();
    http.expectOne('/api/v1/platform/industry-packs').flush([pack()]);
    await Promise.resolve();
    await Promise.resolve();
    http.expectOne('/api/v1/platform/industry-packs/CONTRACTOR_WORKFORCE_EG/kpis').flush([]);
    await promise;
  });

  it('completes a versioned onboarding step then reloads', async () => {
    const p = pack();
    const promise = component.complete(p as never, p.steps[0] as never);
    const request = http.expectOne('/api/v1/platform/industry-packs/CONTRACTOR_WORKFORCE_EG/steps/industryPack.step.company');
    expect(request.request.body).toEqual({ skip: false, expectedVersion: 0 });
    request.flush(p);
    await Promise.resolve();
    await Promise.resolve();
    http.expectOne('/api/v1/platform/industry-packs').flush([p]);
    await Promise.resolve();
    await Promise.resolve();
    http.expectOne('/api/v1/platform/industry-packs/CONTRACTOR_WORKFORCE_EG/kpis').flush([]);
    await promise;
  });

  function pack() {
    return {
      code: 'CONTRACTOR_WORKFORCE_EG',
      nameKey: 'industryPack.contractorWorkforce.name',
      descriptionKey: 'industryPack.contractorWorkforce.description',
      availableVersion: 1,
      installedVersion: 1,
      upgradeAvailable: false,
      status: 'INSTALLED',
      requiredFeatures: ['workforce.enabled'],
      defaultRoles: ['WORKFORCE_MANAGER'],
      kpis: ['contractorFillRate'],
      importTemplates: ['workers.xlsx'],
      settingsJson: '{}',
      customized: false,
      goLiveReady: false,
      version: 0,
      steps: [{ id: 's1', key: 'industryPack.step.company', sequence: 1, optional: false, status: 'READY', version: 0 }],
      roleReadiness: [{ code: 'WORKFORCE_MANAGER', required: true, available: true, assignedUsers: 1, status: 'ASSIGNED' }],
      templateBindings: [{ key: 'WORKERS', fileName: 'workers.xlsx', workflow: 'workforce-workers', downloadable: true, route: '/workforce/import' }],
    };
  }

  function foodPack() {
    return {
      code: 'FOOD_DISTRIBUTION_EG',
      nameKey: 'industryPack.foodDistribution.name',
      descriptionKey: 'industryPack.foodDistribution.description',
      availableVersion: 1,
      upgradeAvailable: false,
      status: 'AVAILABLE',
      requiredFeatures: ['procurement.enabled', 'inventory.advanced.enabled', 'sales.enabled', 'finance.enabled'],
      defaultRoles: ['SALES_MANAGER', 'INVENTORY_MANAGER'],
      kpis: ['expiryRiskValue', 'fillRate'],
      importTemplates: ['items.xlsx', 'customers.xlsx', 'opening-stock.xlsx'],
      customized: false,
      goLiveReady: false,
      version: 0,
      steps: [],
      roleReadiness: [],
      templateBindings: [],
    };
  }
});
