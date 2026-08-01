import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { I18nService } from '../../../core/i18n.service';
import { NotificationService } from '../../../core/notification.service';
import { apiErrorMessage } from '../../../core/api-error';
import { ModalDialogComponent } from '../../../shared/ui/modal-dialog/modal-dialog.component';

export interface BankAccount {
  id: string;
  bankName: string;
  accountNumber: string;
  iban?: string;
  swiftCode?: string;
  accountId?: string;
  active: boolean;
}

@Component({
  selector: 'app-banks-page',
  imports: [ReactiveFormsModule, ModalDialogComponent],
  templateUrl: './banks.page.html',
  styleUrl: './banks.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BanksPage {
  readonly http = inject(HttpClient);
  readonly i18n = inject(I18nService);
  readonly notification = inject(NotificationService);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly banks = signal<BankAccount[]>([]);
  readonly drawerOpen = signal(false);
  readonly editingId = signal<string | null>(null);

  readonly form = new FormGroup({
    bankName: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    accountNumber: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    iban: new FormControl('', { nonNullable: true }),
    swiftCode: new FormControl('', { nonNullable: true }),
    active: new FormControl(true, { nonNullable: true }),
  });

  constructor() {
    void this.load();
  }

  async load() {
    this.loading.set(true);
    this.error.set(null);
    try {
      const data = await firstValueFrom(
        this.http.get<BankAccount[]>('/api/v1/finance/banks'),
      );
      this.banks.set(data);
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    } finally {
      this.loading.set(false);
    }
  }

  openNew() {
    this.editingId.set(null);
    this.form.reset({ bankName: '', accountNumber: '', iban: '', swiftCode: '', active: true });
    this.drawerOpen.set(true);
  }

  closeDrawer() {
    this.drawerOpen.set(false);
  }

  async submit() {
    if (this.form.invalid) return;
    try {
      const payload = this.form.getRawValue();
      const id = this.editingId();
      await firstValueFrom(
        id
          ? this.http.put(`/api/v1/finance/banks/${id}`, payload)
          : this.http.post('/api/v1/finance/banks', payload),
      );
      this.notification.success(this.i18n.t('common.save') + ' ✓');
      this.drawerOpen.set(false);
      await this.load();
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    }
  }
}
