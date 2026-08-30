import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ClinicService } from './clinic.service';
import { DuplicateCheckResponse, Patient, RegisterPatientPayload } from './clinic.models';
import { I18nService } from '../../core/i18n.service';
import { NotificationService } from '../../core/notification.service';
import { RouterLink } from '@angular/router';
import { TablePaginationComponent } from '../../shared/ui/table-pagination/table-pagination.component';
import { ModalDialogComponent } from '../../shared/ui/modal-dialog/modal-dialog.component';

@Component({
  selector: 'app-patients-page',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    TablePaginationComponent,
    ModalDialogComponent,
  ],
  templateUrl: './patients.page.html',
  styleUrl: './patients.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PatientsPageComponent implements OnInit {
  private readonly clinicService = inject(ClinicService);
  private readonly fb = inject(FormBuilder);
  readonly i18n = inject(I18nService);
  private readonly notification = inject(NotificationService);

  readonly patients = signal<Patient[]>([]);
  readonly loading = signal(false);
  readonly totalElements = signal(0);
  readonly totalPages = signal(0);
  readonly page = signal(0);
  readonly pageSize = signal(20);
  readonly searchQuery = signal('');

  readonly drawerOpen = signal(false);
  readonly editingPatientId = signal<string | null>(null);
  readonly saving = signal(false);

  readonly duplicateDialogOpen = signal(false);
  readonly duplicateMatches = signal<Patient[]>([]);

  patientForm: FormGroup = this.fb.group({
    fullName: ['', [Validators.required]],
    phone: ['', [Validators.required]],
    nationalId: [''],
    gender: ['UNKNOWN'],
    birthDate: [''],
    bloodGroup: [''],
    allergiesText: [''],
    notes: [''],
    emergencyContactName: [''],
    emergencyContactPhone: [''],
  });

  ngOnInit(): void {
    this.loadPatients();
  }

  loadPatients(): void {
    this.loading.set(true);
    this.clinicService
      .searchPatients(this.searchQuery(), this.page(), this.pageSize())
      .subscribe({
        next: (res) => {
          this.patients.set(res.content);
          this.totalElements.set(res.totalElements);
          this.totalPages.set(res.totalPages);
          this.loading.set(false);
        },
        error: () => {
          this.loading.set(false);
        },
      });
  }

  onSearch(query: string): void {
    this.searchQuery.set(query);
    this.page.set(0);
    this.loadPatients();
  }

  onPageChange(newPage: number): void {
    this.page.set(newPage);
    this.loadPatients();
  }

  openNewPatientDrawer(): void {
    this.editingPatientId.set(null);
    this.patientForm.reset({ gender: 'UNKNOWN' });
    this.drawerOpen.set(true);
  }

  openEditPatientDrawer(patient: Patient): void {
    this.editingPatientId.set(patient.id);
    this.patientForm.patchValue({
      fullName: patient.fullName,
      phone: patient.phone,
      nationalId: patient.nationalId || '',
      gender: patient.gender || 'UNKNOWN',
      birthDate: patient.birthDate || '',
      bloodGroup: patient.bloodGroup || '',
      allergiesText: patient.allergiesText || '',
      notes: patient.notes || '',
      emergencyContactName: patient.emergencyContactName || '',
      emergencyContactPhone: patient.emergencyContactPhone || '',
    });
    this.drawerOpen.set(true);
  }

  closeDrawer(): void {
    this.drawerOpen.set(false);
    this.editingPatientId.set(null);
  }

  onNationalIdChange(): void {
    const nationalId = this.patientForm.get('nationalId')?.value;
    if (nationalId && nationalId.trim().length === 14) {
      this.clinicService.parseNationalId(nationalId.trim()).subscribe({
        next: (res) => {
          if (res.valid) {
            if (res.birthDate && !this.patientForm.get('birthDate')?.value) {
              this.patientForm.patchValue({ birthDate: res.birthDate });
            }
            if (res.gender) {
              this.patientForm.patchValue({ gender: res.gender });
            }
          }
        },
      });
    }
  }

  onSubmit(): void {
    if (this.patientForm.invalid) {
      this.patientForm.markAllAsTouched();
      return;
    }

    const val = this.patientForm.value;
    const payload: RegisterPatientPayload = {
      fullName: val.fullName?.trim(),
      phone: val.phone?.trim(),
      nationalId: val.nationalId?.trim() || null,
      gender: val.gender || 'UNKNOWN',
      birthDate: val.birthDate || null,
      bloodGroup: val.bloodGroup || null,
      allergiesText: val.allergiesText?.trim() || null,
      notes: val.notes?.trim() || null,
      emergencyContactName: val.emergencyContactName?.trim() || null,
      emergencyContactPhone: val.emergencyContactPhone?.trim() || null,
    };

    if (!this.editingPatientId()) {
      // Check duplicates on new registration
      this.clinicService.checkDuplicates(payload.phone, payload.nationalId || undefined).subscribe({
        next: (dupRes) => {
          if (dupRes.duplicateFound && dupRes.matchingPatients.length > 0) {
            this.duplicateMatches.set(dupRes.matchingPatients);
            this.duplicateDialogOpen.set(true);
          } else {
            this.performSave(payload);
          }
        },
        error: () => this.performSave(payload),
      });
    } else {
      this.performSave(payload);
    }
  }

  proceedWithDuplicateRegistration(): void {
    this.duplicateDialogOpen.set(false);
    const val = this.patientForm.value;
    this.performSave(val);
  }

  openMatchedPatient(patient: Patient): void {
    this.duplicateDialogOpen.set(false);
    this.openEditPatientDrawer(patient);
  }

  private performSave(payload: RegisterPatientPayload): void {
    this.saving.set(true);
    const id = this.editingPatientId();

    const request$ = id
      ? this.clinicService.updatePatient(id, payload)
      : this.clinicService.registerPatient(payload);

    request$.subscribe({
      next: () => {
        this.saving.set(false);
        this.drawerOpen.set(false);
        this.notification.success(this.i18n.t(id ? 'common.saved' : 'clinic.newPatient'));
        this.loadPatients();
      },
      error: () => {
        this.saving.set(false);
      },
    });
  }
}
