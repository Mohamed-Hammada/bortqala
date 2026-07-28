import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

export type PayrollStage = 'DRAFT' | 'REVIEW' | 'APPROVED' | 'POSTED' | 'DISBURSED';

export interface PayrollStepInfo {
  key: PayrollStage;
  labelAr: string;
  labelEn: string;
  stepNumber: number;
}

@Component({
  selector: 'app-payroll-stepper',
  standalone: true,
  templateUrl: './payroll-stepper.component.html',
  styleUrl: './payroll-stepper.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PayrollStepperComponent {
  readonly currentStatus = input<string>('DRAFT');
  readonly isEn = input<boolean>(false);

  readonly stageChange = output<PayrollStage>();

  readonly steps: PayrollStepInfo[] = [
    { key: 'DRAFT', labelAr: 'مسودة', labelEn: 'Draft', stepNumber: 1 },
    { key: 'REVIEW', labelAr: 'مراجعة', labelEn: 'Review', stepNumber: 2 },
    { key: 'APPROVED', labelAr: 'اعتماد وقفل', labelEn: 'Approved & Locked', stepNumber: 3 },
    { key: 'POSTED', labelAr: 'ترحيل محاسبي', labelEn: 'Accounting Posting', stepNumber: 4 },
    { key: 'DISBURSED', labelAr: 'صرف المرتبات', labelEn: 'Disbursement', stepNumber: 5 },
  ];

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
