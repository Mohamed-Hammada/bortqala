import { Injectable, inject } from '@angular/core';
import { Router } from '@angular/router';
import { App } from '@capacitor/app';
import { Capacitor } from '@capacitor/core';
import { I18nService } from '../i18n.service';

/**
 * WP-14 AC-5: hardware back button walks router history; leaving the app only from the
 * root screen and only after an explicit confirmation.
 */
@Injectable({ providedIn: 'root' })
export class BackButtonService {
  private readonly router = inject(Router);
  private readonly i18n = inject(I18nService);
  private registered = false;

  register(): void {
    if (this.registered || !Capacitor.isNativePlatform()) return;
    this.registered = true;
    void App.addListener('backButton', () => {
      const url = this.router.url;
      if (url.startsWith('/dashboard') || url === '/' || url.startsWith('/login') || url.startsWith('/server-setup')) {
        const accepted = window.confirm(this.i18n.t('native.exitMessage'));
        if (accepted) void App.exitApp();
        return;
      }
      window.history.back();
    });
  }
}
