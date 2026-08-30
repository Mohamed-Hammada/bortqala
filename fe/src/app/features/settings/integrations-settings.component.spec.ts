import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { IntegrationsSettingsComponent } from './integrations-settings.component';

function flushInit(http: HttpTestingController) {
  const keysReq = http.expectOne('/api/v1/platform/api-keys');
  expect(keysReq.request.method).toBe('GET');
  keysReq.flush({ keys: [] });
  const hooksReq = http.expectOne('/api/v1/platform/webhooks');
  expect(hooksReq.request.method).toBe('GET');
  hooksReq.flush({ endpoints: [] });
}

function flushLoad(http: HttpTestingController) {
  const keysReq = http.expectOne('/api/v1/platform/api-keys');
  keysReq.flush({ keys: [] });
  const hooksReq = http.expectOne('/api/v1/platform/webhooks');
  hooksReq.flush({ endpoints: [] });
}

describe('IntegrationsSettingsComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [IntegrationsSettingsComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
  });

  it('loads keys and endpoints on init', async () => {
    const http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(IntegrationsSettingsComponent);
    fixture.detectChanges();
    flushInit(http);
    await fixture.whenStable();
    await fixture.whenStable();
    expect(fixture.componentInstance.loading()).toBe(false);
    expect(fixture.componentInstance.keys().length).toBe(0);
    expect(fixture.componentInstance.endpoints().length).toBe(0);
  });

  it('creates an API key and reveals the full key', async () => {
    const http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(IntegrationsSettingsComponent);
    fixture.detectChanges();
    flushInit(http);
    await fixture.whenStable();
    await fixture.whenStable();

    fixture.componentInstance.newKeyName.set('Test Key');
    fixture.componentInstance.newKeyScopes.set('invoices:read');
    fixture.componentInstance.newKeyRateLimit.set(60);

    const createPromise = fixture.componentInstance.createKey();

    const postReq = http.expectOne(r => r.url === '/api/v1/platform/api-keys' && r.method === 'POST');
    postReq.flush({
      id: 'k1', name: 'Test Key', fullKey: 'bk_testsecret123',
      scopes: 'invoices:read', rateLimitPerMin: 60, active: true, createdAtEpochMs: Date.now(),
    });

    await fixture.whenStable();
    flushLoad(http);
    await createPromise;

    expect(fixture.componentInstance.showKeyReveal()).toBe(true);
    expect(fixture.componentInstance.revealedKey()).toBe('bk_testsecret123');
  });

  it('creates a webhook endpoint', async () => {
    const http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(IntegrationsSettingsComponent);
    fixture.detectChanges();
    flushInit(http);
    await fixture.whenStable();
    await fixture.whenStable();

    fixture.componentInstance.newWebhookUrl.set('https://example.com/hook');
    fixture.componentInstance.newWebhookEvents.set('invoice.paid');

    const createPromise = fixture.componentInstance.createWebhook();

    const postReq = http.expectOne(r => r.url === '/api/v1/platform/webhooks' && r.method === 'POST');
    postReq.flush({ id: 'w1', url: 'https://example.com/hook', events: 'invoice.paid', active: true, createdAtEpochMs: Date.now(), updatedAtEpochMs: Date.now(), version: 1 });

    await fixture.whenStable();
    await new Promise(r => setTimeout(r, 0));
    http.expectOne('/api/v1/platform/api-keys').flush({ keys: [] });
    http.expectOne('/api/v1/platform/webhooks').flush({ endpoints: [{ id: 'w1', url: 'https://example.com/hook', events: 'invoice.paid', active: true, createdAtEpochMs: Date.now(), updatedAtEpochMs: Date.now(), version: 1 }] });
    await createPromise;
    expect(fixture.componentInstance.endpoints().length).toBe(1);
  });

  it('loads deliveries for selected endpoint', async () => {
    const http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(IntegrationsSettingsComponent);
    fixture.detectChanges();
    flushInit(http);
    await fixture.whenStable();
    await fixture.whenStable();

    const deliveryPromise = fixture.componentInstance.viewDeliveries('ep-1');
    const req = http.expectOne('/api/v1/platform/webhooks/ep-1/deliveries');
    req.flush({ deliveries: [{ id: 1, endpointId: 'ep-1', event: 'invoice.paid', payload: '{}', status: 'DELIVERED', attempts: 1, lastError: null, responseStatus: 200, createdAtEpochMs: Date.now() }] });

    await deliveryPromise;
    expect(fixture.componentInstance.deliveries().length).toBe(1);
    expect(fixture.componentInstance.selectedEndpointId()).toBe('ep-1');
  });
});
