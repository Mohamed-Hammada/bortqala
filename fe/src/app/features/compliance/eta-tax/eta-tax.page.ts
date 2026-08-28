import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { I18nService } from '../../../core/i18n.service';
import { NotificationService } from '../../../core/notification.service';
import {
  EtaConfig,
  EtaDocumentType,
  EtaItemMapping,
  EtaSubmission,
  EtaSubmissionStatus,
  EtaSummary,
} from './eta-tax.models';
import { EtaTaxService } from './eta-tax.service';
import { HttpClient } from '@angular/common/http';
import { EinvoicingProviderInfo, EinvoicingSettings, EinvoicingProviderType, EinvoicingEnvironment } from '../einvoicing.models';
import { EinvoicingService } from '../einvoicing.service';
import { apiErrorMessage } from '../../../core/api-error';
import { ConfirmDialogService } from '../../../core/confirm-dialog.service';

@Component({
  selector: 'app-eta-tax-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './eta-tax.page.html',
  styleUrls: ['./eta-tax.page.scss'],
})
export class EtaTaxPage implements OnInit {
  readonly i18n = inject(I18nService);
  private readonly etaService = inject(EtaTaxService);
  private readonly notification = inject(NotificationService);
  private readonly fb = inject(FormBuilder);
  private readonly http = inject(HttpClient);
  private readonly einvoicingService = inject(EinvoicingService);
  private readonly confirmDialog = inject(ConfirmDialogService);
  readonly einvoicingSettings = signal<EinvoicingSettings | null>(null);
  readonly einvoicingProviders = signal<EinvoicingProviderInfo[]>([]);
  readonly showProviderModal = signal(false);
  readonly savingProvider = signal(false);

  readonly activeTab = signal<'SUBMISSIONS' | 'CONFIG' | 'MAPPINGS'>('SUBMISSIONS');
  readonly loading = signal(false);
  readonly savingConfig = signal(false);
  readonly submittingDoc = signal(false);
  readonly cancellingDoc = signal(false);
  readonly savingMapping = signal(false);

  readonly summary = signal<EtaSummary | null>(null);
  readonly submissions = signal<EtaSubmission[]>([]);
  readonly config = signal<EtaConfig | null>(null);
  readonly itemMappings = signal<EtaItemMapping[]>([]);
  readonly openInvoices = signal<any[]>([]);

  readonly showQueueModal = signal(false);
  readonly showCancelModal = signal(false);
  readonly showMappingModal = signal(false);
  readonly showRawJsonModal = signal(false);
  readonly selectedRawJson = signal<string>('');
  readonly activeCancelSubmissionId = signal<string | null>(null);

  readonly filterStatus = signal<EtaSubmissionStatus | ''>('');
  readonly filterDocType = signal<EtaDocumentType | ''>('');

  readonly configForm = this.fb.group({
    clientId: ['', [Validators.required]],
    clientSecret: [''],
    issuerTaxId: ['', [Validators.required]],
    issuerName: ['', [Validators.required]],
    environment: ['PRE_PRODUCTION' as 'PRE_PRODUCTION' | 'PRODUCTION', [Validators.required]],
    tokenUrl: [''],
    apiBaseUrl: [''],
    active: [true],
  });

  readonly providerForm = this.fb.group({
    provider: ['NONE' as EinvoicingProviderType, [Validators.required]],
    environment: ['TEST' as EinvoicingEnvironment, [Validators.required]],
  });

  readonly queueForm = this.fb.group({
    invoiceId: ['', [Validators.required]],
    documentType: ['INVOICE' as EtaDocumentType, [Validators.required]],
  });

  readonly cancelForm = this.fb.group({
    reason: ['', [Validators.required, Validators.minLength(5)]],
  });

  readonly mappingForm = this.fb.group({
    itemId: ['', [Validators.required]],
    itemCode: ['', [Validators.required]],
    codeType: ['EGS', [Validators.required]],
    itemCodeValue: ['', [Validators.required]],
    descriptionAr: [''],
    descriptionEn: [''],
    active: [true],
  });

  ngOnInit(): void {
    this.loadAll();
  }

  loadAll(): void {
    this.loading.set(true);
    this.loadSummary();
    this.loadSubmissions();
    this.loadConfig();
    this.loadItemMappings();
    this.loadOpenInvoices();
    this.loadEinvoicing();
  }

