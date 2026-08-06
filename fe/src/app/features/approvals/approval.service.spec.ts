import { TestBed } from '@angular/core/testing';
import { ApprovalService } from './data-access/approval.service';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

describe('ApprovalService', () => {
  let service: ApprovalService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(ApprovalService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should load workflow definitions', () => {
    service.loadWorkflowDefinitions().subscribe(defs => {
      expect(defs.length).toBe(1);
      expect(defs[0].documentType).toBe('PURCHASE_ORDER');
    });

    const req = httpMock.expectOne('/api/v1/approval-workflows');
    expect(req.request.method).toBe('GET');
    req.flush([{ id: 'def-1', documentType: 'PURCHASE_ORDER', name: 'PO Workflow', active: true, version: 1, steps: [], createdAt: 0, updatedAt: 0 }]);
  });

  it('should approve step and refresh tasks', () => {
    service.approveStep('inst-1', 'Approved OK').subscribe(res => {
      expect(res.status).toBe('APPROVED');
    });

    const approveReq = httpMock.expectOne('/api/v1/approvals/approve');
    expect(approveReq.request.method).toBe('POST');
    expect(approveReq.request.body).toEqual({ instanceId: 'inst-1', comment: 'Approved OK' });
    approveReq.flush({ instanceId: 'inst-1', documentType: 'PURCHASE_ORDER', documentId: 'PO-100', currentStepOrder: 1, status: 'APPROVED', submittedBy: 'user1', submittedAt: 0, history: [] });

    const tasksReq = httpMock.expectOne('/api/v1/approvals/my-tasks');
    expect(tasksReq.request.method).toBe('GET');
    tasksReq.flush([]);
  });

  it('should reject step with mandatory comment', () => {
    service.rejectStep('inst-1', 'Over budget limit').subscribe(res => {
      expect(res.status).toBe('REJECTED');
    });

    const rejectReq = httpMock.expectOne('/api/v1/approvals/reject');
    expect(rejectReq.request.method).toBe('POST');
    expect(rejectReq.request.body).toEqual({ instanceId: 'inst-1', comment: 'Over budget limit' });
    rejectReq.flush({ instanceId: 'inst-1', documentType: 'PURCHASE_ORDER', documentId: 'PO-100', currentStepOrder: 1, status: 'REJECTED', submittedBy: 'user1', submittedAt: 0, history: [] });

    const tasksReq = httpMock.expectOne('/api/v1/approvals/my-tasks');
    expect(tasksReq.request.method).toBe('GET');
    tasksReq.flush([]);
  });
});
