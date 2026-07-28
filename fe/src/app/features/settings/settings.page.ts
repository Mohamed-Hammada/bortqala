import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { apiErrorMessage } from '../../core/api-error';
import { AuthService } from '../../core/auth/auth.service';
import { ExcelTableStyle, TableDensity, ThemePreference } from '../../core/auth/auth.models';
import { I18nService } from '../../core/i18n.service';
import { NotificationService } from '../../core/notification.service';

export type SettingsTab = 'appearance' | 'session' | 'security' | 'reports';

@Component({
  selector: 'app-settings-page',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './settings.page.html',
  styleUrl: './settings.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SettingsPage {
  readonly authService = inject(AuthService);
  readonly i18n = inject(I18nService);
  readonly notification = inject(NotificationService);
  private readonly formBuilder = inject(FormBuilder);

  readonly activeTab = signal<SettingsTab>('appearance');

  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  readonly saved = signal(false);
  readonly appSettingsLoading = signal(false);
  readonly appSettingsSaving = signal(false);
  readonly appSettingsSaved = signal(false);
  readonly appSettingsError = signal<string | null>(null);
  readonly desktop = typeof window !== 'undefined' && '__TAURI__' in window;
  readonly licenseMessage = signal<string | null>(null);

  readonly showFavorites = signal(localStorage.getItem('bemo_show_favorites') !== 'false');
  readonly showRecentlyUsed = signal(localStorage.getItem('bemo_show_recently_used') !== 'false');

  toggleShowFavorites(val: boolean) {
    this.showFavorites.set(val);
    localStorage.setItem('bemo_show_favorites', String(val));
    this.notification.success(this.i18n.t('settings.menuSavedFavorites'));
  }

  toggleShowRecentlyUsed(val: boolean) {
    this.showRecentlyUsed.set(val);
    localStorage.setItem('bemo_show_recently_used', String(val));
    this.notification.success(this.i18n.t('settings.menuSavedRecent'));
  }

  clearRecentHistory() {
    localStorage.removeItem('bemo_recent_menus');
    this.notification.success(this.i18n.t('settings.menuCleared'));
  }

  readonly form = this.formBuilder.nonNullable.group({
    theme: [this.authService.preferences().theme as ThemePreference, Validators.required],
    tableDensity: [
      this.authService.preferences().tableDensity as TableDensity,
      Validators.required,
    ],
    locale: [this.authService.preferences().locale, Validators.required],
    excelTableStyle: [
      this.authService.preferences().excelTableStyle as ExcelTableStyle,
      Validators.required,
    ],
  });

  readonly appSettingsForm = this.formBuilder.nonNullable.group({
    sessionTimeoutMinutes: [480, [Validators.required, Validators.min(5), Validators.max(10_080)]],
    sessionTimeoutEnabled: [true, Validators.required],
    showReportPresets: [true, Validators.required],
    minPasswordLength: [8, [Validators.required, Validators.min(6), Validators.max(128)]],
    requireUppercase: [false],
    requireLowercase: [false],
    requireNumbers: [false],
    requireSpecialChars: [false],
    disallowSpaces: [false],
    maxPasswordLength: [128, [Validators.min(0), Validators.max(256)]],
    passwordExpiryDays: [0, [Validators.min(0), Validators.max(365)]],
    passwordHistoryCount: [0, [Validators.min(0), Validators.max(50)]],
  });

  constructor() {
    if (this.authService.hasAnyRole(['SUPER_ADMIN', 'ADMIN'])) void this.loadAppSettings();
  }

  setTab(tab: SettingsTab): void {
    this.activeTab.set(tab);
  }

  async releaseLicense(): Promise<void> {
    if (!this.desktop || !confirm(this.i18n.t('settings.licenseReleaseConfirm'))) return;
    try {
      const tauri = (
        window as unknown as { __TAURI__: { core: { invoke: (name: string) => Promise<void> } } }
      ).__TAURI__;
      await tauri.core.invoke('deactivate_license');
      this.licenseMessage.set(this.i18n.t('settings.licenseReleased'));
    } catch (error) {
      this.licenseMessage.set(String(error));
    }
  }

  cancel(): void {
    const prefs = this.authService.preferences();
    this.form.reset({
      theme: prefs.theme as ThemePreference,
      tableDensity: prefs.tableDensity as TableDensity,
      locale: prefs.locale,
      excelTableStyle: prefs.excelTableStyle as ExcelTableStyle,
    });
  }

  async saveUserPreferences(): Promise<void> {
    if (this.form.invalid) return;
    this.saving.set(true);
    try {
      await firstValueFrom(this.authService.updatePreferences(this.form.getRawValue()));
      this.notification.success(this.i18n.t('settings.saved'));
    } catch (error) {
      const msg = apiErrorMessage(error, this.i18n);
      this.notification.error(msg);
    } finally {
      this.saving.set(false);
    }
  }

  async saveAppSettings(): Promise<void> {
    if (this.appSettingsForm.invalid) {
      this.appSettingsForm.markAllAsTouched();
      return;
    }
    this.appSettingsSaving.set(true);
    try {
      const raw = this.appSettingsForm.getRawValue();
      const saved = await firstValueFrom(this.authService.updateAppSettings(raw));
      this.appSettingsForm.patchValue({
        sessionTimeoutMinutes: saved.sessionTimeoutMinutes,
        sessionTimeoutEnabled: saved.sessionTimeoutEnabled,
        showReportPresets: saved.showReportPresets,
        minPasswordLength: saved.minPasswordLength ?? 8,
        requireUppercase: saved.requireUppercase ?? false,
        requireLowercase: saved.requireLowercase ?? false,
        requireNumbers: saved.requireNumbers ?? false,
        requireSpecialChars: saved.requireSpecialChars ?? false,
        disallowSpaces: saved.disallowSpaces ?? false,
        maxPasswordLength: saved.maxPasswordLength ?? 128,
        passwordExpiryDays: saved.passwordExpiryDays ?? 0,
        passwordHistoryCount: saved.passwordHistoryCount ?? 0,
      });
      this.notification.success(this.i18n.t('settings.saveAllSystemSettings', undefined, 'تم حفظ جميع الإعدادات بنجاح.'));
    } catch (error) {
      const msg = apiErrorMessage(error, this.i18n);
      this.notification.error(msg);
    } finally {
      this.appSettingsSaving.set(false);
    }
  }

  async saveAll(): Promise<void> {
    await this.saveUserPreferences();
    if (this.authService.hasAnyRole(['SUPER_ADMIN', 'ADMIN'])) {
      await this.saveAppSettings();
    }
  }

  hasUnsavedChanges(): boolean {
    return this.form.dirty || this.appSettingsForm.dirty;
  }

  private async loadAppSettings(): Promise<void> {
    this.appSettingsLoading.set(true);
    try {
      const settings = await firstValueFrom(this.authService.appSettings());
      this.appSettingsForm.patchValue({
        sessionTimeoutMinutes: settings.sessionTimeoutMinutes,
        sessionTimeoutEnabled: settings.sessionTimeoutEnabled ?? true,
        showReportPresets: settings.showReportPresets ?? true,
        minPasswordLength: settings.minPasswordLength ?? 8,
        requireUppercase: settings.requireUppercase ?? false,
        requireLowercase: settings.requireLowercase ?? false,
        requireNumbers: settings.requireNumbers ?? false,
        requireSpecialChars: settings.requireSpecialChars ?? false,
        disallowSpaces: settings.disallowSpaces ?? false,
        maxPasswordLength: settings.maxPasswordLength ?? 128,
        passwordExpiryDays: settings.passwordExpiryDays ?? 0,
        passwordHistoryCount: settings.passwordHistoryCount ?? 0,
      });
    } catch (error) {
      this.appSettingsError.set(apiErrorMessage(error, this.i18n));
    } finally {
      this.appSettingsLoading.set(false);
    }
  }
}
