import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { apiErrorMessage } from '../../core/api-error';
import { AuthService } from '../../core/auth/auth.service';
import { ExcelTableStyle, NotificationPreferences, TableDensity, ThemePreference } from '../../core/auth/auth.models';
import { I18nService } from '../../core/i18n.service';
import { LANDING_PAGE_ITEMS, canAccessNavigationItem } from '../../core/navigation/app-navigation';
import { NotificationService } from '../../core/notification.service';
import { WebPushService } from '../../core/notification-center/web-push.service';
import { ActivatedRoute, Router } from '@angular/router';
import { ShortcutSettingsComponent } from './shortcuts/shortcut-settings.component';
import { MOVED_SETTINGS_TAB_ROUTES, SettingsTab, isSettingsTab } from './settings-navigation';
import { SettingsSubmenuComponent } from './settings-submenu.component';


const NOTIFICATION_KEY = 'bemo_notification_prefs';

function loadNotificationPrefs(): NotificationPreferences {
  try {
    const raw = localStorage.getItem(NOTIFICATION_KEY);
    if (raw) return JSON.parse(raw) as NotificationPreferences;
  } catch { /* ignore */ }
  return { emailApprovals: true, emailPayroll: false, pushApprovals: true, pushPayroll: false };
}

function saveNotificationPrefs(prefs: NotificationPreferences): void {
  localStorage.setItem(NOTIFICATION_KEY, JSON.stringify(prefs));
}

import { BusinessVerticalSetupComponent } from './business-vertical-setup/business-vertical-setup.component';

