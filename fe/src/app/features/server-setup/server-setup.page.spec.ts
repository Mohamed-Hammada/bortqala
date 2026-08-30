import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { ServerSetupPage } from './server-setup.page';
import { NativeBridgeService } from '../../core/native/native-bridge.service';

describe('ServerSetupPage (WP-14 AC-1)', () => {
  let httpMock: HttpTestingController;
  let router: Router;
  let native: NativeBridgeService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ServerSetupPage],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    native = TestBed.inject(NativeBridgeService);
  });

  afterEach(() => {
    httpMock.verify();
    vi.restoreAllMocks();
    TestBed.resetTestingModule();
  });

  it('creates with empty server url and no error', () => {
    const fixture = TestBed.createComponent(ServerSetupPage);
    expect(fixture.componentInstance).toBeTruthy();
    expect(fixture.componentInstance.serverUrlValue).toBe('');
    expect(fixture.componentInstance.errorKey()).toBeNull();
  });

  it('surfaces an error key when the probe fails and stays put', async () => {
    const fixture = TestBed.createComponent(ServerSetupPage);
    fixture.componentInstance.serverUrlValue = 'erp.company.com';
    const navigate = vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);

    const connecting = fixture.componentInstance.connect();
    httpMock.expectOne((request) => request.url.includes('/api/v1/i18n/')).flush(null, { status: 0, statusText: '' });
    await connecting;

    expect(fixture.componentInstance.errorKey()).toBe('native.probeUnreachable');
    expect(navigate).not.toHaveBeenCalled();
  });

  it('stores the url and navigates to login on a successful probe', async () => {
    const fixture = TestBed.createComponent(ServerSetupPage);
    fixture.componentInstance.serverUrlValue = 'https://erp.company.com/';
    const storeSpy = vi.spyOn(native, 'storeServerUrl').mockResolvedValue(undefined);
    const navigate = vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);

    const connecting = fixture.componentInstance.connect();
    httpMock.expectOne((request) => request.url.includes('/api/v1/i18n/')).flush({ hello: 'bemo' });
    await connecting;

    expect(storeSpy).toHaveBeenCalledWith('https://erp.company.com/');
    expect(fixture.componentInstance.errorKey()).toBeNull();
    expect(navigate).toHaveBeenCalledWith('/login');
  });
});
