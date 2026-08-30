import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { I18nService } from '../../core/i18n.service';
import { NotificationCenterService } from '../../core/notification-center/notification-center.service';
import {
  AppointmentMetrics,
  AvailableSlot,
  BookAppointmentPayload,
  ClinicAppointment,
  DoctorRoster,
  Patient,
  SaveDoctorRosterPayload,
} from './clinic.models';
import { ClinicService } from './clinic.service';

@Component({
  selector: 'app-appointments-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './appointments.page.html',
  styleUrls: ['./appointments.page.scss'],
})
export class AppointmentsPageComponent implements OnInit {
  private readonly clinicService = inject(ClinicService);
  private readonly router = inject(Router);
  private readonly notificationCenter = inject(NotificationCenterService);
  readonly i18n = inject(I18nService);

  // Filter state
  readonly selectedDate = signal<string>(new Date().toISOString().substring(0, 10));
  readonly selectedDoctorId = signal<string>('doc-1');
  readonly doctors = signal<Array<{ id: string; name: string }>>([
    { id: 'doc-1', name: 'Dr. Tarek (Internal Medicine)' },
    { id: 'doc-2', name: 'Dr. Mona (Pediatrics)' },
    { id: 'doc-3', name: 'Dr. Sameh (Orthopedics)' },
  ]);

  // Data state
  readonly appointments = signal<ClinicAppointment[]>([]);
  readonly availableSlots = signal<AvailableSlot[]>([]);
  readonly rosters = signal<DoctorRoster[]>([]);
  readonly metrics = signal<AppointmentMetrics | null>(null);
  readonly loading = signal<boolean>(false);

  // Modals & Drawers
  readonly showBookModal = signal<boolean>(false);
  readonly showRosterDrawer = signal<boolean>(false);
  readonly saving = signal<boolean>(false);

  // Booking Form
  bookingForm: BookAppointmentPayload = {
    patientId: '',
    doctorEmployeeId: 'doc-1',
    visitDate: new Date().toISOString().substring(0, 10),
    startTime: '09:00',
    durationMinutes: 20,
    source: 'PHONE',
    reason: '',
  };

  // Inline Patient Search for Booking
  readonly patientSearchQuery = signal<string>('');
  readonly searchedPatients = signal<Patient[]>([]);
  readonly selectedPatient = signal<Patient | null>(null);

  // Roster Form
  rosterForm: SaveDoctorRosterPayload = {
    doctorEmployeeId: 'doc-1',
    weekday: 0,
    startTime: '09:00',
    endTime: '17:00',
    slotMinutes: 20,
    maxPatientsPerSlot: 1,
  };

  readonly weekdays = [
    { value: 0, label: 'Sunday / الأحد' },
    { value: 1, label: 'Monday / الإثنين' },
    { value: 2, label: 'Tuesday / الثلاثاء' },
    { value: 3, label: 'Wednesday / الأربعاء' },
    { value: 4, label: 'Thursday / الخميس' },
    { value: 5, label: 'Friday / الجمعة' },
    { value: 6, label: 'Saturday / السبت' },
  ];

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loading.set(true);
    const date = this.selectedDate();
    const docId = this.selectedDoctorId();
    const period = date.substring(0, 7); // 'yyyy-MM'

    this.clinicService.getAppointments(date, docId).subscribe({
      next: (appts) => {
        this.appointments.set(appts);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });

    this.clinicService.getAvailableSlots(docId, date).subscribe({
      next: (slots) => this.availableSlots.set(slots),
      error: () => this.availableSlots.set([]),
    });

    this.clinicService.getRostersForDoctor(docId).subscribe({
      next: (r) => this.rosters.set(r),
      error: () => this.rosters.set([]),
    });

    this.clinicService.getAppointmentMetrics(docId, period).subscribe({
      next: (m) => this.metrics.set(m),
      error: () => this.metrics.set(null),
    });
  }

  onDateChange(newDate: string): void {
    this.selectedDate.set(newDate);
    this.loadData();
  }

  onDoctorChange(doctorId: string): void {
    this.selectedDoctorId.set(doctorId);
    this.loadData();
  }

  // Appointment Status Actions
  confirmAppointment(appt: ClinicAppointment): void {
    this.clinicService.confirmAppointment(appt.id).subscribe({
      next: () => this.loadData(),
    });
  }

  checkInAppointment(appt: ClinicAppointment): void {
    this.clinicService.checkInAppointment(appt.id).subscribe({
      next: () => {
        this.loadData();
        this.router.navigate(['/clinic/queue']);
      },
    });
  }

  markNoShow(appt: ClinicAppointment): void {
    this.clinicService.markNoShow(appt.id).subscribe({
      next: () => this.loadData(),
    });
  }

  cancelAppointment(appt: ClinicAppointment): void {
    this.clinicService.cancelAppointment(appt.id).subscribe({
      next: () => this.loadData(),
    });
  }

  sendTomorrowReminders(): void {
    const cur = new Date(this.selectedDate());
    cur.setDate(cur.getDate() + 1);
    const tomorrow = cur.toISOString().substring(0, 10);

    this.clinicService.sendAppointmentReminders(tomorrow).subscribe({
      next: () => this.loadData(),
    });
  }

  // Booking Modal
  openBookModal(slot?: AvailableSlot): void {
    this.bookingForm = {
      patientId: '',
      doctorEmployeeId: this.selectedDoctorId(),
      visitDate: this.selectedDate(),
      startTime: slot ? slot.startTime : '09:00',
      durationMinutes: slot ? slot.durationMinutes : 20,
      source: 'PHONE',
      reason: '',
    };
    this.selectedPatient.set(null);
    this.patientSearchQuery.set('');
    this.searchedPatients.set([]);
    this.showBookModal.set(true);
  }

  searchPatients(): void {
    const q = this.patientSearchQuery().trim();
    if (!q) return;
    this.clinicService.searchPatients(q).subscribe({
      next: (res) => this.searchedPatients.set(res.content),
    });
  }

  selectPatient(p: Patient): void {
    this.selectedPatient.set(p);
    this.bookingForm.patientId = p.id;
    this.searchedPatients.set([]);
  }

  saveBooking(): void {
    if (!this.bookingForm.patientId) return;
    this.saving.set(true);
    this.clinicService.bookAppointment(this.bookingForm).subscribe({
      next: () => {
        this.saving.set(false);
        this.showBookModal.set(false);
        this.loadData();
      },
      error: () => this.saving.set(false),
    });
  }

  // Roster Drawer
  openRosterDrawer(): void {
    this.rosterForm = {
      doctorEmployeeId: this.selectedDoctorId(),
      weekday: 0,
      startTime: '09:00',
      endTime: '17:00',
      slotMinutes: 20,
      maxPatientsPerSlot: 1,
    };
    this.showRosterDrawer.set(true);
  }

  saveRoster(): void {
    this.saving.set(true);
    this.clinicService.saveRoster(this.rosterForm).subscribe({
      next: () => {
        this.saving.set(false);
        this.loadData();
      },
      error: () => this.saving.set(false),
    });
  }

  deleteRoster(roster: DoctorRoster): void {
    this.clinicService.deleteRoster(roster.id).subscribe({
      next: () => this.loadData(),
    });
  }
}
