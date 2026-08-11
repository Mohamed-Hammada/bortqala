import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { ScreenShortcutService } from './screen-shortcut.service';
import { ScreenShortcutProfile } from './screen-shortcut.models';

const MOCK_PROFILE: ScreenShortcutProfile = {
  profileMode: 'CUSTOM',
  version: 1,
  shortcuts: [
    {
      id: 'sc-1',
      pageCode: 'EMPLOYEES',
      menuId: 'employees',
      route: '/employees',
      titleKey: 'nav.employees',
      secondKeyCode: 'KeyE',
      displayKey: 'E',
      enabled: true,
      defaultShortcut: false,
      availabilityStatus: 'AVAILABLE',
      unavailableReasonKey: null,
    },
    {
      id: 'sc-2',
      pageCode: 'PAYROLL',
      menuId: 'payroll',
      route: '/payroll',
      titleKey: 'nav.payroll',
      secondKeyCode: 'KeyP',
      displayKey: 'P',
      enabled: false,
      defaultShortcut: false,
      availabilityStatus: 'DISABLED',
      unavailableReasonKey: 'shortcuts.disabled',
    },
  ],
  availableDestinations: [
    {
      pageCode: 'EMPLOYEES',
      menuId: 'employees',
      route: '/employees',
      titleKey: 'nav.employees',
      module: 'HR',
      requiredFeature: null,
    },
  ],
  updatedAt: '2026-08-06T12:00:00Z',
};

describe('ScreenShortcutService', () => {
  let service: ScreenShortcutService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ScreenShortcutService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
  });

  it('load stores profile and filters runtimeShortcuts for active available items', async () => {
    const promise = service.load();
    const req = http.expectOne('/api/v1/auth/preferences/shortcuts');
    expect(req.request.method).toBe('GET');
    req.flush(MOCK_PROFILE);

    const profile = await promise;
    expect(profile).toEqual(MOCK_PROFILE);
    expect(service.profile()).toEqual(MOCK_PROFILE);

    const runtime = service.runtimeShortcuts();
    expect(runtime).toHaveLength(1);
    expect(runtime[0].pageCode).toBe('EMPLOYEES');
  });

  it('findByCode finds runtime shortcut by secondKeyCode', async () => {
    service.profile.set(MOCK_PROFILE);
    expect(service.findByCode('KeyE')).toBeDefined();
    expect(service.findByCode('KeyE')?.pageCode).toBe('EMPLOYEES');
    expect(service.findByCode('KeyP')).toBeUndefined(); // disabled
  });

  it('replace updates profile state', async () => {
    const promise = service.replace({
      expectedVersion: 1,
      shortcuts: [{ secondKeyCode: 'KeyE', pageCode: 'EMPLOYEES', enabled: true }],
    });

    const req = http.expectOne('/api/v1/auth/preferences/shortcuts');
    expect(req.request.method).toBe('PUT');
    req.flush(MOCK_PROFILE);

    const profile = await promise;
    expect(profile).toEqual(MOCK_PROFILE);
  });

  it('reset updates profile state', async () => {
    const promise = service.reset();

    const req = http.expectOne('/api/v1/auth/preferences/shortcuts/reset');
    expect(req.request.method).toBe('POST');
    req.flush(MOCK_PROFILE);

    const profile = await promise;
    expect(profile).toEqual(MOCK_PROFILE);
  });
});
