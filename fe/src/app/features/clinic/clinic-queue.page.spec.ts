import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { ClinicQueuePageComponent } from './clinic-queue.page';
import { ClinicService } from './clinic.service';
import { I18nService } from '../../core/i18n.service';
import { NotificationService } from '../../core/notification.service';
import { ClinicVisit, Patient } from './clinic.models';

describe('ClinicQueuePageComponent', () => {
  let component: ClinicQueuePageComponent;
  let fixture: ComponentFixture<ClinicQueuePageComponent>;
  let clinicService: any;

  const mockWaitingVisit: ClinicVisit = {
    id: 'vis-1',
    patientId: 'pat-1',
    patientName: 'Ali Hassan',
    patientMrn: 'MRN-00001',
    patientPhone: '01000000001',
    doctorEmployeeId: 'doc-1',
    doctorName: 'Dr. Mostafa Kamel',
    visitDate: '2026-08-29',
    visitTime: 1724900000000,
    token: 1,
    status: 'WAITING',
    feeCharged: 200,
    insuranceCovered: 0,
    patientShare: 200,
    paymentMethod: 'CASH',
    prescriptionLines: [],
    createdAt: 1724900000000,
    updatedAt: 1724900000000,
  };

  const mockInRoomVisit: ClinicVisit = {
    ...mockWaitingVisit,
    id: 'vis-2',
    token: 2,
    status: 'IN_ROOM',
  };

  const mockPatient: Patient = {
    id: 'pat-1',
    mrn: 'MRN-00001',
    fullName: 'Ali Hassan',
    phone: '01000000001',
    gender: 'MALE',
    createdAt: 1724900000000,
    updatedAt: 1724900000000,
  };

  beforeEach(async () => {
    clinicService = {
      getQueue: vi.fn().mockReturnValue(of([mockWaitingVisit, mockInRoomVisit])),
      searchPatients: vi.fn().mockReturnValue(
        of({
          content: [mockPatient],
          totalElements: 1,
          totalPages: 1,
          number: 0,
          size: 100,
        })
      ),
      queueVisit: vi.fn().mockReturnValue(of(mockWaitingVisit)),
      callVisit: vi.fn().mockReturnValue(of({ ...mockWaitingVisit, status: 'IN_ROOM' })),
      completeVisit: vi.fn().mockReturnValue(of({ ...mockInRoomVisit, status: 'DONE' })),
      cancelVisit: vi.fn().mockReturnValue(of({ ...mockWaitingVisit, status: 'CANCELLED' })),
    };

    await TestBed.configureTestingModule({
      imports: [ClinicQueuePageComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: ClinicService, useValue: clinicService },
        { provide: I18nService, useValue: { t: (k: string) => k, locale: () => 'en-US' } },
        { provide: NotificationService, useValue: { success: vi.fn(), error: vi.fn() } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ClinicQueuePageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and load visits partitioned by status', () => {
    expect(component).toBeTruthy();
    expect(component.visits().length).toBe(2);
    expect(component.waitingVisits().length).toBe(1);
    expect(component.inRoomVisits().length).toBe(1);
    expect(component.completedVisits().length).toBe(0);
  });

  it('should call patient and trigger queue reload', () => {
    component.callPatient(mockWaitingVisit);
    expect(clinicService.callVisit).toHaveBeenCalledWith('vis-1');
  });

  it('should submit complete consultation visit', () => {
    component.openConsultModal(mockInRoomVisit);
    component.consultForm.patchValue({
      chiefComplaint: 'Sore throat',
      diagnosisIcd: 'J02',
    });

    component.submitCompleteVisit();
    expect(clinicService.completeVisit).toHaveBeenCalled();
  });
});
