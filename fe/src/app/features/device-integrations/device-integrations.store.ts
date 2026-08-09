import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { apiErrorMessage } from '../../core/api-error';
import { I18nService } from '../../core/i18n.service';
import {
  DeviceIntegration,
  DeviceIntegrationRequest,
  HubHealth,
  ProbeResult,
  RouteRequest,
  RouteResolution,
  SupplierInfo,
  SyncResult,
} from './device-integrations.models';

@Injectable()
export class DeviceIntegrationsStore {
  private readonly http = inject(HttpClient);
  private readonly i18n = inject(I18nService);

  readonly devices = signal<DeviceIntegration[]>([]);
  readonly suppliers = signal<SupplierInfo[]>([]);
  readonly health = signal<HubHealth | null>(null);
  readonly loading = signal(false);
  readonly resolving = signal(false);
  readonly error = signal<string | null>(null);

  async load(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      this.devices.set(await firstValueFrom(this.http.get<DeviceIntegration[]>('/api/v1/device-integrations')));
      const [suppliersResult, healthResult] = await Promise.allSettled([
        firstValueFrom(this.http.get<SupplierInfo[]>('/api/v1/device-integrations/suppliers')),
        firstValueFrom(this.http.get<HubHealth>('/api/v1/device-integrations/health')),
      ]);
      if (suppliersResult.status === 'fulfilled') this.suppliers.set(suppliersResult.value);
      if (healthResult.status === 'fulfilled') this.health.set(healthResult.value);
      if (suppliersResult.status === 'rejected' || healthResult.status === 'rejected') {
        const reason = suppliersResult.status === 'rejected' ? suppliersResult.reason : healthResult.status === 'rejected' ? healthResult.reason : null;
        this.error.set(apiErrorMessage(reason, this.i18n));
      }
    } catch (error) {
      this.error.set(apiErrorMessage(error, this.i18n));
    } finally {
      this.loading.set(false);
    }
  }

  async resolve(payload: RouteRequest): Promise<RouteResolution | null> {
    this.resolving.set(true);
    this.error.set(null);
    try {
      return await firstValueFrom(
        this.http.post<RouteResolution>('/api/v1/device-integrations/resolve', payload),
      );
    } catch (error) {
      this.error.set(apiErrorMessage(error, this.i18n));
      return null;
    } finally {
      this.resolving.set(false);
    }
  }

  async save(payload: DeviceIntegrationRequest, id?: string): Promise<DeviceIntegration | null> {
    this.loading.set(true);
    this.error.set(null);
    try {
      const saved = await firstValueFrom(
        id
          ? this.http.put<DeviceIntegration>(`/api/v1/device-integrations/${id}`, payload)
          : this.http.post<DeviceIntegration>('/api/v1/device-integrations', payload),
      );
      await this.load();
      return saved;
    } catch (error) {
      this.error.set(apiErrorMessage(error, this.i18n));
      return null;
    } finally {
      this.loading.set(false);
    }
  }

  async probe(id: string): Promise<ProbeResult | null> {
    this.error.set(null);
    try {
      const result = await firstValueFrom(
        this.http.post<ProbeResult>(`/api/v1/device-integrations/${id}/probe`, {}),
      );
      await this.load();
      return result;
    } catch (error) {
      this.error.set(apiErrorMessage(error, this.i18n));
      return null;
    }
  }

  async sync(id: string): Promise<SyncResult | null> {
    this.error.set(null);
    try {
      return await firstValueFrom(
        this.http.post<SyncResult>(`/api/v1/device-integrations/${id}/sync`, {}),
      );
    } catch (error) {
      this.error.set(apiErrorMessage(error, this.i18n));
      return null;
    }
  }
}