  loadEinvoicing(): void {
    this.einvoicingService.getSettings().then((settings) => {
      this.einvoicingSettings.set(settings);
      if (settings) {
        this.providerForm.patchValue({
          provider: settings.provider,
          environment: settings.environment,
        });
      }
    }).catch(() => {});
    this.einvoicingService.listProviders().then((providers) => {
      this.einvoicingProviders.set(providers ?? []);
    }).catch(() => {});
  }

  providerLabel(provider: EinvoicingProviderType): string {
    const info = this.einvoicingProviders().find((item) => item.type === provider);
    return info ? this.i18n.t(info.labelKey) : provider;
  }

  providerSupported(provider: EinvoicingProviderType): boolean {
    return this.einvoicingProviders().find((item) => item.type === provider)?.supported ?? false;
  }

  openProviderModal(): void {
    const settings = this.einvoicingSettings();
    this.providerForm.patchValue({
      provider: settings?.provider ?? 'NONE',
      environment: settings?.environment ?? 'TEST',
    });
    this.showProviderModal.set(true);
  }

  closeProviderModal(): void {
    this.showProviderModal.set(false);
  }

  onProviderChange(): void {
    const provider = this.providerForm.get('provider')?.value as EinvoicingProviderType;
    if (!this.providerSupported(provider)) {
      this.notification.error(this.i18n.t('compliance.provider.notImplemented'));
    }
  }

  async saveProviderSettings(): Promise<void> {
    if (this.providerForm.invalid || this.savingProvider()) return;
    const value = this.providerForm.value;
    const switching = value.provider !== (this.einvoicingSettings()?.provider ?? 'NONE');
    if (switching) {
      const accepted = await this.confirmDialog.confirmOptions({
        titleKey: 'compliance.provider.settingsTitle',
        messageKey: 'compliance.provider.confirmSwitch',
        confirmKey: 'action.confirm',
        cancelKey: 'action.cancel',
      });
      if (!accepted) return;
    }
    this.savingProvider.set(true);
    try {
      const saved = await this.einvoicingService.saveSettings({
        provider: value.provider as EinvoicingProviderType,
        environment: value.environment as EinvoicingEnvironment,
      });
      this.einvoicingSettings.set(saved);
      this.showProviderModal.set(false);
      this.notification.success(this.i18n.t('compliance.provider.saved'));
    } catch (e: unknown) {
      this.notification.error(apiErrorMessage(e as never, this.i18n));
    } finally {
      this.savingProvider.set(false);
    }
  }

  loadSummary(): void {
    this.etaService.getSummary().subscribe({
      next: res => this.summary.set(res),
      error: () => {},
    });
  }

