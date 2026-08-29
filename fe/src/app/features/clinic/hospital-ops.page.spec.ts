import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { HospitalOpsPageComponent } from './hospital-ops.page';
import { ClinicService } from './clinic.service';
import { I18nService } from '../../core/i18n.service';
import { NotificationService } from '../../core/notification.service';
import { of } from 'rxjs';

describe('HospitalOpsPageComponent', () => {
  let component: HospitalOpsPageComponent;
  let fixture: ComponentFixture<HospitalOpsPageComponent>;
  let clinicService: ClinicService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HospitalOpsPageComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        ClinicService,
        I18nService,
        NotificationService,
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(HospitalOpsPageComponent);
    component = fixture.componentInstance;
    clinicService = TestBed.inject(ClinicService);
  });

  it('should create and load metrics and beds on init', () => {
    vi.spyOn(clinicService, 'getOccupancyMetrics').mockReturnValue(of({
      totalBeds: 40,
      occupiedBeds: 28,
      occupancyRatePercent: 70.0,
      averageLengthOfStayDays: 3.5,
    }));

    vi.spyOn(clinicService, 'getBeds').mockReturnValue(of([
      {
        id: 'bed-1',
        roomId: 'room-1',
        roomNumber: '101',
        wardId: 'ward-1',
        wardName: 'Cardiology Ward',
        bedNumber: 'B-101',
        status: 'FREE',
        active: true,
        createdAt: Date.now(),
        updatedAt: Date.now(),
      }
    ]));

    vi.spyOn(clinicService, 'getWards').mockReturnValue(of([]));

    component.ngOnInit();
    fixture.detectChanges();

    expect(component).toBeTruthy();
    expect(component.metrics().totalBeds).toBe(40);
    expect(component.beds().length).toBe(1);
  });

  it('should switch to admissions tab and load active admissions', () => {
    vi.spyOn(clinicService, 'getAdmissions').mockReturnValue(of([
      {
        id: 'adm-1',
        patientId: 'pat-1',
        patientMrn: 'MRN-00001',
        patientName: 'Amr Hassan',
        admittingDoctorId: 'DOC-01',
        status: 'ADMITTED',
        chiefComplaint: 'Chest Pain',
        admittedAt: Date.now(),
        bedStays: [],
        createdAt: Date.now(),
        updatedAt: Date.now(),
      }
    ]));

    vi.spyOn(clinicService, 'getMarEntries').mockReturnValue(of([]));
    vi.spyOn(clinicService, 'getFluidIoEntries').mockReturnValue(of([]));
    vi.spyOn(clinicService, 'getNursingNotes').mockReturnValue(of([]));

    component.setTab('ADMISSIONS');
    fixture.detectChanges();

    expect(component.activeTab()).toBe('ADMISSIONS');
    expect(component.admissions().length).toBe(1);
  });

  it('should switch to OT tab and load schedules', () => {
    vi.spyOn(clinicService, 'getOtSchedules').mockReturnValue(of([
      {
        id: 'ot-1',
        theaterName: 'OR-1',
        patientId: 'pat-1',
        patientMrn: 'MRN-00001',
        patientName: 'Amr Hassan',
        surgeonDoctorId: 'DOC-01',
        surgeryType: 'Appendectomy',
        status: 'PLANNED',
        plannedStart: Date.now() + 3600000,
        durationMinutes: 60,
        charges: [],
        createdAt: Date.now(),
        updatedAt: Date.now(),
      }
    ]));

    component.setTab('OT');
    fixture.detectChanges();

    expect(component.activeTab()).toBe('OT');
    expect(component.otSchedules().length).toBe(1);
  });
});
