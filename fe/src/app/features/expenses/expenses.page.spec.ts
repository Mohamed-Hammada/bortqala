import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { ExpensesPage } from './expenses.page';

describe('ExpensesPage', () => {
  let component: ExpensesPage;
  let fixture: ComponentFixture<ExpensesPage>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ExpensesPage],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(ExpensesPage);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
  });

  function flushAll() {
    const pending = httpMock.match(() => true);
    pending.forEach((r) => r.flush([]));
  }

  it('should create', async () => {
    fixture.detectChanges();
    flushAll();
    await fixture.whenStable();
    expect(component).toBeTruthy();
  });

  it('should load claims on init', async () => {
    fixture.detectChanges();
    const mineReq = httpMock.expectOne('/api/v1/expenses');
    mineReq.flush([
      { id: '1', status: 'DRAFT', amount: 50, category: 'MEAL', spentOn: '2026-08-15', currency: 'EGP' },
    ]);
    flushAll();
    await fixture.whenStable();
    expect(component.claims().length).toBe(1);
  });

  it('should open create drawer', async () => {
    fixture.detectChanges();
    flushAll();
    await fixture.whenStable();
    component.openCreate();
    expect(component.drawerOpen()).toBe(true);
    expect(component.editingId()).toBeNull();
  });

  it('should open edit drawer with claim data', async () => {
    fixture.detectChanges();
    flushAll();
    await fixture.whenStable();
    component.openEdit({ id: 'c1', category: 'TRANSPORT', spentOn: '2026-08-15', amount: 100, currency: 'EGP' } as any);
    expect(component.editingId()).toBe('c1');
    expect(component.drawerOpen()).toBe(true);
  });

  it('should have expense categories', async () => {
    fixture.detectChanges();
    flushAll();
    await fixture.whenStable();
    expect(component.categories).toEqual(['MEAL', 'TRANSPORT', 'LODGING', 'SUPPLIES', 'OTHER']);
  });

  it('should render above-limit badge and open forced-note approve modal', async () => {
    fixture.detectChanges();
    const mineReq = httpMock.expectOne('/api/v1/expenses');
    mineReq.flush([
      { id: '1', status: 'SUBMITTED', amount: 250, category: 'MEAL', spentOn: '2026-08-15', currency: 'EGP', limitExceeded: true } as any,
    ]);
    flushAll();
    await fixture.whenStable();
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.badge-limit');
    expect(badge).toBeTruthy();

    component.openApproveReject({ id: '1', status: 'SUBMITTED', limitExceeded: true } as any, 'approve');
    expect(component.approveOpen()).toBe(true);
    expect(component.approveTarget()?.id).toBe('1');
  });

  it('should approve over-limit claim only with a note', async () => {
    fixture.detectChanges();
    flushAll();
    await fixture.whenStable();
    component.approveTarget.set({ id: '1', status: 'SUBMITTED', limitExceeded: true } as any);
    component.approveOpen.set(true);
    component.approveNote.setValue('Approved for the business trip');
    component.approveFromModal();
    const req = httpMock.expectOne('/api/v1/expenses/1/approve');
    expect(req.request.body).toEqual({ note: 'Approved for the business trip' });
    req.flush({ id: '1', status: 'APPROVED', limitExceeded: true } as any);
    flushAll();
    await fixture.whenStable();
    expect(component.approveOpen()).toBe(false);
  });
});
