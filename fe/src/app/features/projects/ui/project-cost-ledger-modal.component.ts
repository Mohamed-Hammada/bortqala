import { Component, EventEmitter, Input, Output, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { I18nService } from '../../../core/i18n.service';
import { RecordCostLedgerEntryRequest, CostCategory, CostLedgerEntryType } from '../models/cost-control.models';
import { WbsNodeResponse } from '../models/project.models';

@Component({
  selector: 'app-project-cost-ledger-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './project-cost-ledger-modal.component.html',
  styleUrls: ['./project-cost-ledger-modal.component.scss']
})
export class ProjectCostLedgerModalComponent {
  readonly i18n = inject(I18nService);

  @Input({ required: true }) projectId!: string;
  @Input() wbsNodes: WbsNodeResponse[] = [];
  @Output() close = new EventEmitter<void>();
  @Output() save = new EventEmitter<RecordCostLedgerEntryRequest>();

  readonly wbsNodeId = signal<string>('');
  readonly costCategory = signal<CostCategory>('MATERIAL');
  readonly entryType = signal<CostLedgerEntryType>('ACTUAL');
  readonly sourceModule = signal<string>('MANUAL_JOURNAL');
  readonly sourceDocumentNumber = signal<string>('');
  readonly entryDate = signal<string>(new Date().toISOString().substring(0, 10));
  readonly description = signal<string>('');
  readonly quantity = signal<number>(1);
  readonly unitRate = signal<number>(0);
  readonly amount = signal<number>(0);
  readonly submitting = signal<boolean>(false);

  readonly categories: CostCategory[] = ['LABOR', 'EQUIPMENT', 'MATERIAL', 'SUBCONTRACTOR', 'OVERHEAD', 'CONTINGENCY'];
  readonly entryTypes: CostLedgerEntryType[] = ['COMMITTED', 'ACTUAL', 'REVENUE'];

  getCategoryLabel(cat: CostCategory): string {
    switch (cat) {
      case 'LABOR': return this.i18n.t('costControl.catLabor');
      case 'EQUIPMENT': return this.i18n.t('costControl.catEquipment');
      case 'MATERIAL': return this.i18n.t('costControl.catMaterial');
      case 'SUBCONTRACTOR': return this.i18n.t('costControl.catSubcontractor');
      case 'OVERHEAD': return this.i18n.t('costControl.catOverhead');
      case 'CONTINGENCY': return this.i18n.t('costControl.catContingency');
    }
  }

  getEntryTypeLabel(type: CostLedgerEntryType): string {
    switch (type) {
      case 'COMMITTED': return this.i18n.t('costControl.typeCommitted');
      case 'ACTUAL': return this.i18n.t('costControl.typeActual');
      case 'REVENUE': return this.i18n.t('costControl.typeRevenue');
      default: return type;
    }
  }

  onQtyOrRateChange(): void {
    const total = (this.quantity() || 0) * (this.unitRate() || 0);
    if (total > 0) {
      this.amount.set(total);
    }
  }

  onSave(): void {
    if (!this.description().trim() || this.amount() <= 0) return;

    this.submitting.set(true);
    const req: RecordCostLedgerEntryRequest = {
      wbsNodeId: this.wbsNodeId() || undefined,
      costCategory: this.costCategory(),
      entryType: this.entryType(),
      sourceModule: this.sourceModule(),
      sourceDocumentNumber: this.sourceDocumentNumber().trim() || undefined,
      entryDate: this.entryDate(),
      description: this.description().trim(),
      quantity: this.quantity() || undefined,
      unitRate: this.unitRate() || undefined,
      amount: this.amount()
    };

    this.save.emit(req);
  }
}
