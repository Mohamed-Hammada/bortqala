import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpErrorResponse, HttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { ContractorsComponent } from './contractors.component';
import { I18nService } from '../../../../core/i18n.service';
import { NotificationService } from '../../../../core/notification.service';

describe('ContractorsComponent', () => {
  let httpMock: HttpTestingController;
  let component: ContractorsComponent;
  const notify = { success: vi.fn(), error: vi.fn(), warning: vi.fn() };

  const contractor = {
    id: 'ctr-1', code: 'CTR-101', name: 'مقاول النقل', tradeName: 'Trans Co.',
    phone: '01001234567', accountingModel: 'worker_net_total', paymentRouting: 'contractor_full',
    settlementCycleDays: 15, defaultDailyRate: 0, status: 'ACTIVE',
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ContractorsComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: I18nService, useValue: { t: (key: string) => key } },
        { provide: NotificationService, useValue: notify },
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(ContractorsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    httpMock.expectOne((req) => req.url === '/api/v1/workforce/contractors').flush([contractor]);
  });

  afterEach(() => {
    TestBed.resetTestingModule();
    httpMock.verify();
  });

  it('loads contractors into the workforce service signal', () => {
    expect(component.workforceService.contractors()).toHaveLength(1);
    expect(component.workforceService.contractors()[0].id).toBe('ctr-1');
  });

  it('blocks save locally when required fields are missing without calling the API', async () => {
    component.openCreateModal();
    component.form.name = '';
    await component.saveContractor();
    expect(component.saveError()).toBe('workforce.ui.contractors.requiredError');
    expect(() => httpMock.verify()).not.toThrow();
  });

  it('classifies an offline network failure into a localized connection error', async () => {
    component.openCreateModal();
    component.form = {
      code: 'CTR-999', name: 'New Contractor', phone: '01009999999',
      accountingModel: 'worker_net_total', paymentRouting: 'contractor_full',
      settlementCycleDays: 15, defaultDailyRate: 0, status: 'ACTIVE',
    };
    component.saveContractor();
    const post = httpMock.expectOne('/api/v1/workforce/contractors');
    expect(post.request.method).toBe('POST');
    post.error(new ProgressEvent('error'));
    await Promise.resolve();
    expect(component.saveError()).toBe('api.connectionError');
    expect(component.workforceService.contractors()).toHaveLength(1);
  });

  it('verifies the saved contractor is visible in the reloaded list before closing', async () => {
    component.openCreateModal();
    component.form = {
      code: 'CTR-102', name: 'New Contractor', phone: '01009999998',
      accountingModel: 'worker_net_total', paymentRouting: 'contractor_full',
      settlementCycleDays: 15, defaultDailyRate: 0, status: 'ACTIVE',
    };
    const savePromise = component.saveContractor();
    const post = httpMock.expectOne('/api/v1/workforce/contractors');
    post.flush({ ...contractor, id: 'ctr-2', code: 'CTR-102' });
    await Promise.resolve();
    const reload = httpMock.expectOne('/api/v1/workforce/contractors');
    reload.flush([contractor, { ...contractor, id: 'ctr-2', code: 'CTR-102' }]);
    await savePromise;
    expect(component.isModalOpen).toBe(false);
    expect(notify.success).toHaveBeenCalledWith('workforce.ui.contractors.createdSuccess');
  });
});