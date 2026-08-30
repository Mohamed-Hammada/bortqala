import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { I18nService } from '../../core/i18n.service';
import { NotificationCenterService } from '../../core/notification-center/notification-center.service';
import {
  AddAllergyPayload,
  AddConditionPayload,
  ConsentForm,
  PatientAllergy,
  PatientChart,
  PatientCondition,
  PatientDocument,
  RecordVitalsPayload,
  SignConsentPayload,
  UploadDocumentPayload,
  VisitVitals,
  LabOrder,
} from './clinic.models';
import { ClinicService } from './clinic.service';

type ChartTab = 'history' | 'vitals' | 'allergies' | 'documents' | 'consents';

@Component({
  selector: 'app-patient-chart',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './patient-chart.page.html',
  styleUrls: ['./patient-chart.page.scss'],
})
export class PatientChartPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly clinicService = inject(ClinicService);
  private readonly notificationCenter = inject(NotificationCenterService);
  readonly i18n = inject(I18nService);

  readonly patientId = signal<string>('');
  readonly chart = signal<PatientChart | null>(null);
  readonly validatedLabOrders = signal<LabOrder[]>([]);
  readonly loading = signal<boolean>(false);
  readonly activeTab = signal<ChartTab>('history');

  // Modals state
  readonly showVitalsModal = signal<boolean>(false);
  readonly showAllergyModal = signal<boolean>(false);
  readonly showConditionModal = signal<boolean>(false);
  readonly showDocModal = signal<boolean>(false);
  readonly showConsentModal = signal<boolean>(false);
  readonly saving = signal<boolean>(false);

  // Forms data
  vitalsForm: RecordVitalsPayload & { visitId?: string } = {};
  allergyForm: AddAllergyPayload = { substance: '', severity: 'MODERATE' };
  conditionForm: AddConditionPayload = { label: '', chronic: true, status: 'ACTIVE' };
  docForm: UploadDocumentPayload = { documentKind: 'REPORT', fileName: '' };
  consentForm: SignConsentPayload = {
    templateKey: 'GENERAL_TREATMENT',
    title: '',
    bodyText: '',
    signedByName: '',
    signedByRelation: 'SELF',
  };

  readonly computedBmi = computed(() => {
    const w = this.vitalsForm.weightKg;
    const h = this.vitalsForm.heightCm;
    if (!w || !h || h <= 0) return null;
    const hM = h / 100;
    const bmi = w / (hM * hM);
    return Math.round(bmi * 10) / 10;
  });

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.patientId.set(id);
      this.loadChart();
    }
  }

  loadChart(): void {
    const id = this.patientId();
    if (!id) return;
    this.loading.set(true);
    this.clinicService.getPatientChart(id).subscribe({
      next: (data) => {
        this.chart.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
      },
    });
    this.clinicService.getOrdersByPatient(id, true).subscribe({
      next: (orders) => this.validatedLabOrders.set(orders),
    });
  }

  setTab(tab: ChartTab): void {
    this.activeTab.set(tab);
  }

  // Vitals modal
  openVitalsModal(visitId?: string): void {
    this.vitalsForm = {
      visitId: visitId || (this.chart()?.recentVisits[0]?.id ?? ''),
      systolicBp: 120,
      diastolicBp: 80,
      pulse: 75,
      tempC: 37.0,
      spo2: 98,
      weightKg: 70.0,
      heightCm: 170.0,
      notes: '',
    };
    this.showVitalsModal.set(true);
  }

  saveVitals(): void {
    const visitId = this.vitalsForm.visitId || this.chart()?.recentVisits[0]?.id;
    if (!visitId) return;

    this.saving.set(true);
    this.clinicService.recordVitals(visitId, this.vitalsForm).subscribe({
      next: () => {
        this.saving.set(false);
        this.showVitalsModal.set(false);
        this.loadChart();
      },
      error: () => this.saving.set(false),
    });
  }

  // Allergy modal
  openAllergyModal(): void {
    this.allergyForm = { substance: '', severity: 'MODERATE', reactionNotes: '' };
    this.showAllergyModal.set(true);
  }

  saveAllergy(): void {
    if (!this.allergyForm.substance.trim()) return;
    this.saving.set(true);
    this.clinicService.addAllergy(this.patientId(), this.allergyForm).subscribe({
      next: () => {
        this.saving.set(false);
        this.showAllergyModal.set(false);
        this.loadChart();
      },
      error: () => this.saving.set(false),
    });
  }

  deleteAllergy(allergy: PatientAllergy): void {
    this.clinicService.deleteAllergy(allergy.id).subscribe({
      next: () => this.loadChart(),
    });
  }

  // Condition modal
  openConditionModal(): void {
    this.conditionForm = { label: '', icdCode: '', chronic: true, status: 'ACTIVE', notes: '' };
    this.showConditionModal.set(true);
  }

  saveCondition(): void {
    if (!this.conditionForm.label.trim()) return;
    this.saving.set(true);
    this.clinicService.addCondition(this.patientId(), this.conditionForm).subscribe({
      next: () => {
        this.saving.set(false);
        this.showConditionModal.set(false);
        this.loadChart();
      },
      error: () => this.saving.set(false),
    });
  }

  deleteCondition(condition: PatientCondition): void {
    this.clinicService.deleteCondition(condition.id).subscribe({
      next: () => this.loadChart(),
    });
  }

  // Document modal
  openDocModal(): void {
    this.docForm = {
      documentKind: 'REPORT',
      fileName: '',
      notes: '',
    };
    this.showDocModal.set(true);
  }

  saveDocument(): void {
    if (!this.docForm.fileName.trim()) return;
    this.saving.set(true);
    this.clinicService.uploadDocument(this.patientId(), this.docForm).subscribe({
      next: () => {
        this.saving.set(false);
        this.showDocModal.set(false);
        this.loadChart();
      },
      error: () => this.saving.set(false),
    });
  }

  deleteDocument(doc: PatientDocument): void {
    this.clinicService.deleteDocument(doc.id).subscribe({
      next: () => this.loadChart(),
    });
  }

  // Consent modal
  openConsentModal(): void {
    const patientName = this.chart()?.patient.fullName || '';
    this.consentForm = {
      templateKey: 'GENERAL_TREATMENT',
      title: this.i18n.t('clinic.consentGeneral'),
      bodyText: this.getConsentTemplateBody('GENERAL_TREATMENT', patientName),
      signedByName: patientName,
      signedByRelation: 'SELF',
    };
    this.showConsentModal.set(true);
  }

  onConsentTemplateChange(): void {
    const patientName = this.chart()?.patient.fullName || '';
    const key = this.consentForm.templateKey;
    if (key === 'PROCEDURE_CONSENT') {
      this.consentForm.title = this.i18n.t('clinic.consentProcedure');
    } else if (key === 'TELEHEALTH_CONSENT') {
      this.consentForm.title = this.i18n.t('clinic.consentTelehealth');
    } else {
      this.consentForm.title = this.i18n.t('clinic.consentGeneral');
    }
    this.consentForm.bodyText = this.getConsentTemplateBody(key, patientName);
  }

  private getConsentTemplateBody(key: string, name: string): string {
    if (key === 'PROCEDURE_CONSENT') {
      return `I, ${name}, hereby give informed consent for the recommended minor procedure. The nature, purpose, and potential risks have been explained to me.`;
    }
    if (key === 'TELEHEALTH_CONSENT') {
      return `I, ${name}, consent to receive medical consultation and advice via electronic communication and telehealth services.`;
    }
    return `I, ${name}, authorize the clinic and its medical practitioners to perform general medical examination, diagnosis, and necessary outpatient treatments.`;
  }

  saveConsent(): void {
    if (!this.consentForm.signedByName.trim()) return;
    this.saving.set(true);
    this.clinicService.signConsent(this.patientId(), this.consentForm).subscribe({
      next: () => {
        this.saving.set(false);
        this.showConsentModal.set(false);
        this.loadChart();
      },
      error: () => this.saving.set(false),
    });
  }

  printConsent(consent: ConsentForm): void {
    window.print();
  }
}
