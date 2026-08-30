import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { I18nService } from '../../core/i18n.service';
import { NotificationService } from '../../core/notification.service';
import {
  AddDentalPlanItemPayload,
  CreateDentalPlanPayload,
  DentalRecord,
  DentalTreatmentPlan,
  DentalTreatmentPlanItem,
  ExamAnswer,
  ExamTemplate,
  Patient,
  PatientOdontogram,
  RecordToothConditionPayload,
  SaveExamTemplatePayload,
  SubmitExamAnswerPayload,
  ToothCondition,
  ToothStatusSummary,
  ToothSurface,
} from './clinic.models';
import { ClinicService } from './clinic.service';

@Component({
  selector: 'app-dental-charting-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './dental-charting.page.html',
  styleUrls: ['./dental-charting.page.scss'],
})
export class DentalChartingPageComponent implements OnInit {
  private readonly clinicService = inject(ClinicService);
  private readonly notificationService = inject(NotificationService);
  readonly i18n = inject(I18nService);

  readonly activeTab = signal<'ODONTOGRAM' | 'PLANS' | 'TEMPLATES'>('ODONTOGRAM');
  readonly loading = signal<boolean>(false);
  readonly saving = signal<boolean>(false);

  // Patient Context
  readonly patients = signal<Patient[]>([]);
  readonly selectedPatientId = signal<string>('');
  readonly odontogram = signal<PatientOdontogram | null>(null);
  readonly selectedTooth = signal<ToothStatusSummary | null>(null);

  // Quadrants for Adult 32 Teeth
  readonly upperRightTeeth = [18, 17, 16, 15, 14, 13, 12, 11];
  readonly upperLeftTeeth = [21, 22, 23, 24, 25, 26, 27, 28];
  readonly lowerLeftTeeth = [31, 32, 33, 34, 35, 36, 37, 38];
  readonly lowerRightTeeth = [48, 47, 46, 45, 44, 43, 42, 41];

  // Treatment Plans
  readonly treatmentPlans = signal<DentalTreatmentPlan[]>([]);
  readonly selectedPlan = signal<DentalTreatmentPlan | null>(null);

  // Exam Templates & Dynamic Form
  readonly examTemplates = signal<ExamTemplate[]>([]);
  readonly selectedTemplate = signal<ExamTemplate | null>(null);
  readonly templateFields = signal<{ key: string; label: string; type: string; options?: string[] }[]>([]);
  readonly dynamicAnswers = signal<Record<string, any>>({});
  readonly examVisitId = signal<string>('');

  // Modals
  readonly showToothModal = signal<boolean>(false);
  toothForm: RecordToothConditionPayload = {
    toothNumber: 11,
    condition: 'CARIES',
    surface: 'OCCLUSAL',
    notes: '',
  };

  readonly showPlanModal = signal<boolean>(false);
  planForm: CreateDentalPlanPayload = {
    title: '',
  };

  readonly showPlanItemModal = signal<boolean>(false);
  planItemForm: AddDentalPlanItemPayload = {
    toothNumber: 11,
    procedureText: '',
    estimatedCost: 200,
  };

  readonly showTemplateModal = signal<boolean>(false);
  templateForm: SaveExamTemplatePayload = {
    specialty: 'DENTAL',
    name: '',
    schemaJson: '{"fields":[{"key":"complaint","label":"Chief Complaint","type":"text"}]}',
  };

  ngOnInit(): void {
    this.loadPatients();
    this.loadExamTemplates();
  }

  setTab(tab: 'ODONTOGRAM' | 'PLANS' | 'TEMPLATES'): void {
    this.activeTab.set(tab);
    if (tab === 'ODONTOGRAM' && this.selectedPatientId()) {
      this.loadOdontogram(this.selectedPatientId());
    } else if (tab === 'PLANS' && this.selectedPatientId()) {
      this.loadTreatmentPlans(this.selectedPatientId());
    } else if (tab === 'TEMPLATES') {
      this.loadExamTemplates();
    }
  }

