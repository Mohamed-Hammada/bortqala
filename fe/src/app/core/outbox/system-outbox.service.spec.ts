import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { SystemOutboxService } from './system-outbox.service';

describe('SystemOutboxService', () => {
  let service: SystemOutboxService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), SystemOutboxService],
    });
    service = TestBed.inject(SystemOutboxService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('listEvents fetches paged outbox events', () => {
    service.listEvents({ status: 'PENDING', page: 0, size: 10 }).subscribe((res) => {
      expect(res.items.length).toBe(1);
      expect(res.items[0].eventType).toBe('INVOICE_CREATED');
    });

    const req = httpMock.expectOne((r) =>
      r.url === '/api/v1/system/outbox/events' &&
      r.params.get('status') === 'PENDING' &&
      r.params.get('page') === '0'
    );
    expect(req.request.method).toBe('GET');
    req.flush({
      items: [{
        id: 'evt-1',
        eventType: 'INVOICE_CREATED',
        aggregateType: 'Invoice',
        aggregateId: 'inv-123',
        payloadJson: '{}',
        status: 'PENDING',
        retryCount: 0,
        maxRetries: 5,
        createdAt: '2026-08-30T10:00:00Z',
      }],
      page: 0,
      size: 10,
      totalElements: 1,
      totalPages: 1,
    });
  });

  it('getStats fetches counts', () => {
    service.getStats().subscribe((stats) => {
      expect(stats.pendingCount).toBe(2);
      expect(stats.publishedCount).toBe(100);
      expect(stats.deadLetterCount).toBe(0);
    });

    const req = httpMock.expectOne('/api/v1/system/outbox/stats');
    expect(req.request.method).toBe('GET');
    req.flush({
      pendingCount: 2,
      publishedCount: 100,
      failedCount: 1,
      deadLetterCount: 0,
    });
  });

  it('retryEvent posts retry request', () => {
    service.retryEvent('evt-1').subscribe();

    const req = httpMock.expectOne('/api/v1/system/outbox/events/evt-1/retry');
    expect(req.request.method).toBe('POST');
    req.flush(null);
  });
});
