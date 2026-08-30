import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { PharmacyPageComponent } from './pharmacy.page';
import { ClinicService } from './clinic.service';
import { I18nService } from '../../core/i18n.service';
import { NotificationService } from '../../core/notification.service';
import { of } from 'rxjs';

describe('PharmacyPageComponent', () => {
  let component: PharmacyPageComponent;
  let fixture: ComponentFixture<PharmacyPageComponent>;
  let clinicService: ClinicService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PharmacyPageComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        ClinicService,
        I18nService,
        NotificationService,
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PharmacyPageComponent);
    component = fixture.componentInstance;
    clinicService = TestBed.inject(ClinicService);
  });

  it('should create and load initial drug catalog', () => {
    vi.spyOn(clinicService, 'getAllPharmacyItems').mockReturnValue(of([
      {
        id: 'drug-1',
        itemId: 'item-1',
        tradeName: 'Panadol Extra 500mg',
        genericName: 'Paracetamol + Caffeine',
        dosageForm: 'TABLET',
        strengthText: '500mg',
        controlled: false,
      }
    ]));

    component.ngOnInit();
    fixture.detectChanges();

    expect(component).toBeTruthy();
    expect(component.pharmacyItems().length).toBe(1);
  });

  it('should switch to narcotics tab and load narcotics register', () => {
    vi.spyOn(clinicService, 'getNarcoticsRegister').mockReturnValue(of([
      {
        id: 'narc-1',
        dispenseRecordId: 'disp-1',
        pharmacyItemId: 'drug-2',
        tradeName: 'Morphine 10mg',
        patientMrn: 'MRN-00001',
        patientName: 'Ahmed Ali',
        prescriberDoctorName: 'Dr. Tarek',
        dispenserUserName: 'Pharmacist 1',
        secondSignerName: 'Pharmacist 2',
        batchNumber: 'BATCH-M01',
        quantity: 2,
        reason: 'Post-op pain',
        signedAt: 1788700000000,
      }
    ]));

    component.setTab('NARCOTICS');
    fixture.detectChanges();

    expect(component.activeTab()).toBe('NARCOTICS');
    expect(component.narcoticsEntries().length).toBe(1);
  });
});
