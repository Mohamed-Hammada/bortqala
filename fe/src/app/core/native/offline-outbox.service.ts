import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

export interface OutboxEntry {
  /** Client-generated idempotency key — the server dedupes on `operationId` (WP-14 AC-3). */
  operationId: string;
  url: string;
  body: unknown;
  queuedAt: number;
  attempts: number;
}

const DB_NAME = 'bemo-outbox';
const STORE_NAME = 'entries';

/**
 * WP-14 AC-3 offline outbox v1: failed punch POSTs queue in IndexedDB with a
 * client-generated operationId and replay exactly once once connectivity returns.
 * The backend rejects duplicate operationIds, so retries can never double-punch.
 */
@Injectable({ providedIn: 'root' })
export class OfflineOutboxService {
  private readonly http = inject(HttpClient);
  private listening = false;

  async enqueue(url: string, body: unknown, operationId: string): Promise<void> {
    const db = await this.open();
    await new Promise<void>((resolve, reject) => {
      const tx = db.transaction(STORE_NAME, 'readwrite');
      tx.objectStore(STORE_NAME).put({ operationId, url, body, queuedAt: Date.now(), attempts: 0 }, operationId);
      tx.oncomplete = () => resolve();
      tx.onerror = () => reject(tx.error);
    });
  }

  async pending(): Promise<OutboxEntry[]> {
    const db = await this.open();
    return new Promise((resolve, reject) => {
      const request = db.transaction(STORE_NAME, 'readonly').objectStore(STORE_NAME).getAll();
      request.onsuccess = () => resolve(request.result as OutboxEntry[]);
      request.onerror = () => reject(request.error);
    });
  }

  async count(): Promise<number> {
    return (await this.pending()).length;
  }

  /** Replays every queued entry; duplicates are removed either on success or server-dedupe. */
  async flush(): Promise<{ sent: number; remaining: number }> {
    const entries = await this.pending();
    let sent = 0;
    for (const entry of entries) {
      try {
        await firstValueFrom(this.http.post(entry.url, { ...(entry.body as object), operationId: entry.operationId }));
        await this.remove(entry.operationId);
        sent += 1;
      } catch {
        // Still offline or server rejected transiently — keep the entry for the next flush.
      }
    }
    return { sent, remaining: await this.count() };
  }

  /** Wires the online listener once; safe to call from the shell bootstrap. */
  autoFlush(): void {
    if (this.listening) return;
    this.listening = true;
    window.addEventListener('online', () => {
      void this.flush();
    });
  }

  private async remove(operationId: string): Promise<void> {
    const db = await this.open();
    await new Promise<void>((resolve, reject) => {
      const tx = db.transaction(STORE_NAME, 'readwrite');
      tx.objectStore(STORE_NAME).delete(operationId);
      tx.oncomplete = () => resolve();
      tx.onerror = () => reject(tx.error);
    });
  }

  private open(): Promise<IDBDatabase> {
    return new Promise((resolve, reject) => {
      const request = indexedDB.open(DB_NAME, 1);
      request.onupgradeneeded = () => {
        if (!request.result.objectStoreNames.contains(STORE_NAME)) {
          request.result.createObjectStore(STORE_NAME);
        }
      };
      request.onsuccess = () => resolve(request.result);
      request.onerror = () => reject(request.error);
    });
  }
}