  loadSubmissions(): void {
    const s = this.filterStatus() || undefined;
    const d = this.filterDocType() || undefined;
    this.etaService.getSubmissions(s, d).subscribe({
      next: res => {
        this.submissions.set(res);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  loadConfig(): void {
    this.etaService.getConfig().subscribe({
      next: res => {
        if (res) {
          this.config.set(res);
          this.configForm.patchValue({
            clientId: res.clientId,
            clientSecret: '',
            issuerTaxId: res.issuerTaxId,
            issuerName: res.issuerName,
            environment: res.environment,
            tokenUrl: res.tokenUrl,
            apiBaseUrl: res.apiBaseUrl,
            active: res.active,
          });
        }
      },
      error: () => {},
    });
  }

  loadItemMappings(): void {
    this.etaService.getItemMappings().subscribe({
      next: res => this.itemMappings.set(res),
      error: () => {},
    });
  }

  loadOpenInvoices(): void {
    this.http.get<any[]>('/api/v1/trade/sales/receivables/invoices').subscribe({
      next: res => this.openInvoices.set(res || []),
      error: () => {},
    });
  }

  saveConfig(): void {
    if (this.configForm.invalid) return;
    this.savingConfig.set(true);
    const val = this.configForm.value;
    this.etaService
      .saveConfig({
        clientId: val.clientId!,
        clientSecret: val.clientSecret || undefined,
        issuerTaxId: val.issuerTaxId!,
        issuerName: val.issuerName!,
        environment: val.environment!,
        tokenUrl: val.tokenUrl || undefined,
        apiBaseUrl: val.apiBaseUrl || undefined,
        active: !!val.active,
      })
      .subscribe({
        next: res => {
          this.config.set(res);
          this.savingConfig.set(false);
          this.notification.success(this.i18n.t('eta.configSaved'));
        },
        error: () => this.savingConfig.set(false),
      });
  }

  openQueueModal(): void {
    this.queueForm.reset({
      invoiceId: this.openInvoices().length ? this.openInvoices()[0].id : '',
      documentType: 'INVOICE',
    });
    this.showQueueModal.set(true);
  }

  closeQueueModal(): void {
    this.showQueueModal.set(false);
  }

  submitQueue(): void {
    if (this.queueForm.invalid) return;
    const { invoiceId, documentType } = this.queueForm.value;
    this.etaService.queueInvoice(invoiceId!, documentType!).subscribe({
      next: () => {
        this.closeQueueModal();
        this.loadSubmissions();
        this.loadSummary();
      },
      error: () => {},
    });
  }

  submitToEta(sub: EtaSubmission): void {
    this.submittingDoc.set(true);
    this.etaService.submitToEta(sub.id).subscribe({
      next: () => {
        this.submittingDoc.set(false);
        this.notification.success(this.i18n.t('eta.submittedSuccess'));
        this.loadSubmissions();
        this.loadSummary();
      },
      error: () => this.submittingDoc.set(false),
    });
  }

  openCancelModal(sub: EtaSubmission): void {
    this.activeCancelSubmissionId.set(sub.id);
    this.cancelForm.reset({ reason: '' });
    this.showCancelModal.set(true);
  }

  closeCancelModal(): void {
    this.showCancelModal.set(false);
    this.activeCancelSubmissionId.set(null);
  }

  submitCancel(): void {
    if (this.cancelForm.invalid) return;
    const id = this.activeCancelSubmissionId();
    if (!id) return;
    this.cancellingDoc.set(true);
    this.etaService.cancelDocument(id, this.cancelForm.value.reason!).subscribe({
      next: () => {
        this.cancellingDoc.set(false);
        this.closeCancelModal();
        this.notification.success(this.i18n.t('eta.cancelledSuccess'));
        this.loadSubmissions();
        this.loadSummary();
      },
      error: () => this.cancellingDoc.set(false),
    });
  }

  openRawJson(sub: EtaSubmission): void {
    this.selectedRawJson.set(
      sub.rawResponseJson ||
        JSON.stringify(
          {
            internalId: sub.internalId,
            documentType: sub.documentType,
            dateTimeIssued: new Date(sub.dateTimeIssued).toISOString(),
            totalSalesAmount: sub.totalSalesAmount,
            netAmount: sub.netAmount,
            taxAmount: sub.taxAmount,
            totalAmount: sub.totalAmount,
            canonicalHash: sub.canonicalJsonHash,
          },
          null,
          2
        )
    );
    this.showRawJsonModal.set(true);
  }

  closeRawJson(): void {
    this.showRawJsonModal.set(false);
    this.selectedRawJson.set('');
  }

  openMappingModal(): void {
    this.mappingForm.reset({
      itemId: '',
      itemCode: '',
      codeType: 'EGS',
      itemCodeValue: '',
      descriptionAr: '',
      descriptionEn: '',
      active: true,
    });
    this.showMappingModal.set(true);
  }

  closeMappingModal(): void {
    this.showMappingModal.set(false);
  }

  saveMapping(): void {
    if (this.mappingForm.invalid) return;
    this.savingMapping.set(true);
    const val = this.mappingForm.value;
    this.etaService
      .saveItemMapping({
        itemId: val.itemId!,
        itemCode: val.itemCode!,
        codeType: val.codeType!,
        itemCodeValue: val.itemCodeValue!,
        descriptionAr: val.descriptionAr || undefined,
        descriptionEn: val.descriptionEn || undefined,
        active: !!val.active,
      })
      .subscribe({
        next: () => {
          this.savingMapping.set(false);
          this.closeMappingModal();
          this.notification.success(this.i18n.t('eta.itemMappingSaved'));
          this.loadItemMappings();
        },
        error: () => this.savingMapping.set(false),
      });
  }
}
