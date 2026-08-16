import { HttpClient } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { apiErrorMessage } from '../../core/api-error';
import { formatDateTime } from '../../core/date';
import { I18nService } from '../../core/i18n.service';

type BatchRow = Record<string, unknown>;

@Component({
  selector: 'app-import-history-page',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './import-history.page.html',
  styleUrl: './import-history.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ImportHistoryPage {
  private readonly http = inject(HttpClient);
  readonly i18n = inject(I18nService);
  readonly rows = signal<BatchRow[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly query = signal('');
  readonly page = signal(1);
  readonly pageSize = 25;

  readonly filtered = computed(() => {
    const q = this.query().trim().toLowerCase();
    if (!q) return this.rows();
    return this.rows().filter((row) => Object.values(row).some((value) => String(value ?? '').toLowerCase().includes(q)));
  });
  readonly pageCount = computed(() => Math.max(1, Math.ceil(this.filtered().length / this.pageSize)));
  readonly visible = computed(() => {
    const safePage = Math.min(this.page(), this.pageCount());
    const start = (safePage - 1) * this.pageSize;
    return this.filtered().slice(start, start + this.pageSize);
  });

  constructor() { void this.load(); }

  async load(): Promise<void> {
    this.loading.set(true); this.error.set(null);
    try { this.rows.set(await firstValueFrom(this.http.get<BatchRow[]>('/api/v1/imports'))); }
    catch (error) { this.error.set(apiErrorMessage(error, this.i18n)); }
    finally { this.loading.set(false); }
  }

  setQuery(event: Event): void { this.query.set((event.target as HTMLInputElement).value); this.page.set(1); }
  previous(): void { this.page.update((value) => Math.max(1, value - 1)); }
  next(): void { this.page.update((value) => Math.min(this.pageCount(), value + 1)); }
  value(row: BatchRow, ...keys: string[]): string {
    for (const key of keys) {
      const value = row[key];
      if (value !== null && value !== undefined && String(value).trim() !== '') return String(value);
    }
    return '—';
  }
  date(row: BatchRow): string {
    for (const key of ['importedAt', 'createdAt', 'startedAt', 'completedAt']) {
      const raw = row[key];
      if (typeof raw === 'number' && Number.isFinite(raw)) return formatDateTime(raw);
      if (typeof raw === 'string' && raw.trim()) {
        const parsed = Date.parse(raw);
        if (!Number.isNaN(parsed)) return formatDateTime(parsed);
      }
    }
    return '—';
  }
}
