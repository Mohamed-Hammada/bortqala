import {
  ApplicationConfig,
  inject,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
} from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { isDevMode } from '@angular/core';
import { provideServiceWorker } from '@angular/service-worker';
import { Router } from '@angular/router';
import { authInterceptor } from './core/auth/auth.interceptor';
import { AuthService } from './core/auth/auth.service';
import { globalLoadingInterceptor } from './core/global-loading/global-loading.interceptor';
import { serverUrlInterceptor } from './core/native/server-url.interceptor';
import { NativeBridgeService } from './core/native/native-bridge.service';
import { BackButtonService } from './core/native/back-button.service';
import { BiometricGateService } from './core/native/biometric-gate.service';
import { OfflineOutboxService } from './core/native/offline-outbox.service';
import { I18nService } from './core/i18n.service';
import { SystemStatusService } from './core/system-status.service';

import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(withInterceptors([serverUrlInterceptor, globalLoadingInterceptor, authInterceptor])),
    provideServiceWorker('ngsw-worker.js', {
      enabled: !isDevMode(),
      registrationStrategy: 'registerWhenStable:30000',
    }),
    provideAppInitializer(() => inject(SystemStatusService).initialize()),
    provideAppInitializer(() => {
      const i18nService = inject(I18nService);
      return i18nService.use(i18nService.locale());
    }),
    provideAppInitializer(() => inject(AuthService).tryRefresh()),
    provideAppInitializer(async () => {
      // WP-14: inside the native shell resolve the stored company server first; web is a no-op.
      const nativeBridge = inject(NativeBridgeService);
      if (!nativeBridge.isNative) return;
      await nativeBridge.loadServerUrl();
      inject(BackButtonService).register();
      await inject(BiometricGateService).register();
      inject(OfflineOutboxService).autoFlush();
      if (!nativeBridge.configuredServerUrl()) {
        await inject(Router).navigateByUrl('/server-setup');
      }
    }),
  ],
};
