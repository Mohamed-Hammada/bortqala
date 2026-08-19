import { Component, EventEmitter, Input, Output, inject, signal } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { I18nService } from '../../../core/i18n.service';
import { CreateBudgetVersionRequest, SaveBudgetLineRequest, CostCategory } from '../models/cost-control.models';

@Component({
  selector: 'app-project-budget-modal',
  standalone: true,
  imports: [CommonModule, FormsModule, DecimalPipe],
  templateUrl: './project-budget-modal.component.html',
  styleUrls: ['./project-budget-modal.component.scss']
})
export class ProjectBudgetModalComponent {
  readonly i18n = inject(I18nService);

  @Input({ required: true }) projectId!: string;
  @Output() close = new EventEmitter<void>();
  @Output() save = new EventEmitter<CreateBudgetVersionRequest>();

  readonly versionName = signal<string>('Budget Revision');
  readonly notes = signal<string>('');
  readonly initFromWbs = signal<boolean>(true);
  readonly submitting = signal<boolean>(false);

  readonly manualLines = signal<SaveBudgetLineRequest[]>([
    {
      description: '',
      costCategory: 'MATERIAL',
      budgetQuantity: 1,
      unitOfMeasure: 'PCS',
      budgetUnitRate: 0,
      sortOrder: 1
    }
  ]);

  readonly categories: CostCategory[] = ['LABOR', 'EQUIPMENT', 'MATERIAL', 'SUBCONTRACTOR', 'OVERHEAD', 'CONTINGENCY'];

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

  addLine(): void {
    this.manualLines.update(lines => [
      ...lines,
      {
        description: '',
        costCategory: 'MATERIAL',
        budgetQuantity: 1,
        unitOfMeasure: 'PCS',
        budgetUnitRate: 0,
        sortOrder: lines.length + 1
      }
    ]);
  }

  removeLine(index: number): void {
    this.manualLines.update(lines => lines.filter((_, i) => i !== index));
  }

  onSave(): void {
    if (!this.versionName().trim()) return;

    this.submitting.set(true);
    const req: CreateBudgetVersionRequest = {
      versionName: this.versionName().trim(),
      notes: this.notes().trim() || undefined,
      initFromWbs: this.initFromWbs(),
      lines: this.initFromWbs() ? undefined : this.manualLines().filter(l => l.description.trim().length > 0)
    };

    this.save.emit(req);
  }
}
