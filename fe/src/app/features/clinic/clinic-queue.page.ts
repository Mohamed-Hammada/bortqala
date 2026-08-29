import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { ClinicService } from './clinic.service';
import {
  ClinicVisit,
  CompleteVisitPayload,
  Patient,
  PrescriptionLine,
  QueueVisitPayload,
} from './clinic.models';
import { I18nService } from '../../core/i18n.service';
import { NotificationService } from '../../core/notification.service';
import { ModalDialogComponent } from '../../shared/ui/modal-dialog/modal-dialog.component';

@Component({
  selector: 'app-clinic-queue-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, ModalDialogComponent],
  templateUrl: './clinic-queue.page.html',
  styleUrl: './clinic-queue.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ClinicQueuePageComponent implements OnInit {
  private readonly clinicService = inject(ClinicService);
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  readonly i18n = inject(I18nService);
  private readonly notification = inject(NotificationService);

  readonly visits = signal<ClinicVisit[]>([]);
  readonly loading = signal(false);
  readonly selectedDate = signal<string>(new Date().toISOString().substring(0, 10));
  readonly selectedDoctorId = signal<string>('doc-1');
  readonly tvMode = signal(false);

  // Computed buckets
  readonly waitingVisits = computed(() =>
    this.visits().filter((v) => v.status === 'WAITING')
  );
  readonly inRoomVisits = computed(() =>
    this.visits().filter((v) => v.status === 'IN_ROOM')
  );
  readonly completedVisits = computed(() =>
    this.visits().filter((v) => v.status === 'DONE')
  );
  readonly cancelledVisits = computed(() =>
    this.visits().filter((v) => v.status === 'CANCELLED')
  );

  // Walk-in queue modal
  readonly walkinModalOpen = signal(false);
  readonly patientsList = signal<Patient[]>([]);
  readonly patientSearchQuery = signal('');
  readonly walkinForm: FormGroup = this.fb.group({
    patientId: ['', Validators.required],
    doctorEmployeeId: ['doc-1', Validators.required],
    feeCharged: [200, [Validators.required, Validators.min(0)]],
    insuranceCovered: [0, [Validators.min(0)]],
    paymentMethod: ['CASH'],
  });

  // Consult & Prescription modal
  readonly consultModalOpen = signal(false);
  readonly activeConsultVisit = signal<ClinicVisit | null>(null);
  readonly consultForm: FormGroup = this.fb.group({
    chiefComplaint: [''],
    diagnosisIcd: [''],
    diagnosisNotes: [''],
    feeCharged: [200, Validators.required],
    insuranceCovered: [0],
    paymentMethod: ['CASH'],
    prescriptions: this.fb.array([]),
  });

  get prescriptionsArray(): FormArray {
    return this.consultForm.get('prescriptions') as FormArray;
  }

  ngOnInit(): void {
    this.route.queryParams.subscribe((params) => {
      if (params['tv'] === '1' || params['tv'] === 'true') {
        this.tvMode.set(true);
      }
    });
    this.loadQueue();
    this.loadPatientsList();
  }

  loadQueue(): void {
    this.loading.set(true);
    this.clinicService.getQueue(this.selectedDate(), this.selectedDoctorId()).subscribe({
      next: (res) => {
        this.visits.set(res);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
      },
    });
  }

  loadPatientsList(): void {
    this.clinicService.searchPatients('', 0, 100).subscribe({
      next: (res) => this.patientsList.set(res.content),
    });
  }

  openWalkinModal(): void {
    this.walkinForm.reset({
      doctorEmployeeId: this.selectedDoctorId(),
      feeCharged: 200,
      insuranceCovered: 0,
      paymentMethod: 'CASH',
    });
    this.walkinModalOpen.set(true);
  }

  submitWalkinQueue(): void {
    if (this.walkinForm.invalid) {
      this.walkinForm.markAllAsTouched();
      return;
    }
    const val = this.walkinForm.value;
    const payload: QueueVisitPayload = {
      patientId: val.patientId,
      doctorEmployeeId: val.doctorEmployeeId,
      visitDate: this.selectedDate(),
      feeCharged: val.feeCharged,
      insuranceCovered: val.insuranceCovered,
      paymentMethod: val.paymentMethod,
    };

    this.clinicService.queueVisit(payload).subscribe({
      next: () => {
        this.walkinModalOpen.set(false);
        this.notification.success(this.i18n.t('clinic.walkinQueue'));
        this.loadQueue();
      },
    });
  }

  callPatient(visit: ClinicVisit): void {
    this.clinicService.callVisit(visit.id).subscribe({
      next: () => {
        this.notification.success(this.i18n.t('clinic.call'));
        this.loadQueue();
      },
    });
  }

  openConsultModal(visit: ClinicVisit): void {
    this.activeConsultVisit.set(visit);
    this.prescriptionsArray.clear();

    if (visit.prescriptionLines && visit.prescriptionLines.length > 0) {
      visit.prescriptionLines.forEach((line) => {
        this.prescriptionsArray.push(this.createPrescriptionGroup(line));
      });
    }

    this.consultForm.patchValue({
      chiefComplaint: visit.chiefComplaint || '',
      diagnosisIcd: visit.diagnosisIcd || '',
      diagnosisNotes: visit.diagnosisNotes || '',
      feeCharged: visit.feeCharged,
      insuranceCovered: visit.insuranceCovered,
      paymentMethod: visit.paymentMethod || 'CASH',
    });

    this.consultModalOpen.set(true);
  }

  createPrescriptionGroup(line?: PrescriptionLine): FormGroup {
    return this.fb.group({
      drugName: [line?.drugName || '', Validators.required],
      dose: [line?.dose || '', Validators.required],
      frequency: [line?.frequency || '', Validators.required],
      duration: [line?.duration || '', Validators.required],
      instructions: [line?.instructions || ''],
    });
  }

  addPrescriptionLine(): void {
    this.prescriptionsArray.push(this.createPrescriptionGroup());
  }

  removePrescriptionLine(index: number): void {
    this.prescriptionsArray.removeAt(index);
  }

  submitCompleteVisit(): void {
    const visit = this.activeConsultVisit();
    if (!visit) return;

    const val = this.consultForm.value;
    const payload: CompleteVisitPayload = {
      chiefComplaint: val.chiefComplaint?.trim(),
      diagnosisIcd: val.diagnosisIcd?.trim(),
      diagnosisNotes: val.diagnosisNotes?.trim(),
      feeCharged: val.feeCharged,
      insuranceCovered: val.insuranceCovered,
      paymentMethod: val.paymentMethod,
      prescriptionLines: val.prescriptions,
    };

    this.clinicService.completeVisit(visit.id, payload).subscribe({
      next: () => {
        this.consultModalOpen.set(false);
        this.notification.success(this.i18n.t('clinic.complete'));
        this.loadQueue();
      },
    });
  }

  cancelVisit(visit: ClinicVisit): void {
    this.clinicService.cancelVisit(visit.id).subscribe({
      next: () => {
        this.notification.success(this.i18n.t('clinic.cancel'));
        this.loadQueue();
      },
    });
  }

  printPrescription(): void {
    window.print();
  }

  toggleTvMode(): void {
    this.tvMode.update((v) => !v);
  }
}
