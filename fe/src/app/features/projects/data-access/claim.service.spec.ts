import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ClaimService } from './claim.service';
import { CreateProgressClaimRequest, ProjectProgressClaim } from '../models/claim.models';

describe('ClaimService', () => {
  let service: ClaimService;
  let httpMock: HttpTestingController;

  const mockClaim: ProjectProgressClaim = {
    id: 'claim-1',
    claimNumber: 'IPC-OWN-2026-001',
    claimType: 'OWNER_IPC',
    claimKind: 'INTERIM',
    claimSequenceNumber: 1,
    projectId: 'prj-1',
    periodStartDate: '2026-05-01',
    periodEndDate: '2026-05-31',
    currencyCode: 'EGP',
    previousGrossAmount: 0,
    currentGrossAmount: 100000,
    cumulativeGrossAmount: 100000,
    previousRetentionAmount: 0,
    currentRetentionAmount: 5000,
    cumulativeRetentionAmount: 5000,
    previousAdvanceRecoveryAmount: 0,
    currentAdvanceRecoveryAmount: 0,
    cumulativeAdvanceRecoveryAmount: 0,
    currentTaxAmount: 0,
    currentDeductionsAmount: 0,
    currentNetPayableAmount: 95000,
    cumulativeNetPaidAmount: 0,
    status: 'DRAFT',
    linesCount: 2,
    createdAt: Date.now(),
    updatedAt: Date.now(),
    version: 1
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        ClaimService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });

    service = TestBed.inject(ClaimService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('loads claims for a project', () => {
    service.loadClaims('prj-1').subscribe((res) => {
      expect(res.length).toBe(1);
      expect(res[0].claimNumber).toBe('IPC-OWN-2026-001');
    });

    const req = httpMock.expectOne('/api/v1/projects/prj-1/claims');
    expect(req.request.method).toBe('GET');
    req.flush([mockClaim]);

    expect(service.claims().length).toBe(1);
  });

  it('creates an interim claim and updates signal state', () => {
    const payload: CreateProgressClaimRequest = {
      claimType: 'OWNER_IPC',
      claimKind: 'INTERIM',
      projectId: 'prj-1',
      periodStartDate: '2026-06-01',
      periodEndDate: '2026-06-30',
      initFromWbs: true
    };

    service.createClaim('prj-1', payload).subscribe((res) => {
      expect(res.id).toBe('claim-1');
    });

    const req = httpMock.expectOne('/api/v1/projects/prj-1/claims');
    expect(req.request.method).toBe('POST');
    req.flush(mockClaim);

    expect(service.claims().length).toBe(1);
    expect(service.currentClaim()?.id).toBe('claim-1');
  });
});
