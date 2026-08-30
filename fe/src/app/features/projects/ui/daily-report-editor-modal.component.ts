import { ChangeDetectionStrategy, Component, EventEmitter, Input, OnInit, Output, inject, signal } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { FormArray, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { I18nService } from '../../../core/i18n.service';
import { NotificationService } from '../../../core/notification.service';
import { apiErrorMessage } from '../../../core/api-error';
import { ModalDialogComponent } from '../../../shared/ui/modal-dialog/modal-dialog.component';
import { DprService } from '../data-access/dpr.service';
import {
  CreateDailyReportRequest,
  CreateEquipmentLogRequest,
  CreateLaborSnapshotRequest,
  CreateMaterialConsumptionRequest,
  CreateWorkProgressLineRequest,
  DailyReportResponse,
  EquipmentSiteStatus,
  LaborSourceType,
  ReportShift,
  UpdateDailyReportRequest,
  WeatherCondition,
} from '../models/dpr.models';
import { WbsNodeResponse } from '../models/project.models';

@Component({
  selector: 'app-daily-report-editor-modal',
  standalone: true,
  imports: [CommonModule, DecimalPipe, ReactiveFormsModule, ModalDialogComponent],
  templateUrl: './daily-report-editor-modal.component.html',
  styleUrl: './daily-report-editor-modal.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DailyReportEditorModalComponent implements OnInit {
  readonly i18n = inject(I18nService);
  private readonly dprService = inject(DprService);
  private readonly notification = inject(NotificationService);

  @Input({ required: true }) projectId!: string;
  @Input() report: DailyReportResponse | null = null;
  @Input() availableWbsNodes: WbsNodeResponse[] = [];
  @Output() closed = new EventEmitter<boolean>();

  readonly submitting = signal(false);
  readonly activeTab = signal<'progress' | 'labor' | 'equipment' | 'materials' | 'general'>('progress');

  readonly form = new FormGroup({
    reportDate: new FormControl<string>('', { nonNullable: true, validators: [Validators.required] }),
    shift: new FormControl<ReportShift>('DAY', { nonNullable: true, validators: [Validators.required] }),
    weatherCondition: new FormControl<WeatherCondition>('SUNNY', { nonNullable: true }),
    temperatureCelsius: new FormControl<number | null>(null, { nonNullable: true }),
    generalNotes: new FormControl('', { nonNullable: true }),
    blockersAndIssues: new FormControl('', { nonNullable: true }),
    safetyObservations: new FormControl('', { nonNullable: true }),
    progressLines: new FormArray<FormGroup>([]),
    laborSnapshots: new FormArray<FormGroup>([]),
    equipmentLogs: new FormArray<FormGroup>([]),
    materialConsumptions: new FormArray<FormGroup>([]),
  });

  get progressLines(): FormArray<FormGroup> {
    return this.form.get('progressLines') as FormArray<FormGroup>;
  }

  get laborSnapshots(): FormArray<FormGroup> {
    return this.form.get('laborSnapshots') as FormArray<FormGroup>;
  }

  get equipmentLogs(): FormArray<FormGroup> {
    return this.form.get('equipmentLogs') as FormArray<FormGroup>;
  }

  get materialConsumptions(): FormArray<FormGroup> {
    return this.form.get('materialConsumptions') as FormArray<FormGroup>;
  }

  ngOnInit(): void {
    if (this.report) {
      this.populateExistingReport(this.report);
    } else {
      this.initNewReport();
    }
  }

  private initNewReport(): void {
    const today = new Date().toISOString().substring(0, 10);
    this.form.patchValue({
      reportDate: today,
      shift: 'DAY',
      weatherCondition: 'SUNNY',
      temperatureCelsius: 28,
      generalNotes: '',
      blockersAndIssues: '',
      safetyObservations: '',
    });

    // Auto-populate active WBS nodes
    if (this.availableWbsNodes.length > 0) {
      for (const node of this.availableWbsNodes) {
        if (node.nodeType === 'BOQ_ITEM' || node.nodeType === 'WORK_PACKAGE') {
          this.addProgressLine(node.id, 0);
        }
      }
    }
  }

  private populateExistingReport(r: DailyReportResponse): void {
    const dateStr = new Date(r.reportDate).toISOString().substring(0, 10);
    this.form.patchValue({
      reportDate: dateStr,
      shift: r.shift,
      weatherCondition: r.weatherCondition || 'SUNNY',
      temperatureCelsius: r.temperatureCelsius,
      generalNotes: r.generalNotes || '',
      blockersAndIssues: r.blockersAndIssues || '',
      safetyObservations: r.safetyObservations || '',
    });

    if (r.progressLines) {
      for (const p of r.progressLines) {
        this.addProgressLine(p.wbsNodeId, p.todayQuantity, p.locationNotes || '', p.remarks || '');
      }
    }

    if (r.laborSnapshots) {
      for (const l of r.laborSnapshots) {
        this.addLaborSnapshot(
          l.tradeCategory,
          l.sourceType,
          l.headcount,
          l.hoursWorked,
          l.activityDescription || '',
          l.wbsNodeId || null
        );
      }
    }

    if (r.equipmentLogs) {
      for (const e of r.equipmentLogs) {
        this.addEquipmentLog(
          e.equipmentType,
          e.equipmentCode || '',
          e.status,
          e.hoursOperated,
          e.hoursIdle,
          e.fuelConsumedLiters || 0,
          e.operatorName || '',
          e.notes || ''
        );
      }
    }

    if (r.materialConsumptions) {
      for (const m of r.materialConsumptions) {
        this.addMaterialConsumption(
          m.materialName,
          m.unitOfMeasure,
          m.quantityUsed,
          m.deliveryNoteNumber || '',
          m.notes || ''
        );
      }
    }
  }

  addProgressLine(wbsNodeId = '', todayQuantity = 0, locationNotes = '', remarks = ''): void {
    const lineGroup = new FormGroup({
      wbsNodeId: new FormControl(wbsNodeId, { nonNullable: true, validators: [Validators.required] }),
      todayQuantity: new FormControl(todayQuantity, { nonNullable: true, validators: [Validators.min(0)] }),
      locationNotes: new FormControl(locationNotes, { nonNullable: true }),
      remarks: new FormControl(remarks, { nonNullable: true }),
    });
    this.progressLines.push(lineGroup);
  }

  removeProgressLine(index: number): void {
    this.progressLines.removeAt(index);
  }

  addLaborSnapshot(tradeCategory = '', sourceType: LaborSourceType = 'DIRECT_EMPLOYEE',
                   headcount = 1, hoursWorked = 8, activityDescription = '', wbsNodeId: string | null = null): void {
    const group = new FormGroup({
      wbsNodeId: new FormControl(wbsNodeId),
      tradeCategory: new FormControl(tradeCategory, { nonNullable: true, validators: [Validators.required] }),
      sourceType: new FormControl<LaborSourceType>(sourceType, { nonNullable: true, validators: [Validators.required] }),
      headcount: new FormControl(headcount, { nonNullable: true, validators: [Validators.required, Validators.min(1)] }),
      hoursWorked: new FormControl(hoursWorked, { nonNullable: true, validators: [Validators.required, Validators.min(0)] }),
      activityDescription: new FormControl(activityDescription, { nonNullable: true }),
    });
    this.laborSnapshots.push(group);
  }

  removeLaborSnapshot(index: number): void {
    this.laborSnapshots.removeAt(index);
  }

  addEquipmentLog(equipmentType = '', equipmentCode = '', status: EquipmentSiteStatus = 'WORKING',
                  hoursOperated = 8, hoursIdle = 0, fuelConsumedLiters = 0, operatorName = '', notes = ''): void {
    const group = new FormGroup({
      equipmentType: new FormControl(equipmentType, { nonNullable: true, validators: [Validators.required] }),
      equipmentCode: new FormControl(equipmentCode, { nonNullable: true }),
      status: new FormControl<EquipmentSiteStatus>(status, { nonNullable: true, validators: [Validators.required] }),
      hoursOperated: new FormControl(hoursOperated, { nonNullable: true, validators: [Validators.min(0)] }),
      hoursIdle: new FormControl(hoursIdle, { nonNullable: true, validators: [Validators.min(0)] }),
      fuelConsumedLiters: new FormControl(fuelConsumedLiters, { nonNullable: true, validators: [Validators.min(0)] }),
      operatorName: new FormControl(operatorName, { nonNullable: true }),
      notes: new FormControl(notes, { nonNullable: true }),
    });
    this.equipmentLogs.push(group);
  }

  removeEquipmentLog(index: number): void {
    this.equipmentLogs.removeAt(index);
  }

  addMaterialConsumption(materialName = '', unitOfMeasure = 'م3', quantityUsed = 0, deliveryNoteNumber = '', notes = ''): void {
    const group = new FormGroup({
      materialName: new FormControl(materialName, { nonNullable: true, validators: [Validators.required] }),
      unitOfMeasure: new FormControl(unitOfMeasure, { nonNullable: true, validators: [Validators.required] }),
      quantityUsed: new FormControl(quantityUsed, { nonNullable: true, validators: [Validators.required, Validators.min(0)] }),
      deliveryNoteNumber: new FormControl(deliveryNoteNumber, { nonNullable: true }),
      notes: new FormControl(notes, { nonNullable: true }),
    });
    this.materialConsumptions.push(group);
  }

  removeMaterialConsumption(index: number): void {
    this.materialConsumptions.removeAt(index);
  }

  getWbsNode(nodeId: string): WbsNodeResponse | undefined {
    return this.availableWbsNodes.find((n) => n.id === nodeId);
  }

  calculateTotalHeadcount(): number {
    return this.laborSnapshots.controls.reduce((sum, g) => sum + (g.get('headcount')?.value || 0), 0);
  }

  calculateTotalManHours(): number {
    return this.laborSnapshots.controls.reduce(
      (sum, g) => sum + (g.get('headcount')?.value || 0) * (g.get('hoursWorked')?.value || 0),
      0
    );
  }

  calculateTotalEquipment(): number {
    return this.equipmentLogs.length;
  }

  async save(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    const formVal = this.form.getRawValue();

    const progressReqs: CreateWorkProgressLineRequest[] = formVal.progressLines.map((p: any) => ({
      wbsNodeId: p.wbsNodeId,
      todayQuantity: p.todayQuantity || 0,
      locationNotes: p.locationNotes || null,
      remarks: p.remarks || null,
    }));

    const laborReqs: CreateLaborSnapshotRequest[] = formVal.laborSnapshots.map((l: any) => ({
      wbsNodeId: l.wbsNodeId || null,
      tradeCategory: l.tradeCategory,
      sourceType: l.sourceType,
      headcount: l.headcount || 1,
      hoursWorked: l.hoursWorked || 8,
      activityDescription: l.activityDescription || null,
    }));

    const equipReqs: CreateEquipmentLogRequest[] = formVal.equipmentLogs.map((e: any) => ({
      equipmentType: e.equipmentType,
      equipmentCode: e.equipmentCode || null,
      status: e.status,
      hoursOperated: e.hoursOperated || 0,
      hoursIdle: e.hoursIdle || 0,
      fuelConsumedLiters: e.fuelConsumedLiters || 0,
      operatorName: e.operatorName || null,
      notes: e.notes || null,
    }));

    const matReqs: CreateMaterialConsumptionRequest[] = formVal.materialConsumptions.map((m: any) => ({
      materialName: m.materialName,
      unitOfMeasure: m.unitOfMeasure,
      quantityUsed: m.quantityUsed || 0,
      deliveryNoteNumber: m.deliveryNoteNumber || null,
      notes: m.notes || null,
    }));

    try {
      if (this.report) {
        const updateReq: UpdateDailyReportRequest = {
          shift: formVal.shift,
          weatherCondition: formVal.weatherCondition,
          temperatureCelsius: formVal.temperatureCelsius,
          generalNotes: formVal.generalNotes || null,
          blockersAndIssues: formVal.blockersAndIssues || null,
          safetyObservations: formVal.safetyObservations || null,
          progressLines: progressReqs,
          laborSnapshots: laborReqs,
          equipmentLogs: equipReqs,
          materialConsumptions: matReqs,
        };
        await this.dprService.updateReport(this.projectId, this.report.id, updateReq).toPromise();
        this.notification.success(this.i18n.t('dpr.updatedSuccess'));
      } else {
        const createReq: CreateDailyReportRequest = {
          reportDate: new Date(formVal.reportDate).getTime(),
          shift: formVal.shift,
          weatherCondition: formVal.weatherCondition,
          temperatureCelsius: formVal.temperatureCelsius,
          generalNotes: formVal.generalNotes || null,
          blockersAndIssues: formVal.blockersAndIssues || null,
          safetyObservations: formVal.safetyObservations || null,
          progressLines: progressReqs,
          laborSnapshots: laborReqs,
          equipmentLogs: equipReqs,
          materialConsumptions: matReqs,
        };
        await this.dprService.createReport(this.projectId, createReq).toPromise();
        this.notification.success(this.i18n.t('dpr.createdSuccess'));
      }

      this.closed.emit(true);
    } catch (err) {
      this.notification.error(apiErrorMessage(err, this.i18n));
    } finally {
      this.submitting.set(false);
    }
  }

  cancel(): void {
    this.closed.emit(false);
  }
}
