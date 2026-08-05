import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { JournalEntriesPage, JournalEntry } from './journal-entries.page';
import { I18nService } from '../../../core/i18n.service';
import { NotificationService } from '../../../core/notification.service';

describe('JournalEntriesPage', () => {
  let httpMock: HttpTestingController;
  let page: JournalEntriesPage;

  const draftEntry = (overrides: Partial<JournalEntry> = {}): JournalEntry => ({
    id: 'entry-1',
    entryNumber: 'JV-1001',
    entryDate: 1700000000000,
    description: 'Test entry',
    status: 'DRAFT',
    version: 3,
    lines: [],
    totalDebit: 100,
    totalCredit: 100,
    createdAt: 1700000000000,
    updatedAt: 1700000000000,
    ...overrides,
  });

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [JournalEntriesPage],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: I18nService,
          useValue: { t: (key: string) => key },
        },
        {
          provide: NotificationService,
          useValue: { success: () => undefined, error: () => undefined },
        },
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(JournalEntriesPage);
    page = fixture.componentInstance;

    httpMock
      .expectOne((req) => req.url === '/api/v1/finance/journal-entries')
      .flush({ content: [], totalElements: 0 });
    httpMock.expectOne((req) => req.url === '/api/v1/finance/accounts').flush([]);
  });

  afterEach(() => {
    TestBed.resetTestingModule();
    httpMock.verify();
  });

  function flushPost(status: number, body?: unknown): { operationId: string; expectedVersion: number; reason: string | null } {
    const req = httpMock.expectOne(`/api/v1/finance/journal-entries/entry-1/post`);
    expect(req.request.method).toBe('POST');
    const payload = req.request.body as { operationId: string; expectedVersion: number; reason: string | null };
    if (status === 0) {
      req.error(new ProgressEvent('network'), { status: 0, statusText: 'Network error' });
    } else {
      req.flush(body ?? {}, { status, statusText: status === 200 ? 'OK' : 'Error' });
    }
    return payload;
  }

  function flushReload() {
    httpMock
      .expectOne((req) => req.url === '/api/v1/finance/journal-entries' && req.params.get('page') === '1')
      .flush({ content: [], totalElements: 0 });
  }

  async function yieldMicrotasks(): Promise<void> {
    await Promise.resolve();
    await Promise.resolve();
  }

  it('sends a UUID operationId and the current expectedVersion when posting', async () => {
    const postPromise = page.postEntry(draftEntry());
    const payload = flushPost(200);
    await yieldMicrotasks();
    flushReload();
    await postPromise;

    expect(payload.operationId).toMatch(/^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i);
    expect(payload.expectedVersion).toBe(3);
  });

  it('double click sends only one active request', async () => {
    const first = page.postEntry(draftEntry());
    const second = page.postEntry(draftEntry());
    const payload = flushPost(200);
    await yieldMicrotasks();
    flushReload();
    await Promise.all([first, second]);

    expect(payload).toBeTruthy();
    expect(httpMock.match(`/api/v1/finance/journal-entries/entry-1/post`)).toHaveLength(0);
  });

  it('timeout retry reuses the operation ID', async () => {
    const first = page.postEntry(draftEntry());
    const firstPayload = flushPost(0);
    await expect(first).rejects.toBeInstanceOf(HttpErrorResponse);

    const retry = page.postEntry(draftEntry());
    const secondPayload = flushPost(200);
    await yieldMicrotasks();
    flushReload();
    await retry;

    expect(secondPayload.operationId).toBe(firstPayload.operationId);
  });

  it('HTTP 409 clears the operation ID and propagates a deterministic failure', async () => {
    const posting = page.postEntry(draftEntry());
    const payload = flushPost(409, { code: 'RECORD_ALREADY_MODIFIED' });
    await expect(posting).rejects.toBeInstanceOf(HttpErrorResponse);

    expect(page.pendingPostOperations()['entry-1']).toBeUndefined();
    expect(payload.operationId).toBeTruthy();
  });

  it('success clears the stored operation ID and reloads the current page', async () => {
    const reloadPromise = page.postEntry(draftEntry());
    flushPost(200);
    await yieldMicrotasks();
    flushReload();
    await reloadPromise;

    expect(page.pendingPostOperations()['entry-1']).toBeUndefined();
  });
});
