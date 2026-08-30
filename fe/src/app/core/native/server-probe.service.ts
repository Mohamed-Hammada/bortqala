import { Injectable, inject } from '@angular/core';
import { CapacitorHttp, HttpResponse } from '@capacitor/core';
import { firstValueFrom } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { I18nService } from '../i18n.service';
import { NativeBridgeService, normalizeServerUrl } from './native-bridge.service';

/**
 * WP-14 AC-1: first-launch server picker validation.
 * Probes `GET {url}/api/v1/i18n/ar-EG` — on native the probe bypasses Angular HTTP so it
 * can target a not-yet-configured server; on web the same check runs against the current origin.
 */
@Injectable({ providedIn: 'root' })
export class ServerProbeService {
  private readonly http = inject(HttpClient);
  private readonly i18n = inject(I18nService);
  private readonly native = inject(NativeBridgeService);

  async probe(rawUrl: string): Promise<{ ok: boolean; errorKey?: string }> {
    const url = normalizeServerUrl(rawUrl);
    if (!/^https?:\/\/.+/i.test(url)) return { ok: false, errorKey: 'native.probeInvalidUrl' };
    if (this.native.isNative) {
      try {
        const response: HttpResponse = await CapacitorHttp.get({ url: `${url}/api/v1/i18n/ar-EG`, readTimeout: 8000, connectTimeout: 8000 });
        return response.status >= 200 && response.status < 300
          ? { ok: true }
          : { ok: false, errorKey: 'native.probeNotBemoServer' };
      } catch {
        return { ok: false, errorKey: 'native.probeUnreachable' };
      }
    }
    try {
      await firstValueFrom(this.http.get(`/api/v1/i18n/${this.i18n.locale()}`));
      return { ok: true };
    } catch {
      return { ok: false, errorKey: 'native.probeUnreachable' };
    }
  }
}
