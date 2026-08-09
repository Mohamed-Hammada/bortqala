import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { apiErrorMessage } from '../../core/api-error';
import { AuthService } from '../../core/auth/auth.service';
import { I18nService } from '../../core/i18n.service';

@Component({
  selector: 'app-change-password-page',
  imports: [ReactiveFormsModule],
  templateUrl: './change-password.page.html',
  styleUrl: './change-password.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ChangePasswordPage {
  readonly authService = inject(AuthService);
  readonly i18n = inject(I18nService);
  private readonly router = inject(Router);
  readonly loading = signal(false);
  readonly success = signal(false);
  readonly error = signal<string | null>(null);
  readonly showCurrent = signal(false);
  readonly showNew = signal(false);
  readonly showConfirm = signal(false);
  readonly form = new FormGroup({
    currentPassword: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    newPassword: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(8), Validators.pattern(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z\d]).+$/)],
    }),
    confirmPassword: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
  });

  get newPasswordError(): string | null {
    const control = this.form.controls.newPassword;
    if (!control.dirty && !control.touched) return null;
    if (control.hasError('required')) return this.i18n.t('changePassword.new');
    if (control.hasError('minlength')) return this.i18n.t('changePassword.tooShort');
    if (control.hasError('pattern')) return this.i18n.t('changePassword.weak');
    return null;
  }

  get confirmError(): string | null {
    const confirm = this.form.controls.confirmPassword;
    if (!confirm.dirty && !confirm.touched) return null;
    if (confirm.hasError('required')) return this.i18n.t('changePassword.confirm');
    if (this.form.controls.newPassword.value !== confirm.value) return this.i18n.t('changePassword.mismatch');
    return null;
  }

  changeLanguage(locale: 'ar-EG' | 'en-US'): void {
    void this.i18n.use(locale, this.authService.app()?.id ?? null);
    document.documentElement.lang = locale.startsWith('ar') ? 'ar' : 'en';
    document.documentElement.dir = locale.startsWith('ar') ? 'rtl' : 'ltr';
  }

  async submit(): Promise<void> {
    if (this.loading() || this.success()) return;
    this.form.markAllAsTouched();
    if (this.form.invalid || this.newPasswordError || this.confirmError) return;
    this.loading.set(true);
    this.error.set(null);
    try {
      await firstValueFrom(
        this.authService.changePassword(
          this.form.controls.currentPassword.value,
          this.form.controls.newPassword.value,
        ),
      );
      this.success.set(true);
      setTimeout(() => void this.router.navigate(['/login']), 1500);
    } catch (error) {
      if (error instanceof HttpErrorResponse && error.status === 401) {
        this.authService.expireSession();
        void this.router.navigate(['/login']);
        return;
      }
      this.error.set(apiErrorMessage(error, this.i18n));
    } finally {
      this.loading.set(false);
    }
  }

  logout(): void {
    this.authService.logout();
    void this.router.navigate(['/login']);
  }
}
