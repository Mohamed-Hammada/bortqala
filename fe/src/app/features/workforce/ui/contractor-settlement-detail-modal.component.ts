import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ModalDialogComponent } from '../../../shared/ui/modal-dialog/modal-dialog.component';
import { WorkforceService } from '../data-access/workforce.service';
import { ContractorSettlementDetail } from '../models/workforce.models';
import { NotificationService } from '../../../core/notification.service';
import { I18nService } from '../../../core/i18n.service';
import { apiErrorDetail } from '../../../core/api-error';

@Component({
  selector: 'app-contractor-settlement-detail-modal',
  standalone: true,
  imports: [CommonModule, FormsModule, ModalDialogComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './contractor-settlement-detail-modal.component.html',
  styleUrls: ['./contractor-settlement-detail-modal.component.scss']
})
export class ContractorSettlementDetailModalComponent {
  @Input() isOpen = false;
  @Input() settlement: ContractorSettlementDetail | null = null;
  @Output() close = new EventEmitter<void>();
  @Output() updated = new EventEmitter<ContractorSettlementDetail>();

  private readonly workforceService = inject(WorkforceService);
  readonly i18n = inject(I18nService);
  private readonly notification = inject(NotificationService);

  readonly submitting = signal(false);
  readonly showInvoiceForm = signal(false);
  readonly showPaymentForm = signal(false);

  invoiceForm = { invoiceNumber: '', invoiceDate: new Date().toISOString().slice(0, 10), invoiceAmount: 0 };
  paymentForm = { amount: 0, paymentDate: new Date().toISOString().slice(0, 10), paymentReference: '' };

  onClose(): void {
    this.showInvoiceForm.set(false);
    this.showPaymentForm.set(false);
    this.close.emit();
  }

  toggleInvoiceForm(): void {
    if (this.settlement) {
      this.invoiceForm.invoiceNumber = this.settlement.invoiceNumber || '';
      this.invoiceForm.invoiceAmount = this.settlement.netPayable;
    }
    this.showInvoiceForm.set(!this.showInvoiceForm());
    this.showPaymentForm.set(false);
  }

  togglePaymentForm(): void {
    if (this.settlement) {
      this.paymentForm.amount = this.settlement.netPayable - this.settlement.paidAmount;
    }
    this.showPaymentForm.set(!this.showPaymentForm());
    this.showInvoiceForm.set(false);
  }

  postToFinance(): void {
    if (!this.settlement) return;
    this.submitting.set(true);
    const operationId = crypto.randomUUID();
    this.workforceService.postSettlementToFinance(this.settlement.id, {
      operationId,
      expectedVersion: this.settlement.version || 0,
      reason: this.i18n.t('workforce.ui.settlementDetail.postReason')
    }).subscribe({
      next: updatedItem => {
        this.submitting.set(false);
        this.notification.success(this.i18n.t('workforce.settlements.postSuccess'));
        this.updated.emit(updatedItem);
      },
      error: error => {
        this.submitting.set(false);
        this.notification.error(apiErrorDetail(error, this.i18n.t('workforce.ui.settlementDetail.postFailed')));
      }
    });
  }

  saveInvoiceLink(): void {
    if (!this.settlement || !this.invoiceForm.invoiceNumber) return;
    this.submitting.set(true);
    this.workforceService.linkSettlementInvoice(this.settlement.id, {
      invoiceNumber: this.invoiceForm.invoiceNumber,
      invoiceDate: new Date(this.invoiceForm.invoiceDate).getTime(),
      invoiceAmount: this.invoiceForm.invoiceAmount
    }).subscribe({
      next: updatedItem => {
        this.submitting.set(false);
        this.showInvoiceForm.set(false);
        this.notification.success(this.i18n.t('workforce.settlements.invoiceLinkedSuccess'));
        this.updated.emit(updatedItem);
      },
      error: error => {
        this.submitting.set(false);
        this.notification.error(apiErrorDetail(error, this.i18n.t('workforce.ui.settlementDetail.invoiceFailed')));
      }
    });
  }

  savePayment(): void {
    if (!this.settlement || !this.paymentForm.amount) return;
    this.submitting.set(true);
    const operationId = crypto.randomUUID();
    this.workforceService.recordSettlementPayment(this.settlement.id, {
      operationId,
      amount: this.paymentForm.amount,
      paymentDate: new Date(this.paymentForm.paymentDate).getTime(),
      paymentReference: this.paymentForm.paymentReference
    }).subscribe({
      next: updatedItem => {
        this.submitting.set(false);
        this.showPaymentForm.set(false);
        this.notification.success(this.i18n.t('workforce.settlements.paymentRecordedSuccess'));
        this.updated.emit(updatedItem);
      },
      error: error => {
        this.submitting.set(false);
        this.notification.error(apiErrorDetail(error, this.i18n.t('workforce.ui.settlementDetail.paymentFailed')));
      }
    });
  }
}
