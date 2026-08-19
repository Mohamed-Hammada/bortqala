import { Component, EventEmitter, Input, Output, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { I18nService } from '../../../core/i18n.service';
import {
  ProjectTender,
  CreateTenderRequest,
  UpdateTenderRequest,
  TenderType
} from '../models/tender.models';

@Component({
  selector: 'app-tender-editor-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './tender-editor-modal.component.html',
  styleUrls: ['./tender-editor-modal.component.scss']
})
export class TenderEditorModalComponent implements OnInit {
  readonly i18n = inject(I18nService);

  @Input() tender: ProjectTender | null = null;
  @Input() projectId?: string | null = null;
  @Input() isSaving = false;

  @Output() saveTender = new EventEmitter<CreateTenderRequest | UpdateTenderRequest>();
  @Output() close = new EventEmitter<void>();

  title = '';
  titleEn = '';
  tenderType: TenderType = 'EXTERNAL';
  deadlineDateStr = '';
  estimatedValue = 0;
  currencyCode = 'EGP';
  technicalWeightPercent = 70;
  financialWeightPercent = 30;
  bidBondRequired = false;
  bidBondAmount = 0;
  bidBondValidityDays = 90;
  notes = '';

  ngOnInit(): void {
    if (this.tender) {
      this.title = this.tender.title;
      this.titleEn = this.tender.titleEn || '';
      this.tenderType = this.tender.tenderType;
      this.estimatedValue = this.tender.estimatedValue;
      this.currencyCode = this.tender.currencyCode;
      this.technicalWeightPercent = this.tender.technicalWeightPercent;
      this.financialWeightPercent = this.tender.financialWeightPercent;
      this.bidBondRequired = this.tender.bidBondRequired;
      this.bidBondAmount = this.tender.bidBondAmount || 0;
      this.bidBondValidityDays = this.tender.bidBondValidityDays || 90;
      this.notes = this.tender.notes || '';
      if (this.tender.submissionDeadline) {
        this.deadlineDateStr = new Date(this.tender.submissionDeadline).toISOString().substring(0, 10);
      }
    } else {
      const d = new Date();
      d.setDate(d.getDate() + 30);
      this.deadlineDateStr = d.toISOString().substring(0, 10);
    }
  }

  onWeightsChange(source: 'tech' | 'fin'): void {
    if (source === 'tech') {
      this.financialWeightPercent = Math.max(0, 100 - this.technicalWeightPercent);
    } else {
      this.technicalWeightPercent = Math.max(0, 100 - this.financialWeightPercent);
    }
  }

  onSubmit(): void {
    if (!this.title.trim() || !this.deadlineDateStr) return;

    const deadlineEpoch = new Date(this.deadlineDateStr).getTime();

    const payload: CreateTenderRequest = {
      title: this.title.trim(),
      titleEn: this.titleEn.trim() || null,
      tenderType: this.tenderType,
      projectId: this.projectId || (this.tender ? this.tender.projectId : null),
      submissionDeadline: deadlineEpoch,
      estimatedValue: this.estimatedValue,
      currencyCode: this.currencyCode,
      technicalWeightPercent: this.technicalWeightPercent,
      financialWeightPercent: this.financialWeightPercent,
      bidBondRequired: this.bidBondRequired,
      bidBondAmount: this.bidBondRequired ? this.bidBondAmount : 0,
      bidBondValidityDays: this.bidBondRequired ? this.bidBondValidityDays : null,
      notes: this.notes.trim() || null
    };

    this.saveTender.emit(payload);
  }
}
