import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { I18nService } from '../../core/i18n.service';
import { NotificationService } from '../../core/notification.service';
import {
  AddNursingNotePayload,
  AddOtChargePayload,
  AdministerMarEntryPayload,
  AdmitPatientPayload,
  CompleteOtSurgeryPayload,
  CreateMarEntryPayload,
  DischargePatientPayload,
  HospitalAdmission,
  HospitalBed,
  HospitalFluidIoEntry,
  HospitalMarEntry,
  HospitalNursingNote,
  HospitalOccupancyMetrics,
  HospitalOtSchedule,
  HospitalRoom,
  HospitalWard,
  RecordFluidIoPayload,
  SaveHospitalBedPayload,
  SaveHospitalRoomPayload,
  SaveHospitalWardPayload,
  ScheduleOtPayload,
  TransferPatientBedPayload,
} from './clinic.models';
import { ClinicService } from './clinic.service';

@Component({
  selector: 'app-hospital-ops-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './hospital-ops.page.html',
  styleUrls: ['./hospital-ops.page.scss'],
})
export class HospitalOpsPageComponent implements OnInit {
  private readonly clinicService = inject(ClinicService);
  private readonly notificationService = inject(NotificationService);
  readonly i18n = inject(I18nService);

  readonly activeTab = signal<'BEDS' | 'ADMISSIONS' | 'OT'>('BEDS');
  readonly loading = signal<boolean>(false);
  readonly saving = signal<boolean>(false);

  // Metrics
  readonly metrics = signal<HospitalOccupancyMetrics>({
    totalBeds: 0,
    occupiedBeds: 0,
    occupancyRatePercent: 0,
    averageLengthOfStayDays: 0,
  });

  // Wards, Rooms, Beds
  readonly wards = signal<HospitalWard[]>([]);
  readonly rooms = signal<HospitalRoom[]>([]);
  readonly beds = signal<HospitalBed[]>([]);
  readonly selectedBed = signal<HospitalBed | null>(null);

  // Admissions & Inpatient Chart
  readonly admissions = signal<HospitalAdmission[]>([]);
  readonly selectedAdmission = signal<HospitalAdmission | null>(null);
  readonly marEntries = signal<HospitalMarEntry[]>([]);
  readonly fluidEntries = signal<HospitalFluidIoEntry[]>([]);
  readonly nursingNotes = signal<HospitalNursingNote[]>([]);

  // Operating Theater
  readonly otSchedules = signal<HospitalOtSchedule[]>([]);
  readonly selectedOtSchedule = signal<HospitalOtSchedule | null>(null);

  // Modals
  readonly showAdmitModal = signal<boolean>(false);
  admitForm: AdmitPatientPayload = {
    patientId: '',
    admittingDoctorId: '',
    bedId: '',
    chiefComplaint: '',
  };

  readonly showTransferModal = signal<boolean>(false);
  transferForm: TransferPatientBedPayload = {
    targetBedId: '',
    transferReason: '',
  };

  readonly showDischargeModal = signal<boolean>(false);
  dischargeForm: DischargePatientPayload = {
    dischargeSummary: '',
  };

  readonly showMarModal = signal<boolean>(false);
  marForm: CreateMarEntryPayload = {
    admissionId: '',
    medicationName: '',
    dose: '',
    route: 'ORAL',
    dueAt: Date.now(),
  };

  readonly showFluidModal = signal<boolean>(false);
  fluidForm: RecordFluidIoPayload = {
    admissionId: '',
    type: 'INTAKE',
    routeOrFluid: 'ORAL',
    amountMl: 250,
    recordedBy: '',
  };

  readonly showNoteModal = signal<boolean>(false);
  noteForm: AddNursingNotePayload = {
    admissionId: '',
    nurseName: '',
    noteText: '',
  };

  readonly showOtModal = signal<boolean>(false);
  otForm: ScheduleOtPayload = {
    theaterName: 'OR-1',
    patientId: '',
    surgeonDoctorId: '',
    surgeryType: '',
    plannedStart: Date.now() + 3600000,
    durationMinutes: 90,
  };

  readonly showOtChargeModal = signal<boolean>(false);
  otChargeForm: AddOtChargePayload = {
    itemName: '',
    quantity: 1,
    unitPrice: 100,
  };

  ngOnInit(): void {
    this.loadMetrics();
    this.loadBeds();
    this.loadWards();
  }

  setTab(tab: 'BEDS' | 'ADMISSIONS' | 'OT'): void {
    this.activeTab.set(tab);
    if (tab === 'BEDS') {
      this.loadMetrics();
      this.loadBeds();
    } else if (tab === 'ADMISSIONS') {
      this.loadAdmissions();
    } else if (tab === 'OT') {
      this.loadOtSchedules();
    }
  }

  loadMetrics(): void {
    this.clinicService.getOccupancyMetrics().subscribe({
      next: (data) => this.metrics.set(data),
    });
  }

