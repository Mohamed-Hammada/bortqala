import { Component, EventEmitter, Input, OnInit, Output, inject } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { I18nService } from '../../../core/i18n.service';
import {
  AdjustmentType,
  ClaimKind,
  ClaimLineType,
  ClaimType,
  CreateProgressClaimRequest,
  ProjectProgressClaim,
  SaveClaimAdjustmentRequest,
  SaveClaimLineRequest,
  UpdateProgressClaimRequest,
} from '../models/claim.models';

@Component({
  selector: 'app-claim-editor-modal',
  standalone: true,
  imports: [CommonModule, FormsModule, DecimalPipe],
  templateUrl: './claim-editor-modal.component.html',
  styleUrl: './claim-editor-modal.component.scss',
})
export class ClaimEditorModalComponent implements OnInit {
  readonly i18n = inject(I18nService);

  @Input() isOpen = false;
  @Input() projectId = '';
  @Input() claim: ProjectProgressClaim | null = null;
  @Input() parties: Array<{ id: string; name: string }> = [];

  @Output() close = new EventEmitter<void>();
  @Output() saveCreate = new EventEmitter<CreateProgressClaimRequest>();
  @Output() saveUpdate = new EventEmitter<{ id: string; req: UpdateProgressClaimRequest }>();

  // Create Mode Form Fields
  claimType: ClaimType = 'OWNER_IPC';
  claimKind: ClaimKind = 'INTERIM';
  partyId: string | null = null;
  periodStartDate = '';
  periodEndDate = '';
  currencyCode = 'EGP';
  notes = '';
  initFromWbs = true;

  // Edit Mode Lines and Adjustments
  activeTab: 'details' | 'measurements' | 'adjustments' = 'details';
  lines: SaveClaimLineRequest[] = [];
  adjustments: SaveClaimAdjustmentRequest[] = [];

  ngOnInit(): void {
    const today = new Date();
    const lastMonth = new Date(today.getFullYear(), today.getMonth() - 1, 1);
    const lastMonthEnd = new Date(today.getFullYear(), today.getMonth(), 0);

    this.periodStartDate = lastMonth.toISOString().slice(0, 10);
    this.periodEndDate = lastMonthEnd.toISOString().slice(0, 10);

    if (this.claim) {
      this.claimType = this.claim.claimType;
      this.claimKind = this.claim.claimKind;
      this.partyId = this.claim.partyId || null;
      this.periodStartDate = this.claim.periodStartDate;
      this.periodEndDate = this.claim.periodEndDate;
      this.currencyCode = this.claim.currencyCode;
      this.notes = this.claim.notes || '';

      if (this.claim.lines) {
        this.lines = this.claim.lines.map((l) => ({
          id: l.id,
          lineType: l.lineType,
          wbsNodeId: l.wbsNodeId,
          itemCode: l.itemCode,
          description: l.description,
          unitOfMeasure: l.unitOfMeasure,
          contractQuantity: l.contractQuantity,
          unitRate: l.unitRate,
          previousQuantity: l.previousQuantity,
          currentQuantity: l.currentQuantity,
          remarks: l.remarks,
          sortOrder: l.sortOrder,
        }));
      }

      if (this.claim.adjustments) {
        this.adjustments = this.claim.adjustments.map((a) => ({
          id: a.id,
          adjustmentType: a.adjustmentType,
          description: a.description,
          percentageRate: a.percentageRate,
          fixedAmount: a.adjustmentAmount,
          isAddition: a.isAddition,
          notes: a.notes,
        }));
      }
    }
  }

  addLine(): void {
    this.lines.push({
      itemCode: `${this.lines.length + 1}.0`,
      description: '',
      unitOfMeasure: 'M3',
      contractQuantity: 100,
      unitRate: 0,
      previousQuantity: 0,
      currentQuantity: 0,
      lineType: 'BOQ_ITEM',
      sortOrder: this.lines.length + 1,
    });
  }

  removeLine(index: number): void {
    this.lines.splice(index, 1);
  }

  addAdjustment(): void {
    this.adjustments.push({
      adjustmentType: 'RETENTION',
      description: 'تأمين أعمال (5%)',
      percentageRate: 5.0,
      fixedAmount: 0,
      isAddition: false,
      notes: null,
    });
  }

  removeAdjustment(index: number): void {
    this.adjustments.splice(index, 1);
  }

  computeCurrentGross(): number {
    return this.lines.reduce((sum, l) => sum + (l.currentQuantity || 0) * (l.unitRate || 0), 0);
  }

  computePreviousGross(): number {
    return this.lines.reduce((sum, l) => sum + (l.previousQuantity || 0) * (l.unitRate || 0), 0);
  }

  computeCumulativeGross(): number {
    return this.computePreviousGross() + this.computeCurrentGross();
  }

  computeTotalDeductions(): number {
    const gross = this.computeCurrentGross();
    return this.adjustments
      .filter((a) => !a.isAddition)
      .reduce((sum, a) => {
        if (a.percentageRate && a.percentageRate > 0) {
          return sum + (gross * a.percentageRate) / 100;
        }
        return sum + (a.fixedAmount || 0);
      }, 0);
  }

  computeTotalAdditions(): number {
    const gross = this.computeCurrentGross();
    return this.adjustments
      .filter((a) => a.isAddition)
      .reduce((sum, a) => {
        if (a.percentageRate && a.percentageRate > 0) {
          return sum + (gross * a.percentageRate) / 100;
        }
        return sum + (a.fixedAmount || 0);
      }, 0);
  }

  computeNetPayable(): number {
    return this.computeCurrentGross() + this.computeTotalAdditions() - this.computeTotalDeductions();
  }

  onSubmit(): void {
    if (!this.claim) {
      const createReq: CreateProgressClaimRequest = {
        claimType: this.claimType,
        claimKind: this.claimKind,
        projectId: this.projectId,
        partyId: this.partyId,
        periodStartDate: this.periodStartDate,
        periodEndDate: this.periodEndDate,
        currencyCode: this.currencyCode,
        notes: this.notes,
        initFromWbs: this.initFromWbs,
      };
      this.saveCreate.emit(createReq);
    } else {
      const updateReq: UpdateProgressClaimRequest = {
        claimKind: this.claimKind,
        partyId: this.partyId,
        periodStartDate: this.periodStartDate,
        periodEndDate: this.periodEndDate,
        currencyCode: this.currencyCode,
        notes: this.notes,
        lines: this.lines,
        adjustments: this.adjustments,
      };
      this.saveUpdate.emit({ id: this.claim.id, req: updateReq });
    }
  }
}
