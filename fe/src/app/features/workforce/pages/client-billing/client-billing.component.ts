import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { WorkforceService } from '../../data-access/workforce.service';
import { AuthService } from '../../../../core/auth/auth.service';
import { NotificationService } from '../../../../core/notification.service';
import { I18nService } from '../../../../core/i18n.service';
import { ModalDialogComponent } from '../../../../shared/ui/modal-dialog/modal-dialog.component';
import { downloadBlob } from '../../../../core/download';
import { apiErrorDetail } from '../../../../core/api-error';
import {
  ClientBillingLine,
  ClientBillingMargin,
  ClientBillingRate,
  ClientBillingReview,
  CreateClientBillingRatePayload,
} from '../../models/workforce.models';

@Component({
  selector: 'app-client-billing',
  standalone: true,
  imports: [CommonModule, FormsModule, ModalDialogComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './client-billing.component.html',
  styleUrls: ['./client-billing.component.scss']
})
export class ClientBillingComponent implements OnInit {
  readonly workforceService = inject(WorkforceService);
  readonly auth = inject(AuthService);
  readonly i18n = inject(I18nService);
  private readonly notification = inject(NotificationService);

  readonly selectedClientId = signal<string>('');
  readonly selectedPeriod = signal<string>(this.currentMonth());
  readonly pageError = signal<string | null>(null);
  readonly loading = signal(false);
  readonly generating = signal(false);
  readonly confirming = signal(false);
  readonly review = signal<ClientBillingReview | null>(null);
  readonly margin = signal<ClientBillingMargin | null>(null);
  readonly rateModalOpen = signal(false);

  rateForm: CreateClientBillingRatePayload = this.emptyRate();

  ngOnInit(): void {
    this.workforceService.loadClients().subscribe({
      error: error => this.pageError.set(apiErrorDetail(error, this.i18n.t('workforce.clientBilling.loadFailed')))
    });
    this.workforceService.loadCategories().subscribe();
  }

  private currentMonth(): string {
    return new Date().toISOString().slice(0, 7);
  }

  private emptyRate(): CreateClientBillingRatePayload {
    return {
      clientPartyId: '',
      workerCategoryId: '',
      dayRate: 0,
      effectiveFrom: new Date().toISOString().slice(0, 10),
      effectiveTo: ''
    };
  }

  onClientEvent(event: Event): void {
    this.onClientChange((event.target as HTMLSelectElement).value);
  }

  onPeriodChange(event: Event): void {
    this.selectedPeriod.set((event.target as HTMLInputElement).value);
  }

  onClientChange(clientId: string): void {
    this.selectedClientId.set(clientId);
    this.review.set(null);
    this.margin.set(null);
    if (clientId) {
      this.workforceService.loadClientRates(clientId).subscribe({
        error: error => this.notification.error(apiErrorDetail(error, this.i18n.t('workforce.clientBilling.loadFailed')))
      });
    } else {
      this.workforceService.clientRates.set([]);
    }
  }

  clientLabel(client: { id: string; code: string; name: string }): string {
    return `${client.code} · ${client.name}`;
  }

  clientLabelById(id: string): string {
    const client = this.workforceService.clients().find(c => c.id === id);
    return client ? this.clientLabel(client) : id;
  }

  reviewingLocked(review: ClientBillingReview | null): boolean {
    return !!review && review.period.status === 'INVOICED';
  }

  canPrepare(): boolean {
    const can = this.auth.hasAnyPermission(['settlements.prepare', 'settlements.finalize']);
    return can && !!this.selectedClientId() && !!this.selectedPeriod();
  }

  canFinalize(): boolean {
    return this.auth.hasPermission('settlements.finalize');
  }

  hasMissingRates(review: ClientBillingReview | null): boolean {
    return !!review && review.lines.some(line => line.lineStatus === 'MISSING_RATE');
  }

  generate(): void {
    const clientId = this.selectedClientId();
    const period = this.selectedPeriod();
    if (!clientId || !period) {
      this.notification.error(this.i18n.t('workforce.clientBilling.selectClientFirst'));
      return;
    }
    this.generating.set(true);
    this.pageError.set(null);
    this.workforceService.generateClientBilling(clientId, period).subscribe({
      next: result => {
        this.review.set(result);
        this.generating.set(false);
        this.notification.success(this.i18n.t('workforce.clientBilling.generated'));
      },
      error: error => {
        this.generating.set(false);
        this.notification.error(apiErrorDetail(error, this.i18n.t('workforce.clientBilling.generateButton')));
      }
    });
  }

  reviewDraft(): void {
    const clientId = this.selectedClientId();
    const period = this.selectedPeriod();
    if (!clientId || !period) {
      this.notification.error(this.i18n.t('workforce.clientBilling.selectClientFirst'));
      return;
    }
    this.loading.set(true);
    this.pageError.set(null);
    this.workforceService.reviewClientBilling(clientId, period).subscribe({
      next: result => {
        this.review.set(result);
        this.loading.set(false);
      },
      error: error => {
        this.loading.set(false);
        this.notification.error(apiErrorDetail(error, this.i18n.t('workforce.clientBilling.loadFailed')));
      }
    });
  }

  confirm(): void {
    const clientId = this.selectedClientId();
    const period = this.selectedPeriod();
    if (!clientId || !period) {
      this.notification.error(this.i18n.t('workforce.clientBilling.selectClientFirst'));
      return;
    }
    this.confirming.set(true);
    this.workforceService.confirmClientBilling(clientId, period).subscribe({
      next: result => {
        this.confirming.set(false);
        this.notification.success(this.i18n.t('workforce.clientBilling.confirmed', { invoice: result.invoiceNumber }));
        if (this.review()) {
          this.review.update(current => current ? { ...current, period: { ...current.period, status: 'INVOICED', invoiceId: result.invoiceId, invoiceNumber: result.invoiceNumber, totalAmount: result.totalAmount } } : current);
        }
      },
      error: error => {
        this.confirming.set(false);
        this.notification.error(apiErrorDetail(error, this.i18n.t('workforce.clientBilling.confirmButton')));
      }
    });
  }

  loadMargin(): void {
    const clientId = this.selectedClientId();
    const period = this.selectedPeriod();
    if (!clientId || !period) {
      this.notification.error(this.i18n.t('workforce.clientBilling.selectClientFirst'));
      return;
    }
    this.loading.set(true);
    this.workforceService.getClientBillingMargin(clientId, period).subscribe({
      next: result => {
        this.margin.set(result);
        this.loading.set(false);
      },
      error: error => {
        this.loading.set(false);
        this.notification.error(apiErrorDetail(error, this.i18n.t('workforce.clientBilling.loadFailed')));
      }
    });
  }

  exportMargin(): void {
    const clientId = this.selectedClientId();
    const period = this.selectedPeriod();
    if (!clientId || !period) {
      this.notification.error(this.i18n.t('workforce.clientBilling.selectClientFirst'));
      return;
    }
    this.workforceService.exportClientBillingMargin(clientId, period).subscribe({
      next: blob => downloadBlob(blob, `client-billing-margin-${period}.xlsx`),
      error: error => this.notification.error(apiErrorDetail(error, this.i18n.t('workforce.clientBilling.exportMargin')))
    });
  }

  openRateModal(): void {
    this.rateForm = { ...this.emptyRate(), clientPartyId: this.selectedClientId() };
    this.rateModalOpen.set(true);
  }

  saveRate(): void {
    const payload = {
      ...this.rateForm,
      effectiveTo: this.rateForm.effectiveTo || undefined
    };
    this.workforceService.addClientRate(payload).subscribe({
      next: () => {
        this.rateModalOpen.set(false);
        this.notification.success(this.i18n.t('workforce.clientBilling.rateAdded'));
      },
      error: error => this.notification.error(apiErrorDetail(error, this.i18n.t('workforce.clientBilling.addRate')))
    });
  }

  removeRate(rate: ClientBillingRate): void {
    this.workforceService.deleteClientRate(rate.id, this.selectedClientId()).subscribe({
      next: () => this.notification.success(this.i18n.t('workforce.clientBilling.rateRemoved')),
      error: error => this.notification.error(apiErrorDetail(error, this.i18n.t('workforce.clientBilling.deleteRate')))
    });
  }

  lineStatusLabel(line: ClientBillingLine): string {
    return line.lineStatus === 'MISSING_RATE'
      ? this.i18n.t('workforce.clientBilling.missingRate')
      : this.i18n.t('workforce.clientBilling.billable');
  }
}