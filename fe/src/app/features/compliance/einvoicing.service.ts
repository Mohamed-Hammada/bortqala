import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import {
  EinvoicingProviderInfo,
  EinvoicingSettings,
  SaveEinvoicingSettingsRequest,
} from './einvoicing.models';

@Injectable({ providedIn: 'root' })
export class EinvoicingService {
  private readonly http = inject(HttpClient);

  getSettings(): Promise<EinvoicingSettings | null> {
    return firstValueFrom(this.http.get<EinvoicingSettings>('/api/v1/einvoicing/settings'));
  }

  saveSettings(req: SaveEinvoicingSettingsRequest): Promise<EinvoicingSettings> {
    return firstValueFrom(this.http.put<EinvoicingSettings>('/api/v1/einvoicing/settings', req));
  }

  listProviders(): Promise<EinvoicingProviderInfo[]> {
    return firstValueFrom(this.http.get<EinvoicingProviderInfo[]>('/api/v1/einvoicing/providers'));
  }
}