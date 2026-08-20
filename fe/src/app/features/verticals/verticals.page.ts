import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { I18nService } from '../../core/i18n.service';
import { VerticalsService } from './verticals.service';
import {
  CustomsDeclaration,
  StudentEnrollment,
  ThreePlContract,
  TourismBooking,
  VerticalsSummary,
} from './verticals.models';

type VerticalTab = 'school' | 'tourism' | 'customs' | '3pl';

@Component({
  selector: 'app-verticals-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './verticals.page.html',
  styleUrl: './verticals.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class VerticalsPage implements OnInit {
  readonly i18n = inject(I18nService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly verticalsService = inject(VerticalsService);

  readonly activeTab = signal<VerticalTab>('school');
  readonly summary = signal<VerticalsSummary | null>(null);
  readonly students = signal<StudentEnrollment[]>([]);
  readonly bookings = signal<TourismBooking[]>([]);
  readonly declarations = signal<CustomsDeclaration[]>([]);
  readonly contracts = signal<ThreePlContract[]>([]);

  readonly loading = signal(false);
  readonly submitting = signal(false);
  readonly message = signal<string | null>(null);
  readonly error = signal<string | null>(null);

  // Modals
  readonly showStudentModal = signal(false);
  readonly showBookingModal = signal(false);
  readonly showDeclarationModal = signal(false);
  readonly showContractModal = signal(false);

  // School Form
  studentCode = '';
  studentName = '';
  gradeLevel = '';
  academicYear = '2026/2027';
  guardianName = '';
  guardianPhone = '';
  tuitionFee = 25000;
  transportFee = 4000;
  booksFee = 2500;
  installmentsCount = 3;

  // Tourism Form
  bookingCode = '';
  customerName = '';
  packageName = '';
  destination = '';
  travelDateStr = '2026-09-01';
  returnDateStr = '2026-09-06';
  travelersCount = 2;
  sellingPrice = 35000;
  hotelCost = 15000;
  flightCost = 9000;
  excursionCost = 3000;

  // Customs Form
  fileNumber = '';
  importerName = '';
  portOfEntry = 'Alexandria Port';
  billOfLading = '';
  customsCert = '';
  dutyAmount = 85000;
  portHandling = 12000;
  clearanceFee = 8000;

  // 3PL Form
  contractCode = '';
  clientName = '';
  warehouseName = 'Cairo Gateway Cold Hub';
  palletCapacity = 250;
  ratePerPallet = 300;
  handlingIn = 20;
  handlingOut = 20;

  ngOnInit(): void {
    const requested = this.route.snapshot.queryParamMap.get('tab');
    if (requested && ['school', 'tourism', 'customs', '3pl'].includes(requested)) {
      this.activeTab.set(requested as VerticalTab);
    }
    this.loadData();
  }

  setTab(tab: VerticalTab): void {
    this.activeTab.set(tab);
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { tab },
      queryParamsHandling: 'merge',
      replaceUrl: true,
    });
  }

  loadData(): void {
    this.loading.set(true);
    this.error.set(null);

    this.verticalsService.getSummary().subscribe({
      next: (sum) => this.summary.set(sum),
      error: () => {},
    });

    this.verticalsService.listStudents().subscribe({
      next: (list) => {
        this.students.set(list);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err?.message || 'Failed to load vertical data');
        this.loading.set(false);
      },
    });

    this.verticalsService.listTourismBookings().subscribe({
      next: (list) => this.bookings.set(list),
      error: () => {},
    });

    this.verticalsService.listCustomsDeclarations().subscribe({
      next: (list) => this.declarations.set(list),
      error: () => {},
    });

    this.verticalsService.list3plContracts().subscribe({
      next: (list) => this.contracts.set(list),
      error: () => {},
    });
  }

  submitStudent(): void {
    if (!this.studentCode.trim() || !this.studentName.trim()) return;

    this.submitting.set(true);
    this.verticalsService.registerStudent({
      studentCode: this.studentCode.trim(),
      studentName: this.studentName.trim(),
      gradeLevel: this.gradeLevel.trim() || 'Grade 1',
      academicYear: this.academicYear.trim(),
      guardianName: this.guardianName.trim() || this.studentName.trim(),
      guardianPhone: this.guardianPhone.trim() || undefined,
      totalTuitionFee: Number(this.tuitionFee),
      transportFee: Number(this.transportFee),
      booksFee: Number(this.booksFee),
      installmentsCount: Number(this.installmentsCount),
    }).subscribe({
      next: (created) => {
        this.students.update((list) => [created, ...list]);
        this.message.set(this.i18n.t('verticals.studentRegistered'));
        this.showStudentModal.set(false);
        this.submitting.set(false);
        this.resetStudentForm();
      },
      error: (err) => {
        this.error.set(err?.message || 'Failed to register student');
        this.submitting.set(false);
      },
    });
  }

  submitBooking(): void {
    if (!this.bookingCode.trim() || !this.packageName.trim()) return;

    this.submitting.set(true);
    this.verticalsService.createTourismBooking({
      bookingCode: this.bookingCode.trim(),
      customerName: this.customerName.trim() || 'Guest',
      packageName: this.packageName.trim(),
      destination: this.destination.trim() || 'Hurghada',
      travelDate: new Date(this.travelDateStr).getTime(),
      returnDate: new Date(this.returnDateStr).getTime(),
      travelersCount: Number(this.travelersCount),
      sellingPrice: Number(this.sellingPrice),
      hotelCost: Number(this.hotelCost),
      flightCost: Number(this.flightCost),
      excursionCost: Number(this.excursionCost),
    }).subscribe({
      next: (created) => {
        this.bookings.update((list) => [created, ...list]);
        this.message.set(this.i18n.t('verticals.bookingCreated'));
        this.showBookingModal.set(false);
        this.submitting.set(false);
        this.resetBookingForm();
      },
      error: (err) => {
        this.error.set(err?.message || 'Failed to create tour booking');
        this.submitting.set(false);
      },
    });
  }

  submitDeclaration(): void {
    if (!this.fileNumber.trim() || !this.importerName.trim()) return;

    this.submitting.set(true);
    this.verticalsService.openCustomsDeclaration({
      fileNumber: this.fileNumber.trim(),
      importerName: this.importerName.trim(),
      portOfEntry: this.portOfEntry.trim(),
      billOfLadingNumber: this.billOfLading.trim() || 'BL-DEFAULT',
      customsCertificateNumber: this.customsCert.trim() || undefined,
      dutyDisbursementAmount: Number(this.dutyAmount),
      portHandlingAmount: Number(this.portHandling),
      clearanceServiceFee: Number(this.clearanceFee),
    }).subscribe({
      next: (created) => {
        this.declarations.update((list) => [created, ...list]);
        this.message.set(this.i18n.t('verticals.declarationCreated'));
        this.showDeclarationModal.set(false);
        this.submitting.set(false);
        this.resetDeclarationForm();
      },
      error: (err) => {
        this.error.set(err?.message || 'Failed to open customs file');
        this.submitting.set(false);
      },
    });
  }

  submitContract(): void {
    if (!this.contractCode.trim() || !this.clientName.trim()) return;

    this.submitting.set(true);
    this.verticalsService.create3plContract({
      contractCode: this.contractCode.trim(),
      clientName: this.clientName.trim(),
      warehouseName: this.warehouseName.trim(),
      palletCapacity: Number(this.palletCapacity),
      ratePerPalletMonthly: Number(this.ratePerPallet),
      handlingInRatePerPallet: Number(this.handlingIn),
      handlingOutRatePerPallet: Number(this.handlingOut),
    }).subscribe({
      next: (created) => {
        this.contracts.update((list) => [created, ...list]);
        this.message.set(this.i18n.t('verticals.contractCreated'));
        this.showContractModal.set(false);
        this.submitting.set(false);
        this.resetContractForm();
      },
      error: (err) => {
        this.error.set(err?.message || 'Failed to create 3PL contract');
        this.submitting.set(false);
      },
    });
  }

  private resetStudentForm(): void {
    this.studentCode = '';
    this.studentName = '';
    this.guardianName = '';
    this.guardianPhone = '';
  }

  private resetBookingForm(): void {
    this.bookingCode = '';
    this.customerName = '';
    this.packageName = '';
    this.destination = '';
  }

  private resetDeclarationForm(): void {
    this.fileNumber = '';
    this.importerName = '';
    this.billOfLading = '';
    this.customsCert = '';
  }

  private resetContractForm(): void {
    this.contractCode = '';
    this.clientName = '';
  }
}
