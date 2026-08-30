import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { AppointmentsPageComponent } from './appointments.page';
import { ClinicService } from './clinic.service';
import { I18nService } from '../../core/i18n.service';
import { of } from 'rxjs';

describe('AppointmentsPageComponent', () => {
  let component: AppointmentsPageComponent;
  let fixture: ComponentFixture<AppointmentsPageComponent>;
  let clinicService: ClinicService;
  let router: Router;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AppointmentsPageComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        ClinicService,
        I18nService,
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AppointmentsPageComponent);
    component = fixture.componentInstance;
    clinicService = TestBed.inject(ClinicService);
    router = TestBed.inject(Router);
  });

  it('should create and load initial appointments and slots', () => {
    vi.spyOn(clinicService, 'getAppointments').mockReturnValue(of([
      {
        id: 'appt-1',
        patientId: 'pat-1',
        patientName: 'Ahmed Ali',
        patientMrn: 'MRN-00001',
        patientPhone: '01001234567',
        doctorEmployeeId: 'doc-1',
        doctorName: 'Dr. Tarek',
        visitDate: '2026-08-30',
        startTime: '10:00',
        startsAt: 1788700000000,
        durationMinutes: 20,
        status: 'BOOKED',
        source: 'PHONE',
        createdAt: 1000,
        updatedAt: 1000,
      }
    ]));
    vi.spyOn(clinicService, 'getAvailableSlots').mockReturnValue(of([
      {
        startTime: '09:00',
        startsAt: 1788700000000,
        durationMinutes: 20,
        available: true,
      }
    ]));
    vi.spyOn(clinicService, 'getRostersForDoctor').mockReturnValue(of([]));
    vi.spyOn(clinicService, 'getAppointmentMetrics').mockReturnValue(of({
      period: '2026-08',
      totalAppointments: 1,
      bookedCount: 1,
      confirmedCount: 0,
      checkedInCount: 0,
      completedCount: 0,
      noShowCount: 0,
      cancelledCount: 0,
      noShowRatePercent: 0,
    }));

    component.ngOnInit();
    fixture.detectChanges();

    expect(component).toBeTruthy();
    expect(component.appointments().length).toBe(1);
    expect(component.availableSlots().length).toBe(1);
  });

  it('should check-in appointment and navigate to queue', () => {
    const navigateSpy = vi.spyOn(router, 'navigate');
    vi.spyOn(clinicService, 'checkInAppointment').mockReturnValue(of({
      id: 'appt-1',
      patientId: 'pat-1',
      patientName: 'Ahmed Ali',
      patientMrn: 'MRN-00001',
      patientPhone: '01001234567',
      doctorEmployeeId: 'doc-1',
      doctorName: 'Dr. Tarek',
      visitDate: '2026-08-30',
      startTime: '10:00',
      startsAt: 1788700000000,
      durationMinutes: 20,
      status: 'CHECKED_IN',
      source: 'PHONE',
      clinicVisitId: 'vis-1',
      createdAt: 1000,
      updatedAt: 1000,
    }));
    vi.spyOn(clinicService, 'getAppointments').mockReturnValue(of([]));
    vi.spyOn(clinicService, 'getAvailableSlots').mockReturnValue(of([]));
    vi.spyOn(clinicService, 'getRostersForDoctor').mockReturnValue(of([]));
    vi.spyOn(clinicService, 'getAppointmentMetrics').mockReturnValue(of(null as any));

    const appt: any = { id: 'appt-1', status: 'BOOKED' };
    component.checkInAppointment(appt);

    expect(clinicService.checkInAppointment).toHaveBeenCalledWith('appt-1');
    expect(navigateSpy).toHaveBeenCalledWith(['/clinic/queue']);
  });
});
