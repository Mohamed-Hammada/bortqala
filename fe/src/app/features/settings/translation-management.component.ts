import { HttpClient, HttpParams } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, OnDestroy, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { apiErrorMessage } from '../../core/api-error';
import { AuthService } from '../../core/auth/auth.service';
import { I18nService, SupportedLocale } from '../../core/i18n.service';
import { NotificationService } from '../../core/notification.service';
import { TablePagination } from '../../shared/ui/table-pagination/pagination';
import { TablePaginationComponent } from '../../shared/ui/table-pagination/table-pagination.component';
import { SampleTemplateService } from '../../core/sample-template.service';

interface AppOption { id: string; code: string; name: string; active: boolean; }

interface TranslationRow {
  key: string;
  defaultValue: string | null;
  overrideValue: string | null;
  effectiveValue: string | null;
  overridden: boolean;
}

interface TranslationPage {
  content: TranslationRow[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  overriddenCount: number;
}

interface TranslationImportResult {
  importedCount: number;
  createdCount: number;
  updatedCount: number;
  unchangedCount: number;
}

@Component({
  selector: 'app-translation-management',
  standalone: true,
  imports: [FormsModule, TablePaginationComponent],
  templateUrl: './translation-management.component.html',
  styleUrl: './translation-management.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TranslationManagementComponent implements OnDestroy {
  private static readonly SEARCH_DEBOUNCE_MS = 300;
  private static readonly MAX_IMPORT_FILE_BYTES = 5 * 1024 * 1024;

  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  readonly i18n = inject(I18nService);
  private readonly notification = inject(NotificationService);
  private readonly sampleTemplates = inject(SampleTemplateService);

  readonly apps = signal<AppOption[]>([]);
  readonly rows = signal<TranslationRow[]>([]);
  readonly locale = signal<SupportedLocale>(this.i18n.locale());
  readonly appId = signal<string | null>(null);
  readonly search = signal('');
  readonly total = signal(0);
  readonly overriddenCount = signal(0);
  readonly loading = signal(false);
  readonly importing = signal(false);
  readonly savingKey = signal<string | null>(null);
  readonly drafts = signal<Record<string, string>>({});
  readonly newKey = signal('');
  readonly newValue = signal('');
  readonly importFile = signal<File | null>(null);
  readonly importFileName = signal('');
  readonly pagination = new TablePagination(25);

  private searchTimer: ReturnType<typeof setTimeout> | null = null;
  private loadRequestId = 0;

  constructor() {
    void this.initialize();
  }

  ngOnDestroy(): void {
    this.cancelSearchReload();
  }

  async changeLocale(value: string): Promise<void> {
    this.cancelSearchReload();
    this.locale.set(value === 'en-US' ? 'en-US' : 'ar-EG');
    this.pagination.page.set(1);
    await this.loadRows();
  }

  async changeScope(value: string): Promise<void> {
    this.cancelSearchReload();
    this.appId.set(value || null);
    this.pagination.page.set(1);
    await this.loadRows();
  }

  changeSearch(value: string): void {
    this.search.set(value);
    this.pagination.page.set(1);
    this.cancelSearchReload();
    this.searchTimer = setTimeout(() => {
      this.searchTimer = null;
      void this.loadRows();
    }, TranslationManagementComponent.SEARCH_DEBOUNCE_MS);
  }

  async changePage(page: number): Promise<void> {
    this.cancelSearchReload();
    this.pagination.changePage(page, this.total());
    await this.loadRows();
  }

  async changePageSize(pageSize: number): Promise<void> {
    this.cancelSearchReload();
    this.pagination.changePageSize(pageSize);
    await this.loadRows();
  }

  updateDraft(key: string, value: string): void {
    this.drafts.update((drafts) => ({ ...drafts, [key]: value }));
  }

  displayValue(row: TranslationRow): string {
    return this.drafts()[row.key]
      ?? (this.appId() ? row.overrideValue ?? row.effectiveValue : row.defaultValue)
      ?? '';
  }

  onImportFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement | null;
    const file = input?.files?.[0] ?? null;
    if (!file) {
      this.clearImportFile(input ?? undefined);
      return;
    }

    if (!/\.(xlsx|xls)$/i.test(file.name)) {
      this.notification.error(this.i18n.t('translations.invalidExcelFile'));
      this.clearImportFile(input ?? undefined);
      return;
    }

    if (file.size > TranslationManagementComponent.MAX_IMPORT_FILE_BYTES) {
      this.notification.error(this.i18n.t('translations.excelFileTooLarge'));
      this.clearImportFile(input ?? undefined);
      return;
    }

    this.importFile.set(file);
    this.importFileName.set(file.name);
  }

  clearImportFile(input?: HTMLInputElement): void {
    this.importFile.set(null);
    this.importFileName.set('');
    if (input) input.value = '';
  }

  async uploadImport(input?: HTMLInputElement): Promise<void> {
    const file = this.importFile();
    if (!file) return;

    const formData = new FormData();
    formData.append('file', file, file.name);
    formData.append('locale', this.locale());
    const appId = this.appId();
    if (appId) formData.append('appId', appId);

    this.cancelSearchReload();
    this.importing.set(true);
    try {
      const result = await firstValueFrom(this.http.post<TranslationImportResult>(
        '/api/v1/i18n/admin/translations/import',
        formData,
      ));
      await this.refreshActiveBundle();
      this.pagination.page.set(1);
      await this.loadRows();
      this.clearImportFile(input);
      this.notification.success(
        this.i18n.t(
          'translations.importSuccess',
          {
            imported: result.importedCount,
            created: result.createdCount,
            updated: result.updatedCount,
            unchanged: result.unchangedCount,
          },
        ),
      );
    } catch (error) {
      this.notification.error(apiErrorMessage(error, this.i18n));
    } finally {
      this.importing.set(false);
    }
  }

  downloadSampleTemplate(): void {
    void this.sampleTemplates.translations().catch(error => this.notification.error(apiErrorMessage(error, this.i18n)));
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

    const saved = await this.saveKey(key, value);
    if (!saved) return;

    this.newKey.set('');
    this.newValue.set('');
  }

  async restore(row: TranslationRow): Promise<void> {
    const appId = this.appId();
    if (!appId || !row.overridden) return;

    this.savingKey.set(row.key);
    try {
      const params = new HttpParams().set('locale', this.locale()).set('appId', appId);
      await firstValueFrom(this.http.delete<TranslationRow>(
        `/api/v1/i18n/admin/translations/${encodeURIComponent(row.key)}`, { params }));
      this.clearDraft(row.key);
      await this.refreshActiveBundle();
      await this.loadRows();
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
    const requestId = ++this.loadRequestId;
    this.loading.set(true);

    try {
      let params = new HttpParams()
        .set('locale', this.locale())
        .set('page', Math.max(0, this.pagination.page() - 1))
        .set('size', this.pagination.pageSize());

      const appId = this.appId();
      const search = this.search().trim();
      if (appId) params = params.set('appId', appId);
      if (search) params = params.set('search', search);

      const response = await firstValueFrom(
        this.http.get<TranslationPage>('/api/v1/i18n/admin/translations', { params }));

      if (requestId !== this.loadRequestId) return;

      this.rows.set(response.content);
      this.total.set(response.totalElements);
      this.overriddenCount.set(response.overriddenCount);
      this.pagination.page.set(response.page + 1);
      this.pagination.pageSize.set(response.size);
      this.drafts.set({});
    } catch (error) {
      if (requestId === this.loadRequestId) {
        this.notification.error(apiErrorMessage(error, this.i18n));
      }
    } finally {
      if (requestId === this.loadRequestId) {
        this.loading.set(false);
      }
    }
  }

  private async saveKey(key: string, textValue: string): Promise<boolean> {
    this.savingKey.set(key);
    try {
      await firstValueFrom(this.http.put<TranslationRow>(
        `/api/v1/i18n/admin/translations/${encodeURIComponent(key)}`,
        { locale: this.locale(), appId: this.appId(), textValue },
      ));
      this.clearDraft(key);
      await this.refreshActiveBundle();
      await this.loadRows();
      this.notification.success(this.i18n.t('translations.saved'));
      return true;
    } catch (error) {
      this.notification.error(apiErrorMessage(error, this.i18n));
      return false;
    } finally {
      this.savingKey.set(null);
    }
  }

  private cancelSearchReload(): void {
    if (this.searchTimer === null) return;
    clearTimeout(this.searchTimer);
    this.searchTimer = null;
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
