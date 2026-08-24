import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { AdvancesPolicySettingsComponent } from './advances-policy-settings.component';
import { I18nService } from '../../../core/i18n.service';
import { NotificationService } from '../../../core/notification.service';
import { WorkforceService } from '../../workforce/data-access/workforce.service';

describe('AdvancesPolicySettingsComponent', () => {
  let fixture: ComponentFixture<AdvancesPolicySettingsComponent>;
  let component: AdvancesPolicySettingsComponent;
  let http: HttpTestingController;
  let saveSpy: ReturnType<typeof vi.fn>;

  const globalAuto = {
    scopeType: 'GLOBAL',
    deductionMode: 'AUTO',
    deductionFrequency: 'MONTHLY',
    maxDeductionPercent: 50,
    defaultInstallments: 1,
    deferralPeriods: 0,
    version: 3,
    effectiveFrom: '2026-01-01',
    active: true,
  };

  beforeEach(async () => {
    saveSpy = vi.fn(() => of({ ...globalAuto }));
    await TestBed.configureTestingModule({
      imports: [AdvancesPolicySettingsComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: I18nService, useValue: { t: (key: string) => key } },
        { provide: NotificationService, useValue: { success: vi.fn(), error: vi.fn(), info: vi.fn() } },
        {
          provide: WorkforceService,
          useValue: { loadAdvancePolicies: () => of([globalAuto]), saveAdvancePolicy: saveSpy },
        },
      ],
    }).compileComponents();

    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(AdvancesPolicySettingsComponent);
    component = fixture.componentInstance;
  });

  function flushCategories(): void {
    const req = http.expectOne('/api/v1/categories');
    req.flush([
      { id: 'cat-1', name: 'Staff', active: true },
      { id: 'cat-2', name: 'Guards', active: true },
      { id: 'cat-3', name: 'Inactive', active: false },
    ]);
  }

  it('hydrates the global card from the latest policy version and defaults to AUTO', async () => {
    fixture.detectChanges();
    flushCategories();
    await Promise.resolve();
    expect(component.loading()).toBe(false);
    expect(component.globalMode()).toBe('AUTO');
    expect(component.globalCadence()).toBe('MONTHLY');
    expect(component.dirty()).toBe(false);
    expect(component.categories().length).toBe(2);
  });

  it('marks the form dirty on change and saves a new GLOBAL version via Save All', async () => {
    fixture.detectChanges();
    flushCategories();
    await Promise.resolve();

    component.setGlobalMode('MANUAL');
    expect(component.dirty()).toBe(true);

    const promise = component.saveAll();
    await Promise.resolve();
    await Promise.resolve();

    expect(saveSpy).toHaveBeenCalledTimes(1);
    const payload = saveSpy.mock.calls[0][0];
    expect(payload.scopeType).toBe('GLOBAL');
    expect(payload.deductionMode).toBe('MANUAL');
    expect(payload.version).toBe(4);
    await promise;
  });

  it('cancel reverts draft changes without issuing requests', async () => {
    fixture.detectChanges();
    flushCategories();
    await Promise.resolve();

    component.setGlobalMode('MANUAL');
    component.setGlobalCadence('MID_MONTH_SPLIT');
    component.cancel();

    expect(component.globalMode()).toBe('AUTO');
    expect(component.globalCadence()).toBe('MONTHLY');
    expect(component.dirty()).toBe(false);
    expect(saveSpy).not.toHaveBeenCalled();
  });

  it('adds a category exception, persists it as EMPLOYEE_CATEGORY scope, then reloads', async () => {
    fixture.detectChanges();
    flushCategories();
    await Promise.resolve();

    component.addException();
    expect(component.exceptions().length).toBe(1);
    const draft = component.exceptions()[0];
    component.updateException(draft.categoryId, { mode: 'MANUAL' });
    expect(component.dirty()).toBe(true);
    expect(component.availableCategories().length).toBe(1);

    const promise = component.saveAll();
    await Promise.resolve();
    await Promise.resolve();

    expect(saveSpy).toHaveBeenCalledTimes(1);
    const payload = saveSpy.mock.calls[0][0];
    expect(payload.scopeType).toBe('EMPLOYEE_CATEGORY');
    expect(payload.deductionMode).toBe('MANUAL');
    expect(payload.version).toBe(1);
    await promise;
  });

  it('hydrates existing EMPLOYEE_CATEGORY exceptions with their persisted versions', async () => {
    const workforce = TestBed.inject(WorkforceService);
    (workforce as unknown as { loadAdvancePolicies: () => unknown }).loadAdvancePolicies = () =>
      of([
        globalAuto,
        {
          scopeType: 'EMPLOYEE_CATEGORY',
          scopeId: 'cat-1',
          scopeName: 'Staff',
          deductionMode: 'MANUAL',
          deductionFrequency: 'HALF_MONTH',
          maxDeductionPercent: 50,
          defaultInstallments: 1,
          deferralPeriods: 0,
          version: 5,
          effectiveFrom: '2026-01-01',
          active: true,
        },
      ]);

    fixture = TestBed.createComponent(AdvancesPolicySettingsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    flushCategories();
    await Promise.resolve();

    expect(component.exceptions().length).toBe(1);
    expect(component.exceptions()[0].persistedVersion).toBe(5);
    expect(component.exceptions()[0].mode).toBe('MANUAL');
    expect(component.exceptions()[0].cadence).toBe('MONTHLY');
  });
});
