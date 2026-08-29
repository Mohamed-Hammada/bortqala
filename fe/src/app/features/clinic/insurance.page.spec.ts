import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { InsurancePageComponent } from './insurance.page';
import { ClinicService } from './clinic.service';
import { I18nService } from '../../core/i18n.service';
import { NotificationService } from '../../core/notification.service';
import { of } from 'rxjs';

describe('InsurancePageComponent', () => {
  let component: InsurancePageComponent;
  let fixture: ComponentFixture<InsurancePageComponent>;
  let clinicService: ClinicService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [InsurancePageComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        ClinicService,
        I18nService,
        NotificationService,
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(InsurancePageComponent);
    component = fixture.componentInstance;
    clinicService = TestBed.inject(ClinicService);
  });

  it('should create and load initial payers and plans', () => {
    vi.spyOn(clinicService, 'getAllPayers').mockReturnValue(of([
      {
        id: 'payer-1',
        name: 'AXA Egypt',
        type: 'PRIVATE',
        active: true,
      }
    ]));

    vi.spyOn(clinicService, 'getPlansByPayer').mockReturnValue(of([
      {
        id: 'plan-1',
        payerId: 'payer-1',
        name: 'Gold Comprehensive',
        coveragePercent: 80,
        copayFlat: 50,
        active: true,
      }
    ]));

    component.ngOnInit();
    fixture.detectChanges();

    expect(component).toBeTruthy();
    expect(component.payers().length).toBe(1);
    expect(component.plans().length).toBe(1);
  });

  it('should switch to pre-auth tab and load pre-authorizations', () => {
    vi.spyOn(clinicService, 'getPreAuthorizations').mockReturnValue(of([
      {
        id: 'auth-1',
        payerId: 'payer-1',
        payerName: 'AXA Egypt',
        patientId: 'pat-1',
        patientMrn: 'MRN-00001',
        patientName: 'Ahmed Ali',
        procedureText: 'MRI Brain',
        approvalCode: 'AUTH-12345',
        requestedAmount: 3500,
        approvedAmount: 3000,
        status: 'APPROVED',
      }
    ]));

    component.setTab('PRE_AUTH');
    fixture.detectChanges();

    expect(component.activeTab()).toBe('PRE_AUTH');
    expect(component.preAuthorizations().length).toBe(1);
  });

  it('should switch to claims tab and load claim batches', () => {
    vi.spyOn(clinicService, 'getAllClaimBatches').mockReturnValue(of([
      {
        id: 'batch-1',
        batchNumber: 'BATCH-2026-08-1001',
        payerId: 'payer-1',
        payerName: 'AXA Egypt',
        period: '2026-08',
        status: 'SUBMITTED',
        totalClaimedAmount: 24500,
        totalApprovedAmount: 0,
        totalRejectedAmount: 0,
        lines: [],
      }
    ]));

    component.setTab('CLAIMS');
    fixture.detectChanges();

    expect(component.activeTab()).toBe('CLAIMS');
    expect(component.claimBatches().length).toBe(1);
  });
});
