import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { apiErrorMessage } from '../../core/api-error';
import { AuthService } from '../../core/auth/auth.service';
import { I18nService, SupportedLocale } from '../../core/i18n.service';

@Component({
  selector: 'app-login-page',
  imports: [ReactiveFormsModule],
  templateUrl: './login.page.html',
  styleUrl: './login.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LoginPage {
  readonly authService = inject(AuthService);
  readonly i18n = inject(I18nService);
  private readonly router = inject(Router);
  private readonly activatedRoute = inject(ActivatedRoute);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly sessionExpired =
    this.activatedRoute.snapshot.queryParamMap.get('reason') === 'session-expired';
  readonly showPassword = signal(false);
  readonly credentialHint = signal('DEMO / admin / Admin@12345');
  readonly form = new FormGroup({
    appCode: new FormControl('DEMO', { nonNullable: true, validators: [Validators.required] }),
    username: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    password: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
  });

  constructor() {
    if (this.authService.authenticated()) void this.router.navigate(['/dashboard']);
    else void this.attemptDemoLogin();
    void this.loadDesktopCredentials();
  }

  private async attemptDemoLogin(): Promise<void> {
    const secret = this.activatedRoute.snapshot.queryParamMap.get('my_secret');
    if (!secret || this.loading()) return;
    this.loading.set(true);
    this.error.set(null);
    try {
      const session = await firstValueFrom(this.authService.demoLogin(secret));
      await this.i18n.use(session.preferences.locale);
      document.documentElement.lang = this.i18n.locale().startsWith('ar') ? 'ar' : 'en';
      document.documentElement.dir = this.i18n.locale().startsWith('ar') ? 'rtl' : 'ltr';
      await this.router.navigate(
        session.mustChangePassword ? ['/change-password'] : ['/dashboard'],
        { replaceUrl: true },
      );
    } catch (error) {
      await this.router.navigate([], { queryParams: { my_secret: null }, replaceUrl: true });
      this.error.set(apiErrorMessage(error, this.i18n));
    } finally {
      this.loading.set(false);
    }
  }

  private async loadDesktopCredentials(): Promise<void> {
    const tauri = (
      window as typeof window & {
        __TAURI__?: {
          core?: {
            invoke<T>(command: string): Promise<T>;
          };
        };
      }
    ).__TAURI__;
    if (!tauri?.core) return;
    try {
      const credentials = await tauri.core.invoke<{
        appCode: string;
        username: string;
        password: string;
      }>('initial_credentials');
      this.form.patchValue({
        appCode: credentials.appCode,
        username: credentials.username,
        password: credentials.password,
      });
      this.credentialHint.set(
        `${credentials.appCode} / ${credentials.username} / ${credentials.password}`,
      );
    } catch {
      // A normal browser build has no desktop bridge and keeps the development credentials.
    }
  }

  changeLanguage(locale: SupportedLocale): void {
    void this.i18n.use(locale);
    document.documentElement.lang = locale.startsWith('ar') ? 'ar' : 'en';
    document.documentElement.dir = locale.startsWith('ar') ? 'rtl' : 'ltr';
  }

  async submit(): Promise<void> {
    if (this.form.invalid || this.loading()) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    try {
      const session = await firstValueFrom(
        this.authService.login(
          this.form.controls.appCode.value,
          this.form.controls.username.value,
          this.form.controls.password.value,
        ),
      );
      await this.i18n.use(session.preferences.locale);
      document.documentElement.lang = this.i18n.locale().startsWith('ar') ? 'ar' : 'en';
      document.documentElement.dir = this.i18n.locale().startsWith('ar') ? 'rtl' : 'ltr';
      await this.router.navigate(session.mustChangePassword ? ['/change-password'] : ['/dashboard']);
    } catch (error) {
      this.error.set(
        error instanceof HttpErrorResponse && error.status === 401
          ? this.i18n.t('login.invalidCredentials')
          : apiErrorMessage(error, this.i18n),
      );
    } finally {
      this.loading.set(false);
    }
  }
}