  loadWards(): void {
    this.clinicService.getWards().subscribe({
      next: (data) => this.wards.set(data),
    });
  }

  loadBeds(): void {
    this.loading.set(true);
    this.clinicService.getBeds().subscribe({
      next: (data) => {
        this.beds.set(data);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  loadAdmissions(): void {
    this.loading.set(true);
    this.clinicService.getAdmissions('ADMITTED').subscribe({
      next: (data) => {
        this.admissions.set(data);
        if (data.length > 0 && !this.selectedAdmission()) {
          this.selectAdmission(data[0]);
        }
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  selectAdmission(admission: HospitalAdmission): void {
    this.selectedAdmission.set(admission);
    this.loadInpatientChart(admission.id);
  }

  loadInpatientChart(admissionId: string): void {
    this.clinicService.getMarEntries(admissionId).subscribe({
      next: (data) => this.marEntries.set(data),
    });
    this.clinicService.getFluidIoEntries(admissionId).subscribe({
      next: (data) => this.fluidEntries.set(data),
    });
    this.clinicService.getNursingNotes(admissionId).subscribe({
      next: (data) => this.nursingNotes.set(data),
    });
  }

  loadOtSchedules(): void {
    this.loading.set(true);
    this.clinicService.getOtSchedules().subscribe({
      next: (data) => {
        this.otSchedules.set(data);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  // Bed Actions
  openAdmitModal(bed?: HospitalBed): void {
    this.admitForm = {
      patientId: '',
      admittingDoctorId: 'DOC-01',
      bedId: bed ? bed.id : (this.beds().find((b) => b.status === 'FREE')?.id ?? ''),
      chiefComplaint: '',
    };
    this.showAdmitModal.set(true);
  }

  submitAdmit(): void {
    if (!this.admitForm.patientId || !this.admitForm.bedId) return;
    this.saving.set(true);
    this.clinicService.admitPatient(this.admitForm).subscribe({
      next: () => {
        this.saving.set(false);
        this.showAdmitModal.set(false);
        this.loadBeds();
        this.loadMetrics();
        this.notificationService.success(this.i18n.t('clinic.resultSuccess'));
      },
      error: () => this.saving.set(false),
    });
  }

  openTransferModal(admission: HospitalAdmission): void {
    this.selectedAdmission.set(admission);
    this.transferForm = {
      targetBedId: this.beds().find((b) => b.status === 'FREE')?.id ?? '',
      transferReason: '',
    };
    this.showTransferModal.set(true);
  }

  submitTransfer(): void {
    if (!this.selectedAdmission() || !this.transferForm.targetBedId) return;
    this.saving.set(true);
    this.clinicService.transferPatient(this.selectedAdmission()!.id, this.transferForm).subscribe({
      next: (updated) => {
        this.saving.set(false);
        this.showTransferModal.set(false);
        this.selectedAdmission.set(updated);
        this.loadBeds();
        this.notificationService.success(this.i18n.t('clinic.resultSuccess'));
      },
      error: () => this.saving.set(false),
    });
  }

  openDischargeModal(admission: HospitalAdmission): void {
    this.selectedAdmission.set(admission);
    this.dischargeForm = {
      dischargeSummary: '',
    };
    this.showDischargeModal.set(true);
  }

  submitDischarge(): void {
    if (!this.selectedAdmission() || this.dischargeForm.dischargeSummary.length < 20) return;
    this.saving.set(true);
    this.clinicService.dischargePatient(this.selectedAdmission()!.id, this.dischargeForm).subscribe({
      next: () => {
        this.saving.set(false);
        this.showDischargeModal.set(false);
        this.selectedAdmission.set(null);
        this.loadAdmissions();
        this.loadBeds();
        this.loadMetrics();
        this.notificationService.success(this.i18n.t('clinic.resultSuccess'));
      },
      error: () => this.saving.set(false),
    });
  }

  // Nursing Actions
  openAddMarModal(): void {
    if (!this.selectedAdmission()) return;
    this.marForm = {
      admissionId: this.selectedAdmission()!.id,
      medicationName: '',
      dose: '',
      route: 'ORAL',
      dueAt: Date.now(),
    };
    this.showMarModal.set(true);
  }

  submitMarEntry(): void {
    if (!this.marForm.medicationName || !this.marForm.dose) return;
    this.saving.set(true);
    this.clinicService.createMarEntry(this.marForm).subscribe({
      next: () => {
        this.saving.set(false);
        this.showMarModal.set(false);
        this.loadInpatientChart(this.selectedAdmission()!.id);
        this.notificationService.success(this.i18n.t('clinic.resultSuccess'));
      },
      error: () => this.saving.set(false),
    });
  }

  administerDose(entry: HospitalMarEntry, status: 'GIVEN' | 'REFUSED' | 'HELD'): void {
    const payload: AdministerMarEntryPayload = {
      status,
      nurseName: 'Nurse In-Charge',
      notes: status === 'GIVEN' ? 'Dose administered' : 'Dose withheld',
    };
    this.clinicService.administerMarEntry(entry.id, payload).subscribe({
      next: () => {
        this.loadInpatientChart(this.selectedAdmission()!.id);
        this.notificationService.success(this.i18n.t('clinic.resultSuccess'));
      },
    });
  }

  openAddFluidModal(): void {
    if (!this.selectedAdmission()) return;
    this.fluidForm = {
      admissionId: this.selectedAdmission()!.id,
      type: 'INTAKE',
      routeOrFluid: 'ORAL',
      amountMl: 250,
      recordedBy: 'Nurse',
    };
    this.showFluidModal.set(true);
  }

  submitFluidIo(): void {
    if (!this.fluidForm.routeOrFluid || this.fluidForm.amountMl <= 0) return;
    this.saving.set(true);
    this.clinicService.recordFluidIo(this.fluidForm).subscribe({
      next: () => {
        this.saving.set(false);
        this.showFluidModal.set(false);
        this.loadInpatientChart(this.selectedAdmission()!.id);
        this.notificationService.success(this.i18n.t('clinic.resultSuccess'));
      },
      error: () => this.saving.set(false),
    });
  }

  get totalIntake(): number {
    return this.fluidEntries()
      .filter((f) => f.type === 'INTAKE')
      .reduce((sum, f) => sum + f.amountMl, 0);
  }

  get totalOutput(): number {
    return this.fluidEntries()
      .filter((f) => f.type === 'OUTPUT')
      .reduce((sum, f) => sum + f.amountMl, 0);
  }

  get netFluid(): number {
    return this.totalIntake - this.totalOutput;
  }

  openAddNoteModal(): void {
    if (!this.selectedAdmission()) return;
    this.noteForm = {
      admissionId: this.selectedAdmission()!.id,
      nurseName: 'Nurse In-Charge',
      noteText: '',
    };
    this.showNoteModal.set(true);
  }

  submitNursingNote(): void {
    if (!this.noteForm.noteText) return;
    this.saving.set(true);
    this.clinicService.addNursingNote(this.noteForm).subscribe({
      next: () => {
        this.saving.set(false);
        this.showNoteModal.set(false);
        this.loadInpatientChart(this.selectedAdmission()!.id);
        this.notificationService.success(this.i18n.t('clinic.resultSuccess'));
      },
      error: () => this.saving.set(false),
    });
  }

  // OT Actions
  openScheduleOtModal(): void {
    this.otForm = {
      theaterName: 'OR-1',
      patientId: '',
      surgeonDoctorId: 'DOC-01',
      surgeryType: '',
      plannedStart: Date.now() + 3600000,
      durationMinutes: 90,
    };
    this.showOtModal.set(true);
  }

  submitScheduleOt(): void {
    if (!this.otForm.patientId || !this.otForm.surgeryType) return;
    this.saving.set(true);
    this.clinicService.scheduleOt(this.otForm).subscribe({
      next: () => {
        this.saving.set(false);
        this.showOtModal.set(false);
        this.loadOtSchedules();
        this.notificationService.success(this.i18n.t('clinic.resultSuccess'));
      },
      error: () => this.saving.set(false),
    });
  }

  startSurgery(ot: HospitalOtSchedule): void {
    this.clinicService.startOt(ot.id).subscribe({
      next: () => {
        this.loadOtSchedules();
        this.notificationService.success(this.i18n.t('clinic.resultSuccess'));
      },
    });
  }

  completeSurgery(ot: HospitalOtSchedule): void {
    this.clinicService.completeOt(ot.id, {
      anesthesiaNotes: 'General anesthesia uneventful',
      surgicalNotes: 'Procedure completed successfully without complications',
    }).subscribe({
      next: () => {
        this.loadOtSchedules();
        this.notificationService.success(this.i18n.t('clinic.resultSuccess'));
      },
    });
  }

  openAddChargeModal(ot: HospitalOtSchedule): void {
    this.selectedOtSchedule.set(ot);
    this.otChargeForm = {
      itemName: '',
      quantity: 1,
      unitPrice: 500,
    };
    this.showOtChargeModal.set(true);
  }

  submitAddCharge(): void {
    if (!this.selectedOtSchedule() || !this.otChargeForm.itemName) return;
    this.saving.set(true);
    this.clinicService.addOtCharge(this.selectedOtSchedule()!.id, this.otChargeForm).subscribe({
      next: () => {
        this.saving.set(false);
        this.showOtChargeModal.set(false);
        this.loadOtSchedules();
        this.notificationService.success(this.i18n.t('clinic.resultSuccess'));
      },
      error: () => this.saving.set(false),
    });
  }
}
