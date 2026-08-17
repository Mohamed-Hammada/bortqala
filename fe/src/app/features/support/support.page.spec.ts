import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { I18nService } from '../../core/i18n.service';
import { NotificationService } from '../../core/notification.service';
import { AuthService } from '../../core/auth/auth.service';
import { SupportPage } from './support.page';

describe('SupportPage', () => {
  let fixture: ComponentFixture<SupportPage>;
  let component: SupportPage;
  let http: HttpTestingController;

  const ticket = {
    id: 't1', ticketNo: 'SUP-1', priority: 'HIGH', category: 'BUG',
    moduleCode: 'HR', screen: '/employees', businessImpact: 'Blocked',
    description: 'Issue', status: 'NEW', assignedTeam: 'SUPPORT',
    slaDueAt: 1, createdBy: 'user', createdAt: 1, updatedAt: 1,
    resolvedAt: 0, version: 0,
  };

  const health = {
    score: 70,
    band: 'WATCH',
    dimensions: { usage: 20 },
    reasons: [{ key: 'health.usage', points: 20, status: 'POSITIVE', actionRoute: '/settings' }],
    operationId: 'h',
    calculatedAt: 1,
    replayed: false,
  };

  const feedback = {
    id: 'f1',
    type: 'FEATURE',
    moduleCode: 'PAYROLL',
    message: 'Please simplify the payroll flow',
    rating: null,
    route: '/payroll',
    status: 'NEW',
    createdBy: 'user@example.com',
    createdAt: 2,
    replayed: false,
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SupportPage],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: I18nService, useValue: { t: (key: string) => key } },
        { provide: NotificationService, useValue: { success: vi.fn() } },
        { provide: AuthService, useValue: { hasAnyRole: () => true } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SupportPage);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    await flushLoad();
    await fixture.whenStable();
  });

  afterEach(() => {
    http.verify();
    TestBed.resetTestingModule();
  });

  async function flushLoad(): Promise<void> {
    http.expectOne('/api/v1/support/tickets').flush([ticket]);
    await Promise.resolve();
    const healthReq = http.expectOne('/api/v1/support/health');
    const feedbackReq = http.expectOne('/api/v1/support/feedback');
    healthReq.flush(health);
    feedbackReq.flush([feedback]);
  }

  it('loads health and the admin feedback inbox', () => {
    expect(component.health()?.score).toBe(70);
    expect(component.feedbackItems()[0].message).toContain('payroll');
  });

  it('normalizes health labels and factor progress', () => {
    const reason = health.reasons[0];
    expect(component.maxFor(reason)).toBe(25);
    expect(component.percentage(reason)).toBe(80);
    expect(component.bandKey('AT_RISK')).toBe('support.health.band.atrisk');
  });

  it('sends privacy-limited feedback and refreshes the admin inbox', async () => {
    component.feedbackType = 'RATING';
    component.feedbackMessage = 'Useful';
    component.rating = 4;

    const promise = component.feedback();
    const request = http.expectOne('/api/v1/support/feedback');
    expect(request.request.body).toMatchObject({ type: 'RATING', message: 'Useful', rating: 4 });
    expect(request.request.body).not.toHaveProperty('formValues');

    request.flush({ id: 'f2' });
    await Promise.resolve();
    http.expectOne('/api/v1/support/feedback').flush([feedback]);
    await promise;
  });

  it('recalculates customer success evidence', async () => {
    const promise = component.calculate();
    const request = http.expectOne('/api/v1/support/health/calculate');
    expect(request.request.body.operationId).toEqual(expect.any(String));
    request.flush({ ...health, score: 80 });
    await promise;
    expect(component.health()?.score).toBe(80);
  });
});
