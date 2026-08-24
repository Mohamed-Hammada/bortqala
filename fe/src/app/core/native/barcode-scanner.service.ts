import { Injectable } from '@angular/core';
import { Capacitor, registerPlugin } from '@capacitor/core';

export interface BarcodeScannerPlugin {
  scan(): Promise<{ value: string | null; cancelled: boolean }>;
}

/**
 * WP-14 AC-4: barcode scanning via Google's play-services-code-scanner, wrapped by the
 * custom `BarcodeScanner` Android plugin. On web (PWA) it degrades to a manual-entry prompt
 * so the lookup flow stays testable in the browser.
 */
@Injectable({ providedIn: 'root' })
export class BarcodeScannerService {
  private readonly plugin: BarcodeScannerPlugin | null = Capacitor.isNativePlatform()
    ? registerPlugin<BarcodeScannerPlugin>('BarcodeScanner')
    : null;

  async scan(manualFallback: string): Promise<string | null> {
    if (!this.plugin) return window.prompt(manualFallback)?.trim() || null;
    try {
      const result = await this.plugin.scan();
      return result.cancelled ? null : result.value;
    } catch {
      return null;
    }
  }
}
