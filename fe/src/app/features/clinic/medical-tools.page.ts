import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { I18nService } from '../../core/i18n.service';
import { NotificationService } from '../../core/notification.service';
import { ClinicService } from './clinic.service';
import {
  MedicalLicenseRecord,
  PediatricDoseCalculationRequest,
  PediatricDoseCalculationResponse,
  RegisterMedicalLicensePayload,
  ScheduleTelemedicineSessionPayload,
  TelemedicineSession,
} from './clinic.models';

@Component({
  selector: 'app-medical-tools-page',
  standalone: true,
  imports: [CommonModule, FormsModule, DecimalPipe],
  templateUrl: './medical-tools.page.html',
  styleUrls: ['./medical-tools.page.scss'],
})
export class MedicalToolsPageComponent implements OnInit {
  private readonly clinicService = inject(ClinicService);
  private readonly notification = inject(NotificationService);
  readonly i18n = inject(I18nService);

  readonly activeTab = signal<'calculator' | 'telemedicine' | 'licenses'>('calculator');
  readonly loading = signal<boolean>(false);
  readonly saving = signal<boolean>(false);

  // Pediatric Dose Calculator
  calcInput: PediatricDoseCalculationRequest = {
    weightKg: 15,
    doseMgPerKgPerDay: 40,
    frequencyPerDay: 3,
    drugConcentrationMgPerMl: 50,
  };
  calcResult = signal<PediatricDoseCalculationResponse | null>(null);

  // Telemedicine
  readonly sessions = signal<TelemedicineSession[]>([]);
  readonly showTelemedModal = signal<boolean>(false);
  telemedForm: ScheduleTelemedicineSessionPayload = {
    patientId: '',
    doctorId: 'DOC-01',
    doctorName: 'Dr. Tarek Fouad',
    scheduledTime: Date.now() + 3600000,
    roomName: '',
  };

  // Licenses
  readonly licenses = signal<MedicalLicenseRecord[]>([]);
  readonly showLicenseModal = signal<boolean>(false);
  licenseForm: RegisterMedicalLicensePayload = {
    practitionerId: '',
    practitionerName: '',
    licenseType: 'PHYSICIAN',
    licenseNumber: '',
    issuingAuthority: 'MOH',
    issueDate: Date.now(),
    expiryDate: Date.now() + 365 * 24 * 60 * 60 * 1000,
  };

  ngOnInit(): void {
    this.calculateDose();
    this.loadLicenses();
  }

  calculateDose(): void {
    if (!this.calcInput.weightKg || !this.calcInput.doseMgPerKgPerDay || !this.calcInput.frequencyPerDay) return;
    this.clinicService.calculatePediatricDose(this.calcInput).subscribe({
      next: (res) => this.calcResult.set(res),
    });
  }

  loadLicenses(): void {
    this.loading.set(true);
    this.clinicService.getAllLicenses().subscribe({
      next: (res) => {
        this.licenses.set(res);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  openTelemedModal(): void {
    this.telemedForm = {
      patientId: '',
      doctorId: 'DOC-01',
      doctorName: 'Dr. Tarek Fouad',
      scheduledTime: Date.now() + 3600000,
      roomName: 'bemo-room-' + Math.floor(1000 + Math.random() * 9000),
    };
    this.showTelemedModal.set(true);
  }

  submitTelemed(): void {
    if (!this.telemedForm.patientId) return;
    this.saving.set(true);
    this.clinicService.scheduleTelemedicineSession(this.telemedForm).subscribe({
      next: (created) => {
        this.saving.set(false);
        this.showTelemedModal.set(false);
        this.sessions.update((list) => [created, ...list]);
        this.notification.success(this.i18n.t('clinic.telemedScheduled'));
      },
      error: () => this.saving.set(false),
    });
  }

  startVideo(session: TelemedicineSession): void {
    window.open(session.meetingLink, '_blank');
  }

  openLicenseModal(): void {
    this.licenseForm = {
      practitionerId: 'PRAC-' + Math.floor(100 + Math.random() * 900),
      practitionerName: '',
      licenseType: 'PHYSICIAN',
      licenseNumber: 'MOH-' + Math.floor(10000 + Math.random() * 90000),
      issuingAuthority: 'MOH',
      issueDate: Date.now(),
      expiryDate: Date.now() + 365 * 24 * 60 * 60 * 1000,
    };
    this.showLicenseModal.set(true);
  }

  submitLicense(): void {
    if (!this.licenseForm.practitionerName || !this.licenseForm.licenseNumber) return;
    this.saving.set(true);
    this.clinicService.registerLicense(this.licenseForm).subscribe({
      next: () => {
        this.saving.set(false);
        this.showLicenseModal.set(false);
        this.loadLicenses();
        this.notification.success(this.i18n.t('clinic.licenseRegistered'));
      },
      error: () => this.saving.set(false),
    });
  }
}
