import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';
import { PatientChartPageComponent } from './patient-chart.page';
import { ClinicService } from './clinic.service';
import { I18nService } from '../../core/i18n.service';
import { NotificationCenterService } from '../../core/notification-center/notification-center.service';
import { PatientChart } from './clinic.models';

describe('PatientChartPageComponent', () => {
  let component: PatientChartPageComponent;
  let fixture: ComponentFixture<PatientChartPageComponent>;
  let clinicService: any;

  const mockChart: PatientChart = {
    patient: {
      id: 'pat-1',
      mrn: 'MRN-00001',
      fullName: 'Mahmoud Hassan',
      phone: '01001234567',
      nationalId: '29008200101534',
      gender: 'MALE',
      birthDate: '1990-08-20',
      bloodGroup: 'O_POSITIVE',
      allergiesText: 'Penicillin',
      notes: 'Asthmatic',
      emergencyContactName: 'Fatma',
      emergencyContactPhone: '01111223344',
      createdAt: 1724900000000,
      updatedAt: 1724900000000,
    },
    allergies: [
      {
        id: 'all-1',
        patientId: 'pat-1',
        substance: 'Penicillin',
        severity: 'SEVERE',
        reactionNotes: 'Anaphylaxis shock',
        notedAt: 1724900000000,
      },
    ],
    hasSevereAllergies: true,
    conditions: [
      {
        id: 'cond-1',
        patientId: 'pat-1',
        icdCode: 'J45',
        label: 'Bronchial Asthma',
        chronic: true,
        onsetDate: '2015-05-10',
        status: 'ACTIVE',
        notes: 'Uses inhaler',
        createdAt: 1724900000000,
      },
    ],
    vitalsHistory: [
      {
        id: 'vit-1',
        visitId: 'vis-1',
        patientId: 'pat-1',
        systolicBp: 120,
        diastolicBp: 80,
        pulse: 72,
        tempC: 37.0,
        spo2: 98,
        weightKg: 80.0,
        heightCm: 175.0,
        bmi: 26.1,
        notes: 'Normal',
        recordedAt: 1724900000000,
      },
    ],
    recentVisits: [
      {
        id: 'vis-1',
        patientId: 'pat-1',
        patientName: 'Mahmoud Hassan',
        patientMrn: 'MRN-00001',
        patientPhone: '01001234567',
        doctorEmployeeId: 'doc-1',
        doctorName: 'Dr. Tarek',
        visitDate: '2026-08-29',
        visitTime: 1724900000000,
        token: 1,
        status: 'DONE',
        chiefComplaint: 'Shortness of breath',
        diagnosisIcd: 'J45',
        diagnosisNotes: 'Acute mild asthma exacerbation',
        feeCharged: 300,
        insuranceCovered: 240,
        patientShare: 60,
        paymentMethod: 'CASH',
        prescriptionLines: [
          {
            id: 'rx-1',
            drugName: 'Ventolin Inhaler',
            dose: '2 puffs',
            frequency: 'Every 6 hours',
            duration: '5 days',
            instructions: 'As needed for wheezing',
            createdAt: 1724900000000,
          },
        ],
        createdAt: 1724900000000,
        updatedAt: 1724900000000,
      },
    ],
    documents: [],
    consents: [
      {
        id: 'con-1',
        patientId: 'pat-1',
        visitId: 'vis-1',
        templateKey: 'GENERAL_TREATMENT',
        title: 'General Treatment Consent',
        bodyText: 'I consent to general medical treatment.',
        signedByName: 'Mahmoud Hassan',
        signedByRelation: 'SELF',
        signedAt: 1724900000000,
        ipAddress: '127.0.0.1',
      },
    ],
  };

  beforeEach(async () => {
    clinicService = {
      getPatientChart: vi.fn().mockReturnValue(of(mockChart)),
      recordVitals: vi.fn().mockReturnValue(of(mockChart.vitalsHistory[0])),
      addAllergy: vi.fn().mockReturnValue(of(mockChart.allergies[0])),
      deleteAllergy: vi.fn().mockReturnValue(of(undefined)),
      addCondition: vi.fn().mockReturnValue(of(mockChart.conditions[0])),
      deleteCondition: vi.fn().mockReturnValue(of(undefined)),
      uploadDocument: vi.fn().mockReturnValue(of({})),
      deleteDocument: vi.fn().mockReturnValue(of(undefined)),
      signConsent: vi.fn().mockReturnValue(of(mockChart.consents[0])),
      getOrdersByPatient: vi.fn().mockReturnValue(of([])),
    };

    await TestBed.configureTestingModule({
      imports: [PatientChartPageComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: {
                get: (key: string) => (key === 'id' ? 'pat-1' : null),
              },
            },
          },
        },
        { provide: ClinicService, useValue: clinicService },
        { provide: I18nService, useValue: { t: (k: string) => k, locale: () => 'en-US' } },
        { provide: NotificationCenterService, useValue: {} },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PatientChartPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and load patient chart', () => {
    expect(component).toBeTruthy();
    expect(clinicService.getPatientChart).toHaveBeenCalledWith('pat-1');
    expect(component.chart()?.patient.mrn).toBe('MRN-00001');
    expect(component.chart()?.hasSevereAllergies).toBe(true);
  });

  it('should compute BMI correctly from vitals inputs', () => {
    component.vitalsForm = {
      weightKg: 80.0,
      heightCm: 175.0,
    };
    expect(component.computedBmi()).toBe(26.1);
  });

  it('should switch chart tabs correctly', () => {
    expect(component.activeTab()).toBe('history');
    component.setTab('vitals');
    expect(component.activeTab()).toBe('vitals');
    component.setTab('allergies');
    expect(component.activeTab()).toBe('allergies');
  });

  it('should open and save allergy modal', () => {
    component.openAllergyModal();
    expect(component.showAllergyModal()).toBe(true);

    component.allergyForm = {
      substance: 'Aspirin',
      severity: 'SEVERE',
      reactionNotes: 'Bronchospasm',
    };

    component.saveAllergy();
    expect(clinicService.addAllergy).toHaveBeenCalledWith('pat-1', component.allergyForm);
  });

  it('should open and sign consent form', () => {
    component.openConsentModal();
    expect(component.showConsentModal()).toBe(true);
    expect(component.consentForm.signedByName).toBe('Mahmoud Hassan');

    component.saveConsent();
    expect(clinicService.signConsent).toHaveBeenCalledWith('pat-1', component.consentForm);
  });
});
