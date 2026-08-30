import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { I18nService } from '../../core/i18n.service';
import { NotificationService } from '../../core/notification.service';
import {
  BatchFefoSuggestion,
  DispenseLinePayload,
  DispensePrescriptionPayload,
  NarcoticsRegisterEntry,
  PharmacyDispenseRecord,
  PharmacyItem,
  SavePharmacyItemPayload,
} from './clinic.models';
import { ClinicService } from './clinic.service';

@Component({
  selector: 'app-pharmacy-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './pharmacy.page.html',
  styleUrls: ['./pharmacy.page.scss'],
})
export class PharmacyPageComponent implements OnInit {
  private readonly clinicService = inject(ClinicService);
  private readonly notificationService = inject(NotificationService);
  readonly i18n = inject(I18nService);

  readonly activeTab = signal<'CATALOG' | 'DISPENSE' | 'NARCOTICS'>('CATALOG');
  readonly loading = signal<boolean>(false);
  readonly saving = signal<boolean>(false);

  readonly dosageForms = ['TABLET', 'SYRUP', 'INJECTION', 'CAPSULE', 'OINTMENT', 'DROPS', 'INHALER'] as const;
  readonly controlSchedules = ['SCHEDULE_I', 'SCHEDULE_II', 'SCHEDULE_III', 'SCHEDULE_IV', 'SCHEDULE_V'] as const;

  // Drug Catalog
  readonly pharmacyItems = signal<PharmacyItem[]>([]);
  readonly showDrugModal = signal<boolean>(false);
  drugForm: SavePharmacyItemPayload = {
    itemId: '',
    tradeName: '',
    genericName: '',
    dosageForm: 'TABLET',
    strengthText: '',
    controlled: false,
    controlSchedule: '',
  };

  // Dispensing Desk
  readonly prescriptionSearchId = signal<string>('');
  readonly dispenseLines = signal<DispenseLinePayload[]>([]);
  readonly isControlledPrescription = signal<boolean>(false);
  readonly secondSignerId = signal<string>('');
  readonly dispenseNotes = signal<string>('');
  readonly fefoSuggestions = signal<Map<string, BatchFefoSuggestion[]>>(new Map());

  // Narcotics Register
  readonly narcoticsEntries = signal<NarcoticsRegisterEntry[]>([]);

  ngOnInit(): void {
    this.loadCatalog();
  }

  setTab(tab: 'CATALOG' | 'DISPENSE' | 'NARCOTICS'): void {
    this.activeTab.set(tab);
    if (tab === 'CATALOG') {
      this.loadCatalog();
    } else if (tab === 'NARCOTICS') {
      this.loadNarcotics();
    }
  }

  loadCatalog(): void {
    this.loading.set(true);
    this.clinicService.getAllPharmacyItems().subscribe({
      next: (items) => {
        this.pharmacyItems.set(items);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  loadNarcotics(): void {
    this.loading.set(true);
    this.clinicService.getNarcoticsRegister().subscribe({
      next: (entries) => {
        this.narcoticsEntries.set(entries);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  openAddDrugModal(): void {
    this.drugForm = {
      itemId: 'med-' + Date.now(),
      tradeName: '',
      genericName: '',
      dosageForm: 'TABLET',
      strengthText: '',
      controlled: false,
      controlSchedule: 'SCHEDULE_II',
    };
    this.showDrugModal.set(true);
  }

  saveDrugItem(): void {
    if (!this.drugForm.tradeName) return;
    this.saving.set(true);
    this.clinicService.savePharmacyItem(this.drugForm).subscribe({
      next: () => {
        this.saving.set(false);
        this.showDrugModal.set(false);
        this.loadCatalog();
      },
      error: () => this.saving.set(false),
    });
  }

  loadPrescriptionForDispense(): void {
    const rxId = this.prescriptionSearchId().trim();
    if (!rxId) return;

    // Load available drug items & demo prescription lines for dispense
    const items = this.pharmacyItems();
    if (items.length > 0) {
      const selected = items[0];
      this.dispenseLines.set([
        {
          prescriptionLineId: 'line-1',
          pharmacyItemId: selected.id,
          batchNumber: 'BATCH-001',
          expiryDate: '2027-06-30',
          quantity: 1,
        },
      ]);
      this.isControlledPrescription.set(selected.controlled);
      this.loadFefoForDrug(selected.id);
    }
  }

  loadFefoForDrug(drugId: string): void {
    this.clinicService.getFefoSuggestions(drugId).subscribe({
      next: (suggs) => {
        const map = new Map(this.fefoSuggestions());
        map.set(drugId, suggs);
        this.fefoSuggestions.set(map);
      },
    });
  }

  executeDispense(): void {
    const rxId = this.prescriptionSearchId().trim();
    if (!rxId || this.dispenseLines().length === 0) return;

    const payload: DispensePrescriptionPayload = {
      secondSignerId: this.isControlledPrescription() ? this.secondSignerId() : null,
      notes: this.dispenseNotes(),
      lines: this.dispenseLines(),
    };

    this.saving.set(true);
    this.clinicService.dispensePrescription(rxId, payload).subscribe({
      next: () => {
        this.saving.set(false);
        this.dispenseLines.set([]);
        this.prescriptionSearchId.set('');
        this.notificationService.success(this.i18n.t('clinic.dispensedSuccess'));
      },
      error: () => this.saving.set(false),
    });
  }

  approveDispense(record: PharmacyDispenseRecord): void {
    this.clinicService.approveControlledDispense(record.id).subscribe({
      next: () => {
        this.loadNarcotics();
        this.notificationService.success(this.i18n.t('clinic.dispensedSuccess'));
      },
    });
  }

  exportNarcoticsMOH(): void {
    const entries = this.narcoticsEntries();
    if (entries.length === 0) return;

    let csvContent = 'data:text/csv;charset=utf-8,\uFEFF';
    csvContent += 'Trade Name,Patient MRN,Patient Name,Prescriber,Dispenser,Second Signer,Batch,Quantity,Date\n';

    entries.forEach((e) => {
      csvContent += `"${e.tradeName}","${e.patientMrn}","${e.patientName}","${e.prescriberDoctorName}","${e.dispenserUserName}","${e.secondSignerName}","${e.batchNumber || ''}",${e.quantity},"${new Date(e.signedAt).toISOString()}"\n`;
    });

    const encodedUri = encodeURI(csvContent);
    const link = document.createElement('a');
    link.setAttribute('href', encodedUri);
    link.setAttribute('download', `MOH_Narcotics_Register_${new Date().toISOString().substring(0, 10)}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }
}
