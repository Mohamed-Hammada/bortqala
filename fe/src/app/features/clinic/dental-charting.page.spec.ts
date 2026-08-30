import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { DentalChartingPageComponent } from './dental-charting.page';
import { ClinicService } from './clinic.service';
import { I18nService } from '../../core/i18n.service';
import { NotificationService } from '../../core/notification.service';
import { of } from 'rxjs';

describe('DentalChartingPageComponent', () => {
  let component: DentalChartingPageComponent;
  let fixture: ComponentFixture<DentalChartingPageComponent>;
  let clinicService: ClinicService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DentalChartingPageComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        ClinicService,
        I18nService,
        NotificationService,
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(DentalChartingPageComponent);
    component = fixture.componentInstance;
    clinicService = TestBed.inject(ClinicService);
  });

  it('should create and load patient and odontogram on init', () => {
    vi.spyOn(clinicService, 'searchPatients').mockReturnValue(of({
      content: [
        {
          id: 'pat-1',
          mrn: 'MRN-00001',
          fullName: 'Amr Hassan',
          phone: '01001234567',
          gender: 'MALE',
          birthDate: '1995-01-01',
          createdAt: Date.now(),
          updatedAt: Date.now(),
        },
      ],
      totalElements: 1,
      totalPages: 1,
      number: 0,
      size: 50,
    }));

    vi.spyOn(clinicService, 'getPatientOdontogram').mockReturnValue(of({
      patientId: 'pat-1',
      patientName: 'Amr Hassan',
      patientMrn: 'MRN-00001',
      teeth: [
        {
          toothNumber: 11,
          condition: 'HEALTHY',
          surface: null,
          notes: null,
          notedOn: 0,
        },
      ],
      history: [],
    }));

    vi.spyOn(clinicService, 'getDentalPlans').mockReturnValue(of([]));
    vi.spyOn(clinicService, 'getExamTemplates').mockReturnValue(of([]));

    component.ngOnInit();
    fixture.detectChanges();

    expect(component).toBeTruthy();
    expect(component.selectedPatientId()).toBe('pat-1');
    expect(component.odontogram()?.patientName).toBe('Amr Hassan');
  });

  it('should open tooth modal and set tooth form', () => {
    vi.spyOn(clinicService, 'getPatientOdontogram').mockReturnValue(of({
      patientId: 'pat-1',
      patientName: 'Amr Hassan',
      patientMrn: 'MRN-00001',
      teeth: [
        {
          toothNumber: 16,
          condition: 'CARIES',
          surface: 'OCCLUSAL',
          notes: 'Cavity detected',
          notedOn: Date.now(),
        },
      ],
      history: [],
    }));

    component.selectPatient('pat-1');
    component.openToothModal(16);

    expect(component.showToothModal()).toBe(true);
    expect(component.toothForm.toothNumber).toBe(16);
    expect(component.toothForm.condition).toBe('CARIES');
  });

  it('should load treatment plans and switch tab', () => {
    vi.spyOn(clinicService, 'getDentalPlans').mockReturnValue(of([
      {
        id: 'plan-1',
        patientId: 'pat-1',
        title: 'Full Mouth Restoration',
        status: 'ACTIVE',
        createdAt: Date.now(),
        updatedAt: Date.now(),
        items: [],
      },
    ]));

    component.selectPatient('pat-1');
    component.setTab('PLANS');
    fixture.detectChanges();

    expect(component.activeTab()).toBe('PLANS');
    expect(component.treatmentPlans().length).toBe(1);
  });
});