  loadPatients(): void {
    this.loading.set(true);
    this.clinicService.searchPatients('', 0, 50).subscribe({
      next: (res) => {
        this.patients.set(res.content);
        if (res.content.length > 0) {
          this.selectPatient(res.content[0].id);
        }
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  selectPatient(patientId: string): void {
    this.selectedPatientId.set(patientId);
    this.loadOdontogram(patientId);
    this.loadTreatmentPlans(patientId);
  }

  loadOdontogram(patientId: string): void {
    this.clinicService.getPatientOdontogram(patientId).subscribe({
      next: (data) => this.odontogram.set(data),
    });
  }

  loadTreatmentPlans(patientId: string): void {
    this.clinicService.getDentalPlans(patientId).subscribe({
      next: (data) => {
        this.treatmentPlans.set(data);
        if (data.length > 0) {
          this.selectedPlan.set(data[0]);
        }
      },
    });
  }

  loadExamTemplates(): void {
    this.clinicService.getExamTemplates().subscribe({
      next: (data) => {
        this.examTemplates.set(data);
        if (data.length > 0 && !this.selectedTemplate()) {
          this.selectTemplate(data[0]);
        }
      },
    });
  }

  getToothSummary(toothNumber: number): ToothStatusSummary {
    const defaultSummary: ToothStatusSummary = {
      toothNumber,
      condition: 'HEALTHY',
      surface: null,
      notes: null,
      notedOn: 0,
    };
    return this.odontogram()?.teeth.find((t) => t.toothNumber === toothNumber) ?? defaultSummary;
  }

  openToothModal(toothNumber: number): void {
    const summary = this.getToothSummary(toothNumber);
    this.selectedTooth.set(summary);
    this.toothForm = {
      toothNumber,
      condition: summary.condition,
      surface: summary.surface || 'OCCLUSAL',
      notes: summary.notes || '',
    };
    this.showToothModal.set(true);
  }

  submitToothCondition(): void {
    if (!this.selectedPatientId()) return;
    this.saving.set(true);
    this.clinicService.recordToothCondition(this.selectedPatientId(), this.toothForm).subscribe({
      next: () => {
        this.saving.set(false);
        this.showToothModal.set(false);
        this.loadOdontogram(this.selectedPatientId());
        this.notificationService.success(this.i18n.t('clinic.resultSuccess'));
      },
      error: () => this.saving.set(false),
    });
  }

  // Treatment Plans Actions
  openCreatePlanModal(): void {
    this.planForm = { title: '' };
    this.showPlanModal.set(true);
  }

  submitCreatePlan(): void {
    if (!this.selectedPatientId() || !this.planForm.title) return;
    this.saving.set(true);
    this.clinicService.createDentalPlan(this.selectedPatientId(), this.planForm).subscribe({
      next: () => {
        this.saving.set(false);
        this.showPlanModal.set(false);
        this.loadTreatmentPlans(this.selectedPatientId());
        this.notificationService.success(this.i18n.t('clinic.resultSuccess'));
      },
      error: () => this.saving.set(false),
    });
  }

  openAddPlanItemModal(plan: DentalTreatmentPlan): void {
    this.selectedPlan.set(plan);
    this.planItemForm = {
      toothNumber: 11,
      procedureText: '',
      estimatedCost: 250,
    };
    this.showPlanItemModal.set(true);
  }

  submitAddPlanItem(): void {
    if (!this.selectedPlan() || !this.planItemForm.procedureText) return;
    this.saving.set(true);
    this.clinicService.addDentalPlanItem(this.selectedPlan()!.id, this.planItemForm).subscribe({
      next: () => {
        this.saving.set(false);
        this.showPlanItemModal.set(false);
        this.loadTreatmentPlans(this.selectedPatientId());
        this.notificationService.success(this.i18n.t('clinic.resultSuccess'));
      },
      error: () => this.saving.set(false),
    });
  }

  markPlanItemDone(item: DentalTreatmentPlanItem): void {
    this.clinicService.markDentalPlanItemDone(item.id).subscribe({
      next: () => {
        this.loadTreatmentPlans(this.selectedPatientId());
        this.notificationService.success(this.i18n.t('clinic.resultSuccess'));
      },
    });
  }

  // Specialty Exam Templates Actions
  selectTemplate(template: ExamTemplate): void {
    this.selectedTemplate.set(template);
    try {
      const parsed = JSON.parse(template.schemaJson);
      this.templateFields.set(parsed.fields || []);
    } catch {
      this.templateFields.set([]);
    }
    this.dynamicAnswers.set({});
  }

  openCreateTemplateModal(): void {
    this.templateForm = {
      specialty: 'DENTAL',
      name: '',
      schemaJson: JSON.stringify({
        fields: [
          { key: 'occlusion', label: 'Occlusion Class', type: 'select', options: ['Class I', 'Class II', 'Class III'] },
          { key: 'periodontalPockets', label: 'Periodontal Pockets (mm)', type: 'number' },
          { key: 'gingivalBleeding', label: 'Gingival Bleeding on Probing', type: 'checkbox' },
          { key: 'findings', label: 'Clinical Findings', type: 'text' },
        ],
      }, null, 2),
    };
    this.showTemplateModal.set(true);
  }

  submitCreateTemplate(): void {
    if (!this.templateForm.name || !this.templateForm.schemaJson) return;
    this.saving.set(true);
    this.clinicService.saveExamTemplate(this.templateForm).subscribe({
      next: (created) => {
        this.saving.set(false);
        this.showTemplateModal.set(false);
        this.loadExamTemplates();
        this.selectTemplate(created);
        this.notificationService.success(this.i18n.t('clinic.resultSuccess'));
      },
      error: () => this.saving.set(false),
    });
  }

  submitExam(): void {
    if (!this.selectedTemplate()) return;
    const payload: SubmitExamAnswerPayload = {
      visitId: this.examVisitId() || 'VIS-DEFAULT',
      templateId: this.selectedTemplate()!.id,
      answersJson: JSON.stringify(this.dynamicAnswers()),
      recordedBy: 'Attending Doctor',
    };
    this.saving.set(true);
    this.clinicService.submitExamAnswers(payload).subscribe({
      next: () => {
        this.saving.set(false);
        this.notificationService.success(this.i18n.t('clinic.resultSuccess'));
      },
      error: () => this.saving.set(false),
    });
  }
}
