import { ChangeDetectionStrategy, Component, inject, input, output } from '@angular/core';
import { I18nService } from '../../../core/i18n.service';

export type PayrollStage = 'DRAFT' | 'REVIEW' | 'APPROVED' | 'POSTED' | 'DISBURSED';

export interface PayrollStepInfo {
  key: PayrollStage;
  labelKey: string;
  stepNumber: number;
}

const STAGE_LABEL_KEYS: Record<PayrollStage, string> = {
  DRAFT: 'payroll.statusDraft',
  REVIEW: 'payroll.statusReview',
  APPROVED: 'payroll.statusApproved',
  POSTED: 'payroll.statusPosted',
  DISBURSED: 'payroll.statusDisbursed',
};

@Component({
  selector: 'app-payroll-stepper',
  standalone: true,
  templateUrl: './payroll-stepper.component.html',
  styleUrl: './payroll-stepper.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PayrollStepperComponent {
  readonly currentStatus = input<string>('DRAFT');
  readonly i18n = inject(I18nService);

  readonly stageChange = output<PayrollStage>();

  readonly steps: PayrollStepInfo[] = [
    { key: 'DRAFT', labelKey: STAGE_LABEL_KEYS.DRAFT, stepNumber: 1 },
    { key: 'REVIEW', labelKey: STAGE_LABEL_KEYS.REVIEW, stepNumber: 2 },
    { key: 'APPROVED', labelKey: STAGE_LABEL_KEYS.APPROVED, stepNumber: 3 },
    { key: 'POSTED', labelKey: STAGE_LABEL_KEYS.POSTED, stepNumber: 4 },
    { key: 'DISBURSED', labelKey: STAGE_LABEL_KEYS.DISBURSED, stepNumber: 5 },
  ];

  label(step: PayrollStepInfo): string {
    return this.i18n.t(step.labelKey);
  }

  getCurrentStepIndex(): number {
    const status = this.currentStatus().toUpperCase();
    switch (status) {
      case 'DRAFT':
      case 'CALCULATED':
        return 0;
      case 'REVIEW':
      case 'REVIEWED':
        return 1;
      case 'APPROVED':
        return 2;
      case 'POSTED':
        return 3;
      case 'PAID':
      case 'DISBURSED':
        return 4;
      default:
        return 0;
    }
  }

  isStepCompleted(index: number): boolean {
    return index < this.getCurrentStepIndex();
  }

  isStepActive(index: number): boolean {
    return index === this.getCurrentStepIndex();
  }

  onStepClick(stepKey: PayrollStage): void {
    this.stageChange.emit(stepKey);
  }
}
