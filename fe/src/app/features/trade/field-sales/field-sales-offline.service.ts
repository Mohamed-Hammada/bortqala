import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, catchError, map, of, tap } from 'rxjs';
import {
  FieldSalesDocumentType,
  OfflineBundleResponse,
  OfflineTransactionRecordResponse,
  SyncBatchRequest,
  SyncBatchResponse,
  SyncTransactionRequestItem,
} from './field-sales.models';

const DB_NAME = 'bemo_field_sales_db';
const DB_VERSION = 1;
const STORE_BUNDLE = 'bundle';
const STORE_OUTBOX = 'outbox';
const STORE_HISTORY = 'history';

@Injectable({
  providedIn: 'root',
})
export class FieldSalesOfflineService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/trade/field-sales';

  readonly isOnline = signal<boolean>(typeof navigator !== 'undefined' ? navigator.onLine : true);
  readonly outboxCount = signal<number>(0);
  readonly isSyncing = signal<boolean>(false);
  readonly lastSyncTimestamp = signal<number | null>(null);

  private db: IDBDatabase | null = null;
  private dbPromise: Promise<IDBDatabase> | null = null;

  constructor() {
    if (typeof window !== 'undefined') {
      window.addEventListener('online', () => {
        this.isOnline.set(true);
        void this.syncOutboxIfOnline();
      });
      window.addEventListener('offline', () => {
        this.isOnline.set(false);
      });
    }
    void this.initDb().then(() => this.updateOutboxCount());
  }

  private async initDb(): Promise<IDBDatabase> {
    if (this.db) return this.db;
    if (this.dbPromise) return this.dbPromise;

    if (typeof indexedDB === 'undefined') {
      // Return a dummy object if running in environment without IndexedDB
      return {} as IDBDatabase;
    }

    this.dbPromise = new Promise((resolve, reject) => {
      const request = indexedDB.open(DB_NAME, DB_VERSION);

      request.onupgradeneeded = (event: IDBVersionChangeEvent) => {
        const db = (event.target as IDBOpenDBRequest).result;
        if (!db.objectStoreNames.contains(STORE_BUNDLE)) {
          db.createObjectStore(STORE_BUNDLE, { keyPath: 'key' });
        }
        if (!db.objectStoreNames.contains(STORE_OUTBOX)) {
          db.createObjectStore(STORE_OUTBOX, { keyPath: 'clientOfflineId' });
        }
        if (!db.objectStoreNames.contains(STORE_HISTORY)) {
          db.createObjectStore(STORE_HISTORY, { keyPath: 'clientOfflineId' });
        }
      };

      request.onsuccess = () => {
        this.db = request.result;
        resolve(this.db);
      };

      request.onerror = () => {
        console.warn('FieldSales: IndexedDB open failed, falling back to in-memory/localStorage', request.error);
        reject(request.error);
      };
    });

    return this.dbPromise;
  }

  generateOfflineDocNumber(type: FieldSalesDocumentType): string {
    const prefix = {
      INVOICE: 'OFF-INV',
      ORDER: 'OFF-ORD',
      RECEIPT: 'OFF-REC',
      RETURN: 'OFF-RET',
      QUOTATION: 'OFF-QTN',
    }[type] || 'OFF-DOC';

    const now = new Date();
    const dateStr = now.toISOString().slice(0, 10).replace(/-/g, '');
    const rand = Math.floor(1000 + Math.random() * 9000);
    return `${prefix}-${dateStr}-${rand}`;
  }

  fetchAndCacheBundle(): Observable<OfflineBundleResponse> {
    return this.http.get<OfflineBundleResponse>(`${this.baseUrl}/offline-bundle`).pipe(
      tap((bundle) => {
        void this.saveBundleToIndexedDb(bundle);
        this.lastSyncTimestamp.set(Date.now());
      }),
      catchError((err) => {
        console.warn('FieldSales: Failed to fetch online bundle, trying cache', err);
        return of(this.getFallbackBundle());
      })
    );
  }

  async getCachedBundle(): Promise<OfflineBundleResponse | null> {
    try {
      const db = await this.initDb();
      if (!db.transaction) return this.getFallbackBundle();

      return new Promise((resolve) => {
        const tx = db.transaction(STORE_BUNDLE, 'readonly');
        const store = tx.objectStore(STORE_BUNDLE);
        const req = store.get('current_bundle');
        req.onsuccess = () => {
          if (req.result?.data) {
            resolve(req.result.data as OfflineBundleResponse);
          } else {
            resolve(this.getFallbackBundle());
          }
        };
        req.onerror = () => resolve(this.getFallbackBundle());
      });
    } catch {
      return this.getFallbackBundle();
    }
  }

  async queueTransaction(item: SyncTransactionRequestItem): Promise<void> {
    try {
      const db = await this.initDb();
      if (!db.transaction) {
        this.saveToLocalStorageOutbox(item);
        this.updateOutboxCount();
        return;
      }

      await new Promise<void>((resolve, reject) => {
        const tx = db.transaction(STORE_OUTBOX, 'readwrite');
        const store = tx.objectStore(STORE_OUTBOX);
        const req = store.put(item);
        req.onsuccess = () => resolve();
        req.onerror = () => reject(req.error);
      });

      await this.updateOutboxCount();

      if (this.isOnline()) {
        void this.syncOutboxIfOnline();
      }
    } catch (e) {
      this.saveToLocalStorageOutbox(item);
      this.updateOutboxCount();
    }
  }

  async getOutbox(): Promise<SyncTransactionRequestItem[]> {
    try {
      const db = await this.initDb();
      if (!db.transaction) {
        return this.getFromLocalStorageOutbox();
      }

      return new Promise((resolve) => {
        const tx = db.transaction(STORE_OUTBOX, 'readonly');
        const store = tx.objectStore(STORE_OUTBOX);
        const req = store.getAll();
        req.onsuccess = () => resolve(req.result as SyncTransactionRequestItem[]);
        req.onerror = () => resolve(this.getFromLocalStorageOutbox());
      });
    } catch {
      return this.getFromLocalStorageOutbox();
    }
  }

  async removeOutboxItem(clientOfflineId: string): Promise<void> {
    try {
      const db = await this.initDb();
      if (db.transaction) {
        await new Promise<void>((resolve) => {
          const tx = db.transaction(STORE_OUTBOX, 'readwrite');
          tx.objectStore(STORE_OUTBOX).delete(clientOfflineId);
          tx.oncomplete = () => resolve();
          tx.onerror = () => resolve();
        });
      }
    } catch {
      // ignore
    }
    this.removeFromLocalStorageOutbox(clientOfflineId);
    await this.updateOutboxCount();
  }

  syncOutbox(): Observable<SyncBatchResponse | null> {
    if (this.isSyncing()) {
      return of(null);
    }

    this.isSyncing.set(true);

    return new Observable<SyncTransactionRequestItem[]>((observer) => {
      void this.getOutbox().then((items) => {
        observer.next(items);
        observer.complete();
      });
    }).pipe(
      map((items) => {
        if (!items || items.length === 0) {
          this.isSyncing.set(false);
          return null;
        }
        return items;
      }),
      tap((items) => {
        if (!items) this.isSyncing.set(false);
      }),
      catchError(() => {
        this.isSyncing.set(false);
        return of(null);
      }),
      map((items) => items ? { transactions: items } as SyncBatchRequest : null),
      // If transactions exist, send to server
      tap((req) => {
        if (!req) return;
        this.http.post<SyncBatchResponse>(`${this.baseUrl}/sync`, req).pipe(
          tap((resp) => {
            void this.handleSyncResponse(resp);
            this.isSyncing.set(false);
            this.lastSyncTimestamp.set(Date.now());
          }),
          catchError((err) => {
            console.error('FieldSales: Sync request failed', err);
            this.isSyncing.set(false);
            return of(null);
          })
        ).subscribe();
      }),
      map(() => null)
    );
  }

  getHistory(): Observable<OfflineTransactionRecordResponse[]> {
    return this.http.get<OfflineTransactionRecordResponse[]>(`${this.baseUrl}/history`).pipe(
      catchError(() => of([]))
    );
  }

  private async syncOutboxIfOnline(): Promise<void> {
    if (!this.isOnline() || this.isSyncing()) return;
    const items = await this.getOutbox();
    if (items.length === 0) return;

    this.isSyncing.set(true);
    const request: SyncBatchRequest = { transactions: items };
    this.http.post<SyncBatchResponse>(`${this.baseUrl}/sync`, request).subscribe({
      next: (resp) => {
        void this.handleSyncResponse(resp);
        this.isSyncing.set(false);
        this.lastSyncTimestamp.set(Date.now());
      },
      error: () => {
        this.isSyncing.set(false);
      },
    });
  }

  private async handleSyncResponse(response: SyncBatchResponse): Promise<void> {
    if (!response || !response.results) return;

    for (const res of response.results) {
      if (res.status === 'SYNCED') {
        await this.removeOutboxItem(res.clientOfflineId);
      }
      // If CONFLICT, keep in outbox or history with conflict details
      await this.saveToHistory(res);
    }
    await this.updateOutboxCount();
  }

  private async saveToHistory(result: { clientOfflineId: string; status: string; conflictReason?: string }): Promise<void> {
    try {
      const db = await this.initDb();
      if (db.transaction) {
        const tx = db.transaction(STORE_HISTORY, 'readwrite');
        tx.objectStore(STORE_HISTORY).put(result);
      }
    } catch {
      // ignore
    }
  }

  private async saveBundleToIndexedDb(bundle: OfflineBundleResponse): Promise<void> {
    try {
      const db = await this.initDb();
      if (db.transaction) {
        const tx = db.transaction(STORE_BUNDLE, 'readwrite');
        tx.objectStore(STORE_BUNDLE).put({ key: 'current_bundle', data: bundle, savedAt: Date.now() });
      }
      if (typeof localStorage !== 'undefined') {
        localStorage.setItem('bemo_field_sales_bundle', JSON.stringify(bundle));
      }
    } catch {
      if (typeof localStorage !== 'undefined') {
        localStorage.setItem('bemo_field_sales_bundle', JSON.stringify(bundle));
      }
    }
  }

  private async updateOutboxCount(): Promise<void> {
    const items = await this.getOutbox();
    this.outboxCount.set(items.length);
  }

  private getFallbackBundle(): OfflineBundleResponse {
    if (typeof localStorage !== 'undefined') {
      const cached = localStorage.getItem('bemo_field_sales_bundle');
      if (cached) {
        try {
          return JSON.parse(cached) as OfflineBundleResponse;
        } catch {
          // ignore
        }
      }
    }
    return {
      customers: [],
      products: [],
      warehouses: [],
      salesRepUserId: '',
      salesRepName: '',
      serverTimestamp: Date.now(),
    };
  }

  private saveToLocalStorageOutbox(item: SyncTransactionRequestItem): void {
    if (typeof localStorage === 'undefined') return;
    const items = this.getFromLocalStorageOutbox();
    const filtered = items.filter((x) => x.clientOfflineId !== item.clientOfflineId);
    filtered.push(item);
    localStorage.setItem('bemo_field_sales_outbox', JSON.stringify(filtered));
  }

  private getFromLocalStorageOutbox(): SyncTransactionRequestItem[] {
    if (typeof localStorage === 'undefined') return [];
    const val = localStorage.getItem('bemo_field_sales_outbox');
    if (!val) return [];
    try {
      return JSON.parse(val) as SyncTransactionRequestItem[];
    } catch {
      return [];
    }
  }

  private removeFromLocalStorageOutbox(clientOfflineId: string): void {
    if (typeof localStorage === 'undefined') return;
    const items = this.getFromLocalStorageOutbox().filter((x) => x.clientOfflineId !== clientOfflineId);
    localStorage.setItem('bemo_field_sales_outbox', JSON.stringify(items));
  }
}