@Component({
  selector: 'app-settings-page',
  standalone: true,
  imports: [ReactiveFormsModule, ShortcutSettingsComponent, SettingsSubmenuComponent, BusinessVerticalSetupComponent],
  templateUrl: './settings.page.html',
  styleUrl: './settings.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SettingsPage {
  readonly authService = inject(AuthService);
  readonly i18n = inject(I18nService);
  readonly notification = inject(NotificationService);
  readonly webPush = inject(WebPushService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly activeTab = signal<SettingsTab>('appearance');

  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  readonly saved = signal(false);
  readonly appSettingsLoading = signal(false);
  readonly appSettingsSaving = signal(false);
  readonly appSettingsSaved = signal(false);
  readonly appSettingsError = signal<string | null>(null);
  readonly confirmAction = signal<{ message: string; onConfirm: () => void } | null>(null);
  readonly desktop = typeof window !== 'undefined' && '__TAURI__' in window;
  readonly licenseMessage = signal<string | null>(null);

  readonly showFavorites = signal(this.authService.preferences().showFavorites);
  readonly showRecentlyUsed = signal(this.authService.preferences().showRecentlyUsed);
  readonly maxRecentlyUsed = signal(this.authService.preferences().maxRecentlyUsed);
  readonly notificationPrefs = signal<NotificationPreferences>(loadNotificationPrefs());

  readonly availablePages = LANDING_PAGE_ITEMS
    .filter((item) => {
      const user = this.authService.user();
      return !!user && canAccessNavigationItem(
        item,
        user.roles,
        (menuId) => this.authService.hasMenuAccess(menuId),
      );
    })
    .map((item) => ({ path: item.path, labelKey: item.labelKey }));

  async toggleShowFavorites(val: boolean) {
    this.showFavorites.set(val);
    await this.saveNavigationPreferences(this.i18n.t('settings.menuSavedFavorites'));
  }

  async toggleShowRecentlyUsed(val: boolean) {
    this.showRecentlyUsed.set(val);
    await this.saveNavigationPreferences(this.i18n.t('settings.menuSavedRecent'));
  }

  async setMaxRecentlyUsed(value: number): Promise<void> {
    this.maxRecentlyUsed.set(Math.max(1, Math.min(20, value || 1)));
    await this.saveNavigationPreferences(this.i18n.t('settings.menuSavedRecent'));
  }

  async clearRecentHistory(): Promise<void> {
    await this.saveNavigationPreferences(this.i18n.t('settings.menuCleared'), []);
  }

  async resetFavorites(): Promise<void> {
    await this.saveNavigationPreferences(this.i18n.t('settings.menuCleared'), undefined, []);
  }

  updateNotificationPrefs(key: keyof NotificationPreferences, value: boolean) {
    this.notificationPrefs.update((current) => ({ ...current, [key]: value }));
  }

  async toggleWebPush(enabled: boolean): Promise<void> {
    try {
      if (enabled) await this.webPush.enable(this.notificationPrefs());
      else await this.webPush.disable();
      this.notification.success(this.i18n.t(enabled ? 'settings.browserPushEnabled' : 'settings.browserPushDisabled'));
    } catch (error) {
      const key = this.webPush.permissionDenied() ? 'settings.browserPushPermissionDenied'
        : error instanceof Error && error.message === 'WEB_PUSH_NOT_CONFIGURED' ? 'settings.browserPushNotConfigured' : 'common.error';
      this.notification.error(this.i18n.t(key));
    }
  }

  async sendTestPush(): Promise<void> {
    try { await this.webPush.sendTest(); this.notification.success(this.i18n.t('settings.browserPushTest')); }
    catch (error) { this.notification.error(apiErrorMessage(error, this.i18n)); }
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
    defaultPage: [this.authService.preferences().defaultPage ?? '/dashboard', Validators.required],
  });

  readonly appSettingsForm = this.formBuilder.nonNullable.group({
    sessionTimeoutMinutes: [480, [Validators.required, Validators.min(5), Validators.max(10_080)]],
    sessionTimeoutEnabled: [true, Validators.required],
    showReportPresets: [true, Validators.required],
    attendanceAnomalyThresholdPercent: [70, [Validators.required, Validators.min(1), Validators.max(100)]],
    automaticProcurementNumbering: [true, Validators.required],
    automaticDocumentNumbering: [true, Validators.required],
    adminDashboardCustomizationEnabled: [true, Validators.required],
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
    const tabParam = this.route.snapshot.queryParamMap.get('tab');
    const movedRoute = tabParam ? MOVED_SETTINGS_TAB_ROUTES[tabParam] : undefined;
    if (movedRoute) {
      void this.router.navigateByUrl(movedRoute);
      return;
    }

    const adminTab = tabParam === 'session' || tabParam === 'security' || tabParam === 'business';
    if (isSettingsTab(tabParam) && (!adminTab || this.authService.hasAnyRole(['SUPER_ADMIN', 'ADMIN']))) {
      this.activeTab.set(tabParam);
    }
    if (this.authService.hasAnyRole(['SUPER_ADMIN', 'ADMIN'])) void this.loadAppSettings();
  }

  setTab(tab: SettingsTab): void {
    this.activeTab.set(tab);
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { tab },
      queryParamsHandling: 'merge',
      replaceUrl: true,
    });
  }

  releaseLicense(): void {
    if (!this.desktop) return;
    this.confirmAction.set({
      message: this.i18n.t('settings.licenseReleaseConfirm'),
      onConfirm: () => {
        this.confirmAction.set(null);
        this.executeReleaseLicense();
      },
    });
  }

  private async executeReleaseLicense(): Promise<void> {
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
      defaultPage: prefs.defaultPage ?? '/dashboard',
    });
    this.notificationPrefs.set(loadNotificationPrefs());
  }

  async saveUserPreferences(): Promise<void> {
    if (this.form.invalid) return;
    this.saving.set(true);
    try {
      await firstValueFrom(this.authService.updatePreferences(this.form.getRawValue()));
      saveNotificationPrefs(this.notificationPrefs());
      await this.webPush.syncPreferences(this.notificationPrefs());
      this.form.markAsPristine();
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
        attendanceAnomalyThresholdPercent: saved.attendanceAnomalyThresholdPercent ?? 70,
        automaticProcurementNumbering: saved.automaticProcurementNumbering ?? true,
        automaticDocumentNumbering: saved.automaticDocumentNumbering ?? true,
        adminDashboardCustomizationEnabled: saved.adminDashboardCustomizationEnabled ?? true,
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
      this.appSettingsForm.markAsPristine();
      this.notification.success(this.i18n.t('settings.saveAllSystemSettings', undefined));
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
        attendanceAnomalyThresholdPercent: settings.attendanceAnomalyThresholdPercent ?? 70,
        automaticProcurementNumbering: settings.automaticProcurementNumbering ?? true,
        automaticDocumentNumbering: settings.automaticDocumentNumbering ?? true,
        adminDashboardCustomizationEnabled: settings.adminDashboardCustomizationEnabled ?? true,
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
      this.appSettingsForm.markAsPristine();
    } catch (error) {
      this.appSettingsError.set(apiErrorMessage(error, this.i18n));
    } finally {
      this.appSettingsLoading.set(false);
    }
  }

  private async saveNavigationPreferences(message: string, recentMenuIds?: string[], favoriteMenuIds?: string[]): Promise<void> {
    const current = this.authService.preferences();
    try {
      await firstValueFrom(this.authService.updateNavigationPreferences({
        showFavorites: this.showFavorites(),
        showRecentlyUsed: this.showRecentlyUsed(),
        maxRecentlyUsed: this.maxRecentlyUsed(),
        favoriteMenuIds: favoriteMenuIds ?? current.favoriteMenuIds,
        recentMenuIds: recentMenuIds ?? current.recentMenuIds,
      }));
      this.notification.success(message);
    } catch (error) {
      this.notification.error(apiErrorMessage(error, this.i18n));
      this.showFavorites.set(current.showFavorites);
      this.showRecentlyUsed.set(current.showRecentlyUsed);
      this.maxRecentlyUsed.set(current.maxRecentlyUsed);
    }
  }
}
