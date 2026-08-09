import { HttpClient, HttpParams } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { apiErrorMessage } from '../../core/api-error';
import { AuthService } from '../../core/auth/auth.service';
import { I18nService, SupportedLocale } from '../../core/i18n.service';
import { NotificationService } from '../../core/notification.service';

interface AppOption { id: string; code: string; name: string; active: boolean; }
interface TranslationRow {
  key: string;
  defaultValue: string | null;
  overrideValue: string | null;
  effectiveValue: string | null;
  overridden: boolean;
}

@Component({
  selector: 'app-translation-management',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './translation-management.component.html',
  styleUrl: './translation-management.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TranslationManagementComponent {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  readonly i18n = inject(I18nService);
  private readonly notification = inject(NotificationService);

  readonly apps = signal<AppOption[]>([]);
  readonly rows = signal<TranslationRow[]>([]);
  readonly locale = signal<SupportedLocale>(this.i18n.locale());
  readonly appId = signal<string | null>(null);
  readonly search = signal('');
  readonly loading = signal(false);
  readonly savingKey = signal<string | null>(null);
  readonly drafts = signal<Record<string, string>>({});
  readonly newKey = signal('');
  readonly newValue = signal('');

  readonly filteredRows = computed(() => {
    const query = this.search().trim().toLowerCase();
    if (!query) return this.rows();
    return this.rows().filter((row) =>
      row.key.toLowerCase().includes(query) || (row.effectiveValue ?? '').toLowerCase().includes(query));
  });

  constructor() {
    void this.initialize();
  }

  async changeLocale(value: string): Promise<void> {
    this.locale.set(value === 'en-US' ? 'en-US' : 'ar-EG');
    await this.loadRows();
  }

  async changeScope(value: string): Promise<void> {
    this.appId.set(value || null);
    await this.loadRows();
  }

  updateDraft(key: string, value: string): void {
    this.drafts.update((drafts) => ({ ...drafts, [key]: value }));
  }

  displayValue(row: TranslationRow): string {
    return this.drafts()[row.key] ?? (this.appId() ? row.overrideValue ?? row.effectiveValue : row.defaultValue) ?? '';
  }

  async save(row: TranslationRow): Promise<void> {
    const value = this.displayValue(row).trim();
    if (!value) return;
    await this.saveKey(row.key, value);
  }

  async add(): Promise<void> {
    const key = this.newKey().trim();
    const value = this.newValue().trim();
    if (!key || !value) return;
    await this.saveKey(key, value);
    this.newKey.set('');
    this.newValue.set('');
  }

  async restore(row: TranslationRow): Promise<void> {
    const appId = this.appId();
    if (!appId || !row.overridden) return;
    this.savingKey.set(row.key);
    try {
      const params = new HttpParams().set('locale', this.locale()).set('appId', appId);
      const updated = await firstValueFrom(this.http.delete<TranslationRow>(
        `/api/v1/i18n/admin/translations/${encodeURIComponent(row.key)}`, { params }));
      this.replaceRow(updated);
      this.clearDraft(row.key);
      await this.refreshActiveBundle();
      this.notification.success(this.i18n.t('translations.restored'));
    } catch (error) {
      this.notification.error(apiErrorMessage(error, this.i18n));
    } finally {
      this.savingKey.set(null);
    }
  }

  private async initialize(): Promise<void> {
    this.loading.set(true);
    try {
      this.apps.set(await firstValueFrom(this.http.get<AppOption[]>('/api/v1/i18n/admin/apps')));
      await this.loadRows();
    } catch (error) {
      this.notification.error(apiErrorMessage(error, this.i18n));
      this.loading.set(false);
    }
  }

  private async loadRows(): Promise<void> {
    this.loading.set(true);
    try {
      let params = new HttpParams().set('locale', this.locale());
      if (this.appId()) params = params.set('appId', this.appId()!);
      this.rows.set(await firstValueFrom(
        this.http.get<TranslationRow[]>('/api/v1/i18n/admin/translations', { params })));
      this.drafts.set({});
    } catch (error) {
      this.notification.error(apiErrorMessage(error, this.i18n));
    } finally {
      this.loading.set(false);
    }
  }

  private async saveKey(key: string, textValue: string): Promise<void> {
    this.savingKey.set(key);
    try {
      const updated = await firstValueFrom(this.http.put<TranslationRow>(
        `/api/v1/i18n/admin/translations/${encodeURIComponent(key)}`,
        { locale: this.locale(), appId: this.appId(), textValue },
      ));
      this.replaceRow(updated);
      this.clearDraft(key);
      await this.refreshActiveBundle();
      this.notification.success(this.i18n.t('translations.saved'));
    } catch (error) {
      this.notification.error(apiErrorMessage(error, this.i18n));
    } finally {
      this.savingKey.set(null);
    }
  }

  private replaceRow(updated: TranslationRow): void {
    this.rows.update((rows) => {
      const found = rows.some((row) => row.key === updated.key);
      const next = found ? rows.map((row) => row.key === updated.key ? updated : row) : [...rows, updated];
      return next.sort((a, b) => a.key.localeCompare(b.key));
    });
  }

  private clearDraft(key: string): void {
    this.drafts.update((drafts) => {
      const next = { ...drafts };
      delete next[key];
      return next;
    });
  }

  private async refreshActiveBundle(): Promise<void> {
    const changedAppId = this.appId();
    this.i18n.invalidate(this.locale(), changedAppId);
    if (this.locale() !== this.i18n.locale()) return;
    const currentAppId = this.auth.app()?.id ?? null;
    if (changedAppId !== null && changedAppId !== currentAppId) return;
    this.i18n.invalidate(this.locale(), currentAppId);
    await this.i18n.use(this.locale(), currentAppId);
  }
}
