import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { I18nService } from '../../core/i18n.service';
import { NotificationService } from '../../core/notification.service';
import { ModalDialogComponent } from '../../shared/ui/modal-dialog/modal-dialog.component';
import {
  DeviceIntegration,
  DeviceIntegrationRequest,
  RouteCandidate,
  RouteRequest,
  RouteResolution,
} from './device-integrations.models';
import { DeviceIntegrationsStore } from './device-integrations.store';

@Component({
  selector: 'app-device-integrations-page',
  imports: [ReactiveFormsModule, ModalDialogComponent],
  providers: [DeviceIntegrationsStore],
  templateUrl: './device-integrations.page.html',
  styleUrl: './device-integrations.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DeviceIntegrationsPage {
  readonly store = inject(DeviceIntegrationsStore);
  readonly i18n = inject(I18nService);
  private readonly notifications = inject(NotificationService);

  readonly drawerOpen = signal(false);
  readonly editingId = signal<string | null>(null);
  readonly resolution = signal<RouteResolution | null>(null);
  readonly selectedRoute = signal<string>('');
  readonly workingId = signal<string | null>(null);
  readonly showAdvanced = signal(false);
  readonly filter = signal('');

  readonly visibleDevices = computed(() => {
    const query = this.filter().trim().toLocaleLowerCase();
    if (!query) return this.store.devices();
    return this.store.devices().filter((device) =>
      `${device.name} ${device.vendor} ${device.model} ${device.route}`.toLocaleLowerCase().includes(query),
    );
  });

  readonly form = new FormGroup({
    name: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    vendor: new FormControl('zkteco', { nonNullable: true, validators: [Validators.required] }),
    model: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    serialNumber: new FormControl('', { nonNullable: true }),
    firmwareVersion: new FormControl('', { nonNullable: true }),
    platformVersion: new FormControl('', { nonNullable: true }),
    serverVersion: new FormControl('', { nonNullable: true }),
    osName: new FormControl('', { nonNullable: true }),
    architecture: new FormControl('', { nonNullable: true }),
    host: new FormControl('', { nonNullable: true }),
    port: new FormControl<number | null>(null, { validators: [Validators.min(1), Validators.max(65535)] }),
    baseUrl: new FormControl('', { nonNullable: true }),
    username: new FormControl('', { nonNullable: true }),
    password: new FormControl('', { nonNullable: true }),
    sdkVersionsText: new FormControl('', { nonNullable: true }),
    apiVersionsText: new FormControl('', { nonNullable: true }),
    capabilityHintsText: new FormControl('', { nonNullable: true }),
    authMode: new FormControl('', { nonNullable: true }),
    authHeader: new FormControl('', { nonNullable: true }),
    punchPath: new FormControl('', { nonNullable: true }),
    punchMethod: new FormControl('GET', { nonNullable: true }),
    punchArrayPath: new FormControl('', { nonNullable: true }),
    sinceParam: new FormControl('', { nonNullable: true }),
    optionsText: new FormControl('', { nonNullable: true }),
    enabled: new FormControl(true, { nonNullable: true }),
    syncIntervalMinutes: new FormControl(15, {
      nonNullable: true,
      validators: [Validators.required, Validators.min(1), Validators.max(1440)],
    }),
  });

  constructor() {
    void this.store.load();
  }

  openNew(): void {
    this.editingId.set(null);
    this.resolution.set(null);
    this.selectedRoute.set('');
    this.showAdvanced.set(false);
    this.form.reset({
      name: '', vendor: 'zkteco', model: '', serialNumber: '', firmwareVersion: '', platformVersion: '',
      serverVersion: '', osName: '', architecture: '', host: '', port: null, baseUrl: '', username: '',
      password: '', sdkVersionsText: '', apiVersionsText: '', capabilityHintsText: '', authMode: '',
      authHeader: '', punchPath: '', punchMethod: 'GET', punchArrayPath: '', sinceParam: '', optionsText: '',
      enabled: true, syncIntervalMinutes: 15,
    });
    this.drawerOpen.set(true);
  }

  openEdit(device: DeviceIntegration): void {
    this.editingId.set(device.id);
    this.selectedRoute.set(device.route);
    this.resolution.set(null);
    this.showAdvanced.set(true);
    this.form.reset({
      name: device.name,
      vendor: device.vendor,
      model: device.model,
      serialNumber: device.serialNumber ?? '',
      firmwareVersion: device.firmwareVersion ?? '',
      platformVersion: device.platformVersion ?? '',
      serverVersion: device.serverVersion ?? '',
      osName: device.osName ?? '',
      architecture: device.architecture ?? '',
      host: device.host ?? '',
      port: device.port,
      baseUrl: device.baseUrl ?? '',
      username: device.username ?? '',
      password: '',
      sdkVersionsText: this.keyValueText(device.sdkVersions),
      apiVersionsText: this.keyValueText(device.apiVersions),
      capabilityHintsText: device.capabilityHints.join(', '),
      authMode: String(device.options['auth'] ?? ''),
      authHeader: String(device.options['auth_header'] ?? ''),
      punchPath: String(device.options['punch_path'] ?? ''),
      punchMethod: String(device.options['punch_method'] ?? 'GET'),
      punchArrayPath: String(device.options['punch_array_path'] ?? ''),
      sinceParam: String(device.options['since_param'] ?? ''),
      optionsText: this.remainingOptions(device.options),
      enabled: device.enabled,
      syncIntervalMinutes: device.syncIntervalMinutes,
    });
    this.drawerOpen.set(true);
    void this.resolveRoutes();
  }

  async resolveRoutes(): Promise<void> {
    if (!this.form.controls.vendor.value || !this.form.controls.model.value.trim()) {
      this.form.controls.model.markAsTouched();
      return;
    }
    const result = await this.store.resolve(this.routePayload());
    this.resolution.set(result);
    if (result) {
      const current = this.selectedRoute();
      const stillValid = result.candidates.some((candidate) => candidate.route === current);
      if (!stillValid) this.selectedRoute.set(result.preferredRoute ?? '');
    }
  }

  selectRoute(candidate: RouteCandidate): void {
    this.selectedRoute.set(candidate.route);
  }

  async save(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    if (!this.resolution()) await this.resolveRoutes();
    const route = this.selectedRoute();
    const selected = this.resolution()?.candidates.find((candidate) => candidate.route === route);
    if (!selected || selected.status !== 'COMPATIBLE') {
      this.notifications.error(this.i18n.t('deviceIntegrations.routeMustBeCompatible'));
      return;
    }
    const saved = await this.store.save(this.devicePayload(route), this.editingId() ?? undefined);
    if (saved) {
      this.notifications.success(this.i18n.t('deviceIntegrations.saved'));
      this.drawerOpen.set(false);
    }
  }

  async probe(device: DeviceIntegration): Promise<void> {
    this.workingId.set(device.id);
    const result = await this.store.probe(device.id);
    this.workingId.set(null);
    if (result?.ok) {
      this.notifications.success(this.i18n.t('deviceIntegrations.probeSuccess'));
    } else if (result) {
      this.notifications.error(result.detail);
    }
  }

  async sync(device: DeviceIntegration): Promise<void> {
    this.workingId.set(device.id);
    const result = await this.store.sync(device.id);
    this.workingId.set(null);
    if (result) {
      this.notifications.success(this.i18n.t('deviceIntegrations.syncSuccess', { count: result.importedRows }));
    }
  }

  implementationReady(status: string): boolean {
    const value = (status ?? '').toLocaleLowerCase();
    return value.includes('implemented') || value.includes('functional') || value.includes('http-client');
  }

  statusClass(status: string): string {
    const value = (status ?? '').toLocaleLowerCase();
    if (value === 'compatible' || value === 'success') return 'ok';
    if (value.includes('need') || value.includes('matrix') || value.includes('scaffold') || value.includes('bridge')) return 'warn';
    return value === 'failed' || value === 'incompatible' ? 'bad' : 'muted';
  }

  supplierName(value: string): string {
    const names: Record<string, string> = {
      zkteco: 'ZKTeco', hikvision: 'Hikvision', dahua: 'Dahua', suprema: 'Suprema',
      virdi: 'VIRDI', anviz: 'Anviz', honeywell: 'Honeywell',
    };
    return names[value] ?? value;
  }

  private routePayload(): RouteRequest {
    const raw = this.form.getRawValue();
    return {
      vendor: raw.vendor,
      model: raw.model.trim(),
      firmwareVersion: this.empty(raw.firmwareVersion),
      platformVersion: this.empty(raw.platformVersion),
      serverVersion: this.empty(raw.serverVersion),
      osName: this.empty(raw.osName),
      architecture: this.empty(raw.architecture),
      sdkVersions: this.parseVersions(raw.sdkVersionsText),
      apiVersions: this.parseVersions(raw.apiVersionsText),
      capabilityHints: this.parseHints(raw.capabilityHintsText),
      host: this.empty(raw.host),
      port: raw.port,
      baseUrl: this.empty(raw.baseUrl),
      route: this.selectedRoute() || null,
      options: this.adapterOptions(raw),
    };
  }

  private devicePayload(route: string): DeviceIntegrationRequest {
    const raw = this.form.getRawValue();
    return {
      name: raw.name.trim(),
      vendor: raw.vendor,
      model: raw.model.trim(),
      serialNumber: this.empty(raw.serialNumber),
      firmwareVersion: this.empty(raw.firmwareVersion),
      platformVersion: this.empty(raw.platformVersion),
      serverVersion: this.empty(raw.serverVersion),
      osName: this.empty(raw.osName),
      architecture: this.empty(raw.architecture),
      sdkVersions: this.parseVersions(raw.sdkVersionsText),
      apiVersions: this.parseVersions(raw.apiVersionsText),
      capabilityHints: this.parseHints(raw.capabilityHintsText),
      host: this.empty(raw.host),
      port: raw.port,
      baseUrl: this.empty(raw.baseUrl),
      route,
      options: this.adapterOptions(raw),
      username: this.empty(raw.username),
      password: this.empty(raw.password),
      enabled: raw.enabled,
      syncIntervalMinutes: raw.syncIntervalMinutes,
    };
  }


  private adapterOptions(raw: {
    authMode: string;
    authHeader: string;
    punchPath: string;
    punchMethod: string;
    punchArrayPath: string;
    sinceParam: string;
    optionsText: string;
  }): Record<string, unknown> {
    const result = this.parseOptions(raw.optionsText);
    const assign = (key: string, value: string) => {
      const cleaned = value?.trim();
      if (cleaned) result[key] = cleaned; else delete result[key];
    };
    assign('auth', raw.authMode);
    assign('auth_header', raw.authHeader);
    assign('punch_path', raw.punchPath);
    assign('punch_method', raw.punchMethod);
    assign('punch_array_path', raw.punchArrayPath);
    assign('since_param', raw.sinceParam);
    return result;
  }

  private remainingOptions(options: Record<string, unknown>): string {
    const value = { ...options };
    for (const key of ['auth', 'auth_header', 'punch_path', 'punch_method', 'punch_array_path', 'since_param']) delete value[key];
    return Object.keys(value).length ? JSON.stringify(value, null, 2) : '';
  }

  private parseVersions(value: string): Record<string, string> {
    const result: Record<string, string> = {};
    for (const token of value.split(/[\n,]+/)) {
      const [route, ...versionParts] = token.split('=');
      const version = versionParts.join('=').trim();
      if (route?.trim() && version) result[route.trim()] = version;
    }
    return result;
  }

  private parseHints(value: string): string[] {
    return value.split(/[\n,]+/).map((item) => item.trim()).filter(Boolean);
  }

  private parseOptions(value: string): Record<string, unknown> {
    if (!value.trim()) return {};
    try {
      const parsed = JSON.parse(value);
      return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : {};
    } catch {
      this.notifications.error(this.i18n.t('deviceIntegrations.optionsInvalid'));
      return {};
    }
  }

  private keyValueText(value: Record<string, string>): string {
    return Object.entries(value).map(([key, version]) => `${key}=${version}`).join('\n');
  }

  private empty(value: string): string | null {
    return value?.trim() ? value.trim() : null;
  }
}
