import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { I18nService } from '../../core/i18n.service';
import { NotificationService } from '../../core/notification.service';
import { GuidedOnboardingComponent } from './guided-onboarding.component';

describe('GuidedOnboardingComponent', () => {
  let fixture: ComponentFixture<GuidedOnboardingComponent>;
  let component: GuidedOnboardingComponent;
  let http: HttpTestingController;

  const mockPacks = [
    {
      code: 'CONTRACTOR_WORKFORCE_EG',
      nameKey: 'industryPack.workforce.name',
      descriptionKey: 'industryPack.workforce.description',
      availableVersion: 1,
      installedVersion: 1,
      status: 'INSTALLED',
    },
    {
      code: 'FOOD_DISTRIBUTION_EG',
      nameKey: 'industryPack.food.name',
      descriptionKey: 'industryPack.food.description',
      availableVersion: 1,
      installedVersion: 1,
      status: 'INSTALLED',
    },
  ];

  const overview = {
    packCode: 'CONTRACTOR_WORKFORCE_EG',
    setupProgress: 50,
    dataQualityScore: 60,
    readiness: 'BLOCKED',
    assessedAt: 0,
    issues: [{ code: 'IMPORT', labelKey: 'onboarding.issue.import', route: '/imports', count: 0, blocker: true }],
    steps: [{ key: 'industryPack.step.company', sequence: 1, optional: false, status: 'COMPLETED' }],
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GuidedOnboardingComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: I18nService, useValue: { t: (k: string) => k } },
        { provide: NotificationService, useValue: { success: vi.fn() } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(GuidedOnboardingComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);

    http.expectOne('/api/v1/platform/industry-packs').flush(mockPacks);
    await Promise.resolve();
    await Promise.resolve();
    http.expectOne('/api/v1/platform/onboarding/CONTRACTOR_WORKFORCE_EG').flush(overview);
    await fixture.whenStable();
  });

  afterEach(() => {
    http.verify();
    TestBed.resetTestingModule();
  });

  it('loads installed packs and selects first installed pack', () => {
    expect(component.packs().length).toBe(2);
    expect(component.selectedPackCode()).toBe('CONTRACTOR_WORKFORCE_EG');
    expect(component.overview()?.setupProgress).toBe(50);
    expect(component.overview()?.issues[0].route).toBe('/imports');
    expect(component.overview()?.steps[0].status).toBe('COMPLETED');
  });

  it('switches between installed packs', async () => {
    const promise = component.selectPack('FOOD_DISTRIBUTION_EG');
    const req = http.expectOne('/api/v1/platform/onboarding/FOOD_DISTRIBUTION_EG');
    req.flush({
      ...overview,
      packCode: 'FOOD_DISTRIBUTION_EG',
      setupProgress: 100,
    });
    await promise;
    expect(component.selectedPackCode()).toBe('FOOD_DISTRIBUTION_EG');
    expect(component.overview()?.setupProgress).toBe(100);
  });

  it('runs an operation-id assessment on active pack', async () => {
    const promise = component.assess();
    const request = http.expectOne('/api/v1/platform/onboarding/CONTRACTOR_WORKFORCE_EG/assess');
    expect(request.request.body.operationId).toBeTruthy();
    request.flush({ ...overview, setupProgress: 100, dataQualityScore: 100, readiness: 'READY' });
    await promise;
    expect(component.overview()?.readiness).toBe('READY');
  });

  it('maps backend readiness and step states to translation keys', () => {
    expect(component.statusKey('IN_PROGRESS')).toBe('onboarding.status.in_progress');
    expect(component.statusKey('SKIPPED')).toBe('onboarding.status.skipped');
  });
});
