import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { apiErrorMessage } from '../../core/api-error';
import { AuthService } from '../../core/auth/auth.service';
import { ExcelTableStyle, TableDensity, ThemePreference } from '../../core/auth/auth.models';
import { I18nService } from '../../core/i18n.service';

@Component({
  selector: 'app-settings-page',
  imports: [ReactiveFormsModule],
  templateUrl: './settings.page.html',
  styleUrl: './settings.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SettingsPage {
  readonly authService = inject(AuthService);
  readonly i18n = inject(I18nService);
  private readonly formBuilder = inject(FormBuilder);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  readonly saved = signal(false);
  readonly appSettingsLoading = signal(false);
  readonly appSettingsSaving = signal(false);
  readonly appSettingsSaved = signal(false);
  readonly appSettingsError = signal<string | null>(null);
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
  });

  constructor() {
    if (this.authService.hasAnyRole(['ADMIN'])) void this.loadAppSettings();
  }

  preview(): void {
    if (this.form.valid) {
      const value = this.form.getRawValue();
      document.documentElement.dataset['theme'] = value.theme.toLowerCase();
      document.documentElement.dataset['density'] = value.tableDensity.toLowerCase();
      document.documentElement.lang = value.locale.startsWith('ar') ? 'ar' : 'en';
      document.documentElement.dir = value.locale.startsWith('ar') ? 'rtl' : 'ltr';
      void this.i18n.use(value.locale);
    }
  }

  async save(): Promise<void> {
    if (this.form.invalid) return;
    this.saving.set(true);
    this.error.set(null);
    this.saved.set(false);
    try {
      await firstValueFrom(this.authService.updatePreferences(this.form.getRawValue()));
      this.saved.set(true);
    } catch (error) {
      this.error.set(apiErrorMessage(error));
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
    this.appSettingsSaved.set(false);
    this.appSettingsError.set(null);
    try {
      const saved = await firstValueFrom(
        this.authService.updateAppSettings(
          this.appSettingsForm.controls.sessionTimeoutMinutes.value,
        ),
      );
      this.appSettingsForm.controls.sessionTimeoutMinutes.setValue(saved.sessionTimeoutMinutes);
      this.appSettingsSaved.set(true);
    } catch (error) {
      this.appSettingsError.set(apiErrorMessage(error));
    } finally {
      this.appSettingsSaving.set(false);
    }
  }

  private async loadAppSettings(): Promise<void> {
    this.appSettingsLoading.set(true);
    try {
      const settings = await firstValueFrom(this.authService.appSettings());
      this.appSettingsForm.controls.sessionTimeoutMinutes.setValue(settings.sessionTimeoutMinutes);
    } catch (error) {
      this.appSettingsError.set(apiErrorMessage(error));
    } finally {
      this.appSettingsLoading.set(false);
    }
  }
}
