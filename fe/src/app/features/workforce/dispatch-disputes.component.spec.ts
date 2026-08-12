import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { AuthService } from '../../core/auth/auth.service';
import { I18nService } from '../../core/i18n.service';
import { NotificationService } from '../../core/notification.service';
import { DispatchDisputesComponent } from './pages/dispatch-disputes/dispatch-disputes.component';

describe('DispatchDisputesComponent', () => {
  let fixture: ComponentFixture<DispatchDisputesComponent>;
  let component: DispatchDisputesComponent;
  let httpTestingController: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DispatchDisputesComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { hasAnyRole: () => true } },
        { provide: I18nService, useValue: { t: (key: string) => key } },
        { provide: NotificationService, useValue: { success: vi.fn(), error: vi.fn() } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(DispatchDisputesComponent);
    component = fixture.componentInstance;
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
    [
      '/api/v1/workforce/dispatches',
      '/api/v1/workforce/labor-requests',
      '/api/v1/workforce/contractors',
      '/api/v1/workforce/workers',
      '/api/v1/workforce/settlements/periods',
    ].forEach(url => httpTestingController.expectOne(url).flush([]));
    fixture.detectChanges();
  });

  afterEach(() => httpTestingController.verify());

  it('creates a dispatch using the typed API contract', () => {
    component.dispatchForm = { requestId: 'req-1', contractorId: 'contractor-1', dispatchDate: '2026-08-12' };
    component.createDispatch();

    const request = httpTestingController.expectOne('/api/v1/workforce/dispatches');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(component.dispatchForm);
    request.flush({ id: 'dispatch-1', ...component.dispatchForm, status: 'DRAFT', createdAt: 1, updatedAt: 1, version: 0 });
    httpTestingController.expectOne('/api/v1/workforce/dispatches').flush([]);
  });

  it('loads and resolves disputes without sending an actor identity', () => {
    component.selectPeriod('period-1');
    httpTestingController.expectOne('/api/v1/workforce/settlements/period-1/disputes').flush([]);

    vi.spyOn(window, 'prompt').mockReturnValue('Reviewed and corrected');
    component.transitionDispute({
      id: 'dispute-1', settlementPeriodId: 'period-1', contractorId: 'contractor-1', disputedAmount: 50,
      reason: 'Mismatch', status: 'UNDER_REVIEW', createdAt: 1, updatedAt: 1, version: 0,
    }, 'resolve');

    const request = httpTestingController.expectOne('/api/v1/workforce/disputes/dispute-1/resolve');
    expect(request.request.body).toEqual({ resolutionNotes: 'Reviewed and corrected' });
    expect(request.request.body.resolvedBy).toBeUndefined();
    request.flush({ id: 'dispute-1', settlementPeriodId: 'period-1', contractorId: 'contractor-1', disputedAmount: 50,
      reason: 'Mismatch', status: 'RESOLVED', resolutionNotes: 'Reviewed and corrected', resolvedBy: 'server-user', createdAt: 1, updatedAt: 2, version: 1 });
  });
});
