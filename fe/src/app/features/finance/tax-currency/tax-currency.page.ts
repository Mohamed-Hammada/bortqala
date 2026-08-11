import { DatePipe, DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';
import { I18nService } from '../../../core/i18n.service';
import { NotificationService } from '../../../core/notification.service';
import { apiErrorMessage } from '../../../core/api-error';
import { ModalDialogComponent } from '../../../shared/ui/modal-dialog/modal-dialog.component';

export interface TaxRate {
  id: string;
  code: string;
  name: string;
  ratePercentage: number;
  taxType: string;
  active: boolean;
}

export interface Currency {
  id: string;
  code: string;
  name: string;
  symbol: string;
  isBase: boolean;
  exchangeRate: number;
  referenceExchangeRate: number | null;
  referenceRateProvider: string | null;
  referenceRateBaseCode: string | null;
  referenceRateDate: number | null;
  referenceRateFetchedAt: number | null;
  referenceRateSupported: boolean | null;
  active: boolean;
}

interface ExchangeRateHintSettings {
  provider: string;
  enabled: boolean;
  refreshIntervalHours: number;
  lastAttemptAt: number | null;
  lastSuccessAt: number | null;
  nextRefreshAt: number | null;
  lastErrorCode: string | null;
}

interface ExchangeRateRefreshResponse {
  success: boolean;
  refreshedCount: number;
  unsupportedCount: number;
  baseCurrency: string | null;
  fetchedAt: number | null;
  errorCode: string | null;
}

@Component({
  selector: 'app-tax-currency-page',
  standalone: true,
  imports: [ReactiveFormsModule, DatePipe, DecimalPipe, ModalDialogComponent],
  templateUrl: './tax-currency.page.html',
  styleUrl: './tax-currency.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TaxCurrencyPage {
  readonly http = inject(HttpClient);
  readonly i18n = inject(I18nService);
  readonly notification = inject(NotificationService);
  readonly authService = inject(AuthService);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly activeTab = signal<'taxes' | 'currencies'>('taxes');
  readonly taxes = signal<TaxRate[]>([]);
  readonly currencies = signal<Currency[]>([]);
  readonly drawerOpen = signal(false);

  readonly hintSettings = signal<ExchangeRateHintSettings | null>(null);
  readonly hintError = signal<string | null>(null);
  readonly hintSettingsSaving = signal(false);
  readonly hintsRefreshing = signal(false);

  readonly baseCurrencyCode = computed(
    () => this.currencies().find((currency) => currency.active && currency.isBase)?.code ?? '—',
  );

  readonly taxForm = new FormGroup({
    code: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    name: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    ratePercentage: new FormControl(14, { nonNullable: true, validators: [Validators.required, Validators.min(0)] }),
    taxType: new FormControl('OUTPUT_VAT', { nonNullable: true, validators: [Validators.required] }),
    active: new FormControl(true, { nonNullable: true }),
  });

  readonly currencyForm = new FormGroup({
    code: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    name: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    symbol: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    isBase: new FormControl(false, { nonNullable: true }),
    exchangeRate: new FormControl(1, { nonNullable: true, validators: [Validators.required, Validators.min(0.0001)] }),
    active: new FormControl(true, { nonNullable: true }),
  });

  readonly hintSettingsForm = new FormGroup({
    enabled: new FormControl(true, { nonNullable: true }),
    refreshIntervalHours: new FormControl(4, {
      nonNullable: true,
      validators: [Validators.required, Validators.min(1), Validators.max(168)],
    }),
  });

  constructor() {
    void this.load();
  }

  canConfigureHints(): boolean {
    return this.authService.hasAnyRole(['SUPER_ADMIN', 'ADMIN']);
  }

  canRefreshHints(): boolean {
    return this.authService.hasAnyRole(['SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER']);
  }

  async load(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      this.taxes.set(await firstValueFrom(this.http.get<TaxRate[]>('/api/v1/finance/taxes')));
      this.currencies.set(await firstValueFrom(this.http.get<Currency[]>('/api/v1/finance/currencies')));
      await this.loadHintSettings();
    } catch (error) {
      this.error.set(apiErrorMessage(error, this.i18n));
    } finally {
      this.loading.set(false);
    }
  }

  async loadHintSettings(): Promise<void> {
    this.hintError.set(null);
    try {
      const settings = await firstValueFrom(
        this.http.get<ExchangeRateHintSettings>('/api/v1/finance/exchange-rate-hints/settings'),
      );
      this.hintSettings.set(settings);
      this.hintSettingsForm.patchValue({
        enabled: settings.enabled,
        refreshIntervalHours: settings.refreshIntervalHours,
      });
      this.hintSettingsForm.markAsPristine();
    } catch (error) {
      this.hintError.set(apiErrorMessage(error, this.i18n));
    }
  }

  async saveHintSettings(): Promise<void> {
    if (!this.canConfigureHints() || this.hintSettingsForm.invalid) {
      this.hintSettingsForm.markAllAsTouched();
      return;
    }

    this.hintSettingsSaving.set(true);
    try {
      const saved = await firstValueFrom(
        this.http.put<ExchangeRateHintSettings>(
          '/api/v1/finance/exchange-rate-hints/settings',
          this.hintSettingsForm.getRawValue(),
        ),
      );
      this.hintSettings.set(saved);
      this.hintSettingsForm.markAsPristine();
      this.notification.success(this.i18n.t('taxCurrency.onlineSettingsSaved'));
    } catch (error) {
      this.notification.error(apiErrorMessage(error, this.i18n));
    } finally {
      this.hintSettingsSaving.set(false);
    }
  }

  async refreshHintsNow(): Promise<void> {
    if (!this.canRefreshHints()) return;

    this.hintsRefreshing.set(true);
    try {
      const result = await firstValueFrom(
        this.http.post<ExchangeRateRefreshResponse>('/api/v1/finance/exchange-rate-hints/refresh', {}),
      );

      if (!result.success) {
        this.notification.warning(this.onlineError(result.errorCode));
      } else if (result.unsupportedCount > 0) {
        this.notification.warning(this.i18n.t('taxCurrency.onlineRefreshPartial'));
      } else {
        this.notification.success(this.i18n.t('taxCurrency.onlineRefreshSuccess'));
      }

      this.currencies.set(await firstValueFrom(this.http.get<Currency[]>('/api/v1/finance/currencies')));
      await this.loadHintSettings();
    } catch (error) {
      this.notification.error(apiErrorMessage(error, this.i18n));
    } finally {
      this.hintsRefreshing.set(false);
    }
  }

  onlineError(code: string | null): string {
    switch (code) {
      case 'DISABLED':
        return this.i18n.t('taxCurrency.onlineError.DISABLED');
      case 'NO_ACTIVE_CURRENCIES':
        return this.i18n.t('taxCurrency.onlineError.NO_ACTIVE_CURRENCIES');
      case 'BASE_CURRENCY_REQUIRED':
        return this.i18n.t('taxCurrency.onlineError.BASE_CURRENCY_REQUIRED');
      case 'MULTIPLE_BASE_CURRENCIES':
        return this.i18n.t('taxCurrency.onlineError.MULTIPLE_BASE_CURRENCIES');
      case 'BASE_CURRENCY_UNSUPPORTED':
        return this.i18n.t('taxCurrency.onlineError.BASE_CURRENCY_UNSUPPORTED');
      default:
        return this.i18n.t('taxCurrency.onlineError.FRANKFURTER_UNAVAILABLE');
    }
  }

  rateDifferencePercent(currency: Currency): number | null {
    const reference = currency.referenceExchangeRate;
    if (reference == null || reference === 0) return null;
    return ((currency.exchangeRate - reference) / reference) * 100;
  }

  openNew(): void {
    this.drawerOpen.set(true);
  }

  closeDrawer(): void {
    this.drawerOpen.set(false);
    this.taxForm.reset({ code: '', name: '', ratePercentage: 14, taxType: 'OUTPUT_VAT', active: true });
    this.currencyForm.reset({ code: '', name: '', symbol: '', isBase: false, exchangeRate: 1, active: true });
  }

  async submitTax(): Promise<void> {
    if (this.taxForm.invalid) return;
    try {
      const created = await firstValueFrom(
        this.http.post<TaxRate>('/api/v1/finance/taxes', this.taxForm.getRawValue()),
      );
      this.taxes.update((list) => [...list, created].sort((a, b) => a.code.localeCompare(b.code)));
      this.notification.success(this.i18n.t('taxCurrency.taxCreated'));
      this.closeDrawer();
    } catch (error) {
      this.notification.error(apiErrorMessage(error, this.i18n));
    }
  }

  async submitCurrency(): Promise<void> {
    if (this.currencyForm.invalid) return;
    try {
      const created = await firstValueFrom(
        this.http.post<Currency>('/api/v1/finance/currencies', this.currencyForm.getRawValue()),
      );
      this.currencies.update((list) => [...list, created].sort((a, b) => a.code.localeCompare(b.code)));
      this.notification.success(this.i18n.t('taxCurrency.currencyCreated'));
      this.closeDrawer();
    } catch (error) {
      this.notification.error(apiErrorMessage(error, this.i18n));
    }
  }
}
