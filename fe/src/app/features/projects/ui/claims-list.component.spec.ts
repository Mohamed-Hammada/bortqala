import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ClaimsListComponent } from './claims-list.component';
import { ClaimService } from '../data-access/claim.service';
import { of } from 'rxjs';
import { ProjectProgressClaim } from '../models/claim.models';

describe('ClaimsListComponent', () => {
  let component: ClaimsListComponent;
  let fixture: ComponentFixture<ClaimsListComponent>;
  let claimService: ClaimService;

  const mockClaims: ProjectProgressClaim[] = [
    {
      id: 'c-1',
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
      linesCount: 3,
      createdAt: Date.now(),
      updatedAt: Date.now(),
      version: 1
    }
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ClaimsListComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        ClaimService
      ]
    }).compileComponents();

    claimService = TestBed.inject(ClaimService);
    vi.spyOn(claimService, 'loadClaims').mockReturnValue(of(mockClaims));
    claimService.claims.set(mockClaims);

    fixture = TestBed.createComponent(ClaimsListComponent);
    component = fixture.componentInstance;
    component.projectId = 'prj-1';
    fixture.detectChanges();
  });

  it('computes KPI summary cards', () => {
    expect(component.kpiTotal()).toBe(1);
    expect(component.kpiOwner()).toBe(1);
    expect(component.kpiSubcontractor()).toBe(0);
    expect(component.kpiTotalNetPayable()).toBe(95000);
  });

  it('filters claims by search query', () => {
    component.searchTerm.set('Nonexistent');
    expect(component.filteredClaims().length).toBe(0);

    component.searchTerm.set('IPC-OWN');
    expect(component.filteredClaims().length).toBe(1);
  });
});
