import { TestBed } from '@angular/core/testing';
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { OfflineOutboxService, OutboxEntry } from './offline-outbox.service';

describe('OfflineOutboxService (WP-14 AC-3)', () => {
  let service: OfflineOutboxService;
  let httpMock: HttpTestingController;

  const withFakeDb = () => {
    // Minimal IndexedDB stand-in: an in-memory object store keyed by operationId.
    const store = new Map<string, OutboxEntry>();
    const fakeRequest = <T>(result: T): IDBRequest & { result: T } => {
      const request = { result, onsuccess: null, onerror: null } as unknown as IDBRequest & {
        onsuccess: (() => void) | null;
        onerror: (() => void) | null;
      };
      queueMicrotask(() => request.onsuccess?.());
      return request;
    };
    class FakeTx {
      objectStore() {
        return {
          put: (value: OutboxEntry, key: string) =>
            fakeRequest((store.set(key, value), undefined)),
          delete: (key: string) => fakeRequest((store.delete(key), undefined)),
          getAll: () => fakeRequest([...store.values()] as unknown as undefined),
        };
      }
      oncomplete: (() => void) | null = null;
      onerror: (() => void) | null = null;
    }
    class FakeDb {
      transaction(): FakeTx {
        const tx = new FakeTx();
        queueMicrotask(() => tx.oncomplete?.());
        return tx;
      }
      objectStoreNames = { contains: () => true };
    }
    const db = new FakeDb();
    // jsdom ships no IndexedDB — install the fake as the global before the service touches it.
    vi.stubGlobal('indexedDB', { open: () => fakeRequest(db as unknown as IDBDatabase) });
    return store;
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    service = TestBed.inject(OfflineOutboxService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    vi.restoreAllMocks();
  });

  it('queues a failed punch and replays it exactly once on flush', async () => {
    const store = withFakeDb();
    await service.enqueue('/api/v1/attendance/selfie-punch', { imageBase64: 'x' }, 'op-1');
    expect(await service.count()).toBe(1);
    expect(store.get('op-1')).toBeTruthy();

    const flushing = service.flush();
    // pending() resolves on a microtask; let the flush loop reach its first POST.
    await new Promise((resolve) => setTimeout(resolve, 0));
    const request = httpMock.expectOne('/api/v1/attendance/selfie-punch');
    expect(request.request.body['operationId']).toBe('op-1');
    request.flush({ id: 'punch-1', duplicate: false });

    const { sent, remaining } = await flushing;
    expect(sent).toBe(1);
    expect(remaining).toBe(0);
    expect(store.has('op-1')).toBe(false);
  });

  it('keeps the entry when the server is still unreachable', async () => {
    withFakeDb();
    await service.enqueue('/api/v1/attendance/selfie-punch', { imageBase64: 'x' }, 'op-2');

    const flushing = service.flush();
    await new Promise((resolve) => setTimeout(resolve, 0));
    httpMock.expectOne('/api/v1/attendance/selfie-punch').flush(null, { status: 503, statusText: 'Offline' });
    const { sent, remaining } = await flushing;

    expect(sent).toBe(0);
    expect(remaining).toBe(1);
  });
});
