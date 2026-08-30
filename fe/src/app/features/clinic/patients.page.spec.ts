import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { of } from 'rxjs';
import { PatientsPageComponent } from './patients.page';
import { ClinicService } from './clinic.service';
import { I18nService } from '../../core/i18n.service';
import { NotificationService } from '../../core/notification.service';
import { Patient } from './clinic.models';

import { provideRouter } from '@angular/router';

describe('PatientsPageComponent', () => {
  let component: PatientsPageComponent;
  let fixture: ComponentFixture<PatientsPageComponent>;
  let clinicService: any;

  const mockPatient: Patient = {
    id: 'pat-1',
    mrn: 'MRN-00001',
    fullName: 'Mahmoud Hassan',
    phone: '01001234567',
    nationalId: '29008200101534',
    gender: 'MALE',
    birthDate: '1990-08-20',
    bloodGroup: 'O_POSITIVE',
    allergiesText: 'None',
    notes: 'Healthy',
    emergencyContactName: 'Fatma',
    emergencyContactPhone: '01111223344',
    createdAt: 1724900000000,
    updatedAt: 1724900000000,
  };

  beforeEach(async () => {
    clinicService = {
      searchPatients: vi.fn().mockReturnValue(
        of({
          content: [mockPatient],
          totalElements: 1,
          totalPages: 1,
          number: 0,
          size: 20,
        })
      ),
      registerPatient: vi.fn().mockReturnValue(of(mockPatient)),
      updatePatient: vi.fn().mockReturnValue(of(mockPatient)),
      checkDuplicates: vi.fn().mockReturnValue(
        of({
          duplicateFound: false,
          matchingPatients: [],
        })
      ),
      parseNationalId: vi.fn().mockReturnValue(
        of({
          valid: true,
          nationalId: '29008200101534',
          birthDate: '1990-08-20',
          gender: 'MALE',
        })
      ),
    };

    await TestBed.configureTestingModule({
      imports: [PatientsPageComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ClinicService, useValue: clinicService },
        { provide: I18nService, useValue: { t: (k: string) => k, locale: () => 'en-US' } },
        { provide: NotificationService, useValue: { success: vi.fn(), error: vi.fn() } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PatientsPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and load patients list', () => {
    expect(component).toBeTruthy();
    expect(component.patients().length).toBe(1);
    expect(component.patients()[0].mrn).toBe('MRN-00001');
    expect(component.patients()[0].fullName).toBe('Mahmoud Hassan');
  });

  it('should open new patient drawer and reset form', () => {
    component.openNewPatientDrawer();
    expect(component.drawerOpen()).toBe(true);
    expect(component.editingPatientId()).toBeNull();
  });

  it('should auto-populate birthDate and gender on valid National ID parse', () => {
    component.openNewPatientDrawer();
    component.patientForm.patchValue({ nationalId: '29008200101534' });
    component.onNationalIdChange();

    expect(clinicService.parseNationalId).toHaveBeenCalledWith('29008200101534');
    expect(component.patientForm.get('birthDate')?.value).toBe('1990-08-20');
    expect(component.patientForm.get('gender')?.value).toBe('MALE');
  });

  it('should show duplicate dialog when duplicate phone is found', () => {
    clinicService.checkDuplicates.mockReturnValue(
      of({
        duplicateFound: true,
        matchingPatients: [mockPatient],
      })
    );

    component.openNewPatientDrawer();
    component.patientForm.patchValue({
      fullName: 'New Duplicate',
      phone: '01001234567',
    });

    component.onSubmit();

    expect(clinicService.checkDuplicates).toHaveBeenCalled();
    expect(component.duplicateDialogOpen()).toBe(true);
    expect(component.duplicateMatches().length).toBe(1);
  });
});
