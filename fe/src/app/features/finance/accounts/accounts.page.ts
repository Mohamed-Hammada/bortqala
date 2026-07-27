import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { I18nService } from '../../../core/i18n.service';
import { NotificationService } from '../../../core/notification.service';
import { apiErrorMessage } from '../../../core/api-error';

export interface Account {
  id: string;
  code: string;
  name: string;
  type: string;
  parentId?: string;
  isHeader: boolean;
  currency: string;
  active: boolean;
}

@Component({
  selector: 'app-accounts-page',
  imports: [ReactiveFormsModule],
  templateUrl: './accounts.page.html',
  styleUrl: './accounts.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AccountsPage {
  readonly http = inject(HttpClient);
  readonly i18n = inject(I18nService);
  readonly notification = inject(NotificationService);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly accounts = signal<Account[]>([]);
  readonly drawerOpen = signal(false);
  readonly editingId = signal<string | null>(null);

  readonly form = new FormGroup({
    code: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    name: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    type: new FormControl('ASSET', { nonNullable: true, validators: [Validators.required] }),
    parentId: new FormControl('', { nonNullable: true }),
    isHeader: new FormControl(false, { nonNullable: true }),
    currency: new FormControl('EGP', { nonNullable: true }),
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
        this.http.get<Account[]>('/api/v1/finance/accounts'),
      );
      this.accounts.set(data);
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    } finally {
      this.loading.set(false);
    }
  }

  openNew() {
    this.editingId.set(null);
    this.form.reset({ code: '', name: '', type: 'ASSET', parentId: '', isHeader: false, currency: 'EGP', active: true });
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
          ? this.http.put(`/api/v1/finance/accounts/${id}`, payload)
          : this.http.post('/api/v1/finance/accounts', payload),
      );
      this.notification.success(this.i18n.t('common.save') + ' ✓');
      this.drawerOpen.set(false);
      await this.load();
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    }
  }
}
