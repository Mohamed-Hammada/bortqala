import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { I18nService } from '../../../core/i18n.service';
import { NotificationService } from '../../../core/notification.service';
import { apiErrorMessage } from '../../../core/api-error';

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
  active: boolean;
}

@Component({
  selector: 'app-tax-currency-page',
  imports: [ReactiveFormsModule],
  templateUrl: './tax-currency.page.html',
  styleUrl: './tax-currency.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TaxCurrencyPage {
  readonly http = inject(HttpClient);
  readonly i18n = inject(I18nService);
  readonly notification = inject(NotificationService);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly activeTab = signal<'taxes' | 'currencies'>('taxes');

  readonly taxes = signal<TaxRate[]>([]);
  readonly currencies = signal<Currency[]>([]);

  readonly drawerOpen = signal(false);

  readonly taxForm = new FormGroup({
    code: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    name: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    ratePercentage: new FormControl(14, { nonNullable: true, validators: [Validators.required] }),
    taxType: new FormControl('OUTPUT_VAT', { nonNullable: true, validators: [Validators.required] }),
    active: new FormControl(true, { nonNullable: true }),
  });

  readonly currencyForm = new FormGroup({
    code: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    name: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    symbol: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    isBase: new FormControl(false, { nonNullable: true }),
    exchangeRate: new FormControl(1.0, { nonNullable: true, validators: [Validators.required] }),
    active: new FormControl(true, { nonNullable: true }),
  });

  constructor() {
    void this.load();
  }

  async load() {
    this.loading.set(true);
    this.error.set(null);
    
    try {
      const tData = await firstValueFrom(this.http.get<TaxRate[]>('/api/v1/finance/taxes'));
      this.taxes.set(tData);
    } catch (e) {
      console.error('Failed to load taxes:', e);
    }

    try {
      const cData = await firstValueFrom(this.http.get<Currency[]>('/api/v1/finance/currencies'));
      this.currencies.set(cData);
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    } finally {
      this.loading.set(false);
    }
  }

  openNew() {
    if (this.activeTab() === 'taxes') {
      this.taxForm.reset({ code: '', name: '', ratePercentage: 14, taxType: 'OUTPUT_VAT', active: true });
    } else {
      this.currencyForm.reset({ code: '', name: '', symbol: '', isBase: false, exchangeRate: 1.0, active: true });
    }
    this.drawerOpen.set(true);
  }

  closeDrawer() {
    this.drawerOpen.set(false);
  }

  async submitTax() {
    if (this.taxForm.invalid) return;
    try {
      await firstValueFrom(this.http.post('/api/v1/finance/taxes', this.taxForm.getRawValue()));
      this.notification.success(this.i18n.t('common.save') + ' ✓');
      this.drawerOpen.set(false);
      await this.load();
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    }
  }

  async submitCurrency() {
    if (this.currencyForm.invalid) return;
    try {
      await firstValueFrom(this.http.post('/api/v1/finance/currencies', this.currencyForm.getRawValue()));
      this.notification.success(this.i18n.t('common.save') + ' ✓');
      this.drawerOpen.set(false);
      await this.load();
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    }
  }
}
