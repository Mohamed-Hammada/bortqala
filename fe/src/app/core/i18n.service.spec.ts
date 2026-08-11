import '@angular/compiler';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { I18nService } from './i18n.service';

describe('I18nService application scopes', () => {
  let service: I18nService;
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [I18nService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(I18nService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    TestBed.resetTestingModule();
  });

  it('caches the same locale separately for the default scope and each application', async () => {
    const defaultLoad = service.use('ar-EG');
    http.expectOne('/api/v1/i18n/ar-EG').flush({
      locale: 'ar-EG', appId: null, messages: { 'nav.title': 'العنوان العام' },
    });
    await defaultLoad;

    const appLoad = service.use('ar-EG', 'demo-app');
    http.expectOne('/api/v1/i18n/ar-EG').flush({
      locale: 'ar-EG', appId: 'demo-app', messages: { 'nav.title': 'عنوان ديمو' },
    });
    await appLoad;
    expect(service.t('nav.title')).toBe('عنوان ديمو');

    await service.use('ar-EG', 'demo-app');
    http.expectNone('/api/v1/i18n/ar-EG');
  });

  it('lets a database value override emergency in-code copy', async () => {
    const load = service.use('en-US', 'client-app');
    http.expectOne('/api/v1/i18n/en-US').flush({
      locale: 'en-US', appId: 'client-app', messages: { 'nav.settingsHint': 'Client settings' },
    });
    await load;

    expect(service.t('nav.settingsHint')).toBe('Client settings');
  });
});
