import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { SelfiePunchPage } from './selfie-punch.page';

/** jsdom ships no IndexedDB — the outbox only needs a no-op store for these tests. */
const stubIndexedDb = (): void => {
  const fakeRequest = (result: unknown): IDBRequest => {
    const request = { result, onsuccess: null as (() => void) | null, onerror: null };
    queueMicrotask(() => request.onsuccess?.());
    return request as unknown as IDBRequest;
  };
  vi.stubGlobal('indexedDB', {
    open: () => {
      const request = fakeRequest({
        transaction: () => ({
          objectStore: () => ({ getAll: () => fakeRequest([]), delete: () => fakeRequest(undefined) }),
          oncomplete: null,
          onerror: null,
        }),
        objectStoreNames: { contains: () => true },
      });
      return request;
    },
  });
};

describe('SelfiePunchPage (WP-14 AC-3)', () => {
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    stubIndexedDb();
    await TestBed.configureTestingModule({
      imports: [SelfiePunchPage],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    vi.unstubAllGlobals();
    TestBed.resetTestingModule();
  });

  function makeImage(bytes: number): string {
    const payload = btoa('A'.repeat(Math.max(4, bytes)));
    return `data:image/jpeg;base64,${payload}`;
  }

  it('creates and starts empty', () => {
    const fixture = TestBed.createComponent(SelfiePunchPage);
    expect(fixture.componentInstance).toBeTruthy();
    expect(fixture.componentInstance.previewDataUrl()).toBeNull();
    expect(fixture.componentInstance.canSubmit()).toBe(false);
  });

  it('rejects oversized images before submitting', async () => {
    const fixture = TestBed.createComponent(SelfiePunchPage);
    await fixture.whenStable();
    // ~2.7 MB decoded: (len*3)/4 must exceed the 2 MB cap.
    (fixture.componentInstance as unknown as { applyImage(dataUrl: string, type: string): void }).applyImage(
      makeImage(3_600_000),
      'image/jpeg',
    );
    expect(fixture.componentInstance.canSubmit()).toBe(true);

    await fixture.componentInstance.submitPunch();
    expect(fixture.componentInstance.previewDataUrl()).not.toBeNull(); // rejected, preview kept
  });

  it('posts to the idempotent endpoint on submit', async () => {
    const fixture = TestBed.createComponent(SelfiePunchPage);
    await fixture.whenStable();

    (fixture.componentInstance as unknown as { applyImage(dataUrl: string, type: string): void }).applyImage(
      makeImage(64),
      'image/jpeg',
    );
    const submitting = fixture.componentInstance.submitPunch();
    const request = httpMock.expectOne('/api/v1/attendance/selfie-punch');
    expect(request.request.body['operationId']).toMatch(/^[0-9a-f]{32}$/);
    request.flush({ id: 'punch-1', duplicate: false });
    await submitting;

    expect(fixture.componentInstance.previewDataUrl()).toBeNull();
    httpMock.verify();
  });
});
