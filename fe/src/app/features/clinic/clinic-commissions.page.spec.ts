import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { of } from 'rxjs';
import { ClinicCommissionsPageComponent } from './clinic-commissions.page';
import { ClinicService } from './clinic.service';
import { I18nService } from '../../core/i18n.service';
import { DoctorCommissionStatement } from './clinic.models';

describe('ClinicCommissionsPageComponent', () => {
  let component: ClinicCommissionsPageComponent;
  let fixture: ComponentFixture<ClinicCommissionsPageComponent>;
  let clinicService: any;

  const mockStatement: DoctorCommissionStatement = {
    doctorEmployeeId: 'doc-1',
    doctorName: 'Dr. Mostafa Kamel',
    period: '2026-08',
    completedVisitsCount: 20,
    totalRevenue: 5000,
    commissionRatePercent: 60,
    commissionAmount: 3000,
    visits: [],
  };

  beforeEach(async () => {
    clinicService = {
      getCommissionStatement: vi.fn().mockReturnValue(of(mockStatement)),
    };

    await TestBed.configureTestingModule({
      imports: [ClinicCommissionsPageComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ClinicService, useValue: clinicService },
        { provide: I18nService, useValue: { t: (k: string) => k, locale: () => 'en-US' } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ClinicCommissionsPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and load commission statement calculation', () => {
    expect(component).toBeTruthy();
    expect(component.statement()).toEqual(mockStatement);
    expect(component.statement()?.commissionAmount).toBe(3000);
    expect(component.statement()?.totalRevenue).toBe(5000);
    expect(component.statement()?.completedVisitsCount).toBe(20);
  });
});
