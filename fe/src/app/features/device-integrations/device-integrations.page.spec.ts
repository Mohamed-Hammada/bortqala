import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { DeviceIntegrationsPage } from './device-integrations.page';
import { DeviceIntegrationsStore } from './device-integrations.store';

describe('DeviceIntegrationsPage', () => {
  let store: DeviceIntegrationsStore;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DeviceIntegrationsPage],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        DeviceIntegrationsStore,
      ],
    }).compileComponents();

    store = TestBed.inject(DeviceIntegrationsStore);
  });

  it('renders and filters device list', () => {
    const fixture = TestBed.createComponent(DeviceIntegrationsPage);
    const component = fixture.componentInstance;

    component.store.devices.set([
      {
        id: 'dev-1',
        biometricDeviceId: 'bio-1',
        hubDeviceId: 'hub-1',
        name: 'Main Gate ZKTeco',
        vendor: 'zkteco',
        model: 'IN01-A',
        serialNumber: 'SN-001',
        firmwareVersion: 'v1.0',
        platformVersion: null,
        serverVersion: null,
        osName: null,
        architecture: null,
        sdkVersions: {},
        apiVersions: {},
        capabilityHints: [],
        host: '192.168.1.100',
        port: 4370,
        baseUrl: null,
        route: 'zkteco-standalone',
        routeStatus: 'COMPATIBLE',
        routeKind: 'DIRECT',
        implementationStatus: 'ACTIVE',
        officialDocumentation: [],
        options: {},
        enabled: true,
        syncIntervalMinutes: 15,
        username: 'admin',
        hasPassword: true,
        lastProbeStatus: 'SUCCESS',
        lastProbeMessage: 'OK',
        lastProbeAt: '2026-08-19T10:00:00Z',
        createdAt: '2026-08-19T00:00:00Z',
        updatedAt: '2026-08-19T00:00:00Z',
      },
      {
        id: 'dev-2',
        biometricDeviceId: 'bio-2',
        hubDeviceId: 'hub-2',
        name: 'Warehouse Hikvision',
        vendor: 'hikvision',
        model: 'DS-K1T671',
        serialNumber: 'SN-002',
        firmwareVersion: 'v2.0',
        platformVersion: null,
        serverVersion: null,
        osName: null,
        architecture: null,
        sdkVersions: {},
        apiVersions: {},
        capabilityHints: [],
        host: '192.168.1.101',
        port: 80,
        baseUrl: null,
        route: 'hikvision-isapi',
        routeStatus: 'COMPATIBLE',
        routeKind: 'DIRECT',
        implementationStatus: 'ACTIVE',
        officialDocumentation: [],
        options: {},
        enabled: true,
        syncIntervalMinutes: 30,
        username: 'admin',
        hasPassword: true,
        lastProbeStatus: 'SUCCESS',
        lastProbeMessage: 'OK',
        lastProbeAt: '2026-08-19T10:00:00Z',
        createdAt: '2026-08-19T00:00:00Z',
        updatedAt: '2026-08-19T00:00:00Z',
      },
    ]);

    expect(component.visibleDevices().length).toBe(2);

    component.filter.set('Gate');
    expect(component.visibleDevices().length).toBe(1);
    expect(component.visibleDevices()[0].name).toBe('Main Gate ZKTeco');
  });

  it('opens new device drawer with defaults', () => {
    const fixture = TestBed.createComponent(DeviceIntegrationsPage);
    const component = fixture.componentInstance;

    component.openNew();
    expect(component.drawerOpen()).toBe(true);
    expect(component.editingId()).toBeNull();
    expect(component.form.controls.vendor.value).toBe('zkteco');
    expect(component.form.controls.syncIntervalMinutes.value).toBe(15);
  });
});
