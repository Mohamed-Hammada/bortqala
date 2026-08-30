import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { I18nService } from '../../../../core/i18n.service';
import { SystemOutboxService } from '../../../../core/outbox/system-outbox.service';
import { OutboxEventSummary, OutboxStatsResponse } from '../../../../core/outbox/system-outbox.models';

@Component({
  selector: 'app-system-outbox-page',
  standalone: true,
  imports: [CommonModule, FormsModule, DatePipe],
  template: `
    <div class="outbox-page">
      <header class="page-header">
        <h1>{{ i18n.t('outbox.title') }}</h1>
        <button class="btn btn-secondary" (click)="refresh()">
          🔄 {{ i18n.t('common.refresh') }}
        </button>
      </header>

      @if (stats()) {
        <div class="stats-grid">
          <div class="stat-card pending">
            <div class="stat-num">{{ stats()!.pendingCount }}</div>
            <div class="stat-label">{{ i18n.t('outbox.pendingEvents') }}</div>
          </div>
          <div class="stat-card published">
            <div class="stat-num">{{ stats()!.publishedCount }}</div>
            <div class="stat-label">{{ i18n.t('outbox.publishedEvents') }}</div>
          </div>
          <div class="stat-card failed">
            <div class="stat-num">{{ stats()!.failedCount }}</div>
            <div class="stat-label">{{ i18n.t('outbox.failedEvents') }}</div>
          </div>
          <div class="stat-card dead-letter">
            <div class="stat-num">{{ stats()!.deadLetterCount }}</div>
            <div class="stat-label">Dead Letter</div>
          </div>
        </div>
      }

      <div class="filter-bar">
        <div class="filter-item">
          <label>{{ i18n.t('common.status') }}</label>
          <select [(ngModel)]="statusFilter" (change)="loadEvents()">
            <option value="">{{ i18n.t('common.all') }}</option>
            <option value="PENDING">PENDING</option>
            <option value="PUBLISHED">PUBLISHED</option>
            <option value="FAILED">FAILED</option>
            <option value="DEAD_LETTER">DEAD_LETTER</option>
          </select>
        </div>
      </div>

      <div class="table-container">
        <table class="data-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>{{ i18n.t('common.type') }}</th>
              <th>Aggregate</th>
              <th>{{ i18n.t('common.status') }}</th>
              <th>Retries</th>
              <th>Created At</th>
              <th>Last Error</th>
              <th>{{ i18n.t('common.actions') }}</th>
            </tr>
          </thead>
          <tbody>
            @if (loading()) {
              <tr><td colspan="8" class="text-center">{{ i18n.t('common.loading') }}</td></tr>
            } @else if (events().length === 0) {
              <tr><td colspan="8" class="text-center">{{ i18n.t('common.noData') }}</td></tr>
            } @else {
              @for (evt of events(); track evt.id) {
                <tr>
                  <td class="font-mono text-xs">{{ evt.id.substring(0, 8) }}...</td>
                  <td class="font-bold">{{ evt.eventType }}</td>
                  <td>{{ evt.aggregateType }} ({{ evt.aggregateId }})</td>
                  <td>
                    <span class="badge" [class.badge-pending]="evt.status === 'PENDING'" [class.badge-published]="evt.status === 'PUBLISHED'" [class.badge-failed]="evt.status === 'FAILED' || evt.status === 'DEAD_LETTER'">
                      {{ evt.status }}
                    </span>
                  </td>
                  <td>{{ evt.retryCount }} / {{ evt.maxRetries }}</td>
                  <td>{{ evt.createdAt | date:'short' }}</td>
                  <td class="error-cell">{{ evt.lastError || '—' }}</td>
                  <td>
                    @if (evt.status === 'FAILED' || evt.status === 'DEAD_LETTER') {
                      <button class="btn btn-sm btn-primary" (click)="retry(evt.id)">
                        {{ i18n.t('outbox.retryNow') }}
                      </button>
                    }
                  </td>
                </tr>
              }
            }
          </tbody>
        </table>
      </div>
    </div>
  `,
  styles: [`
    .outbox-page { padding: 1.5rem; }
    .page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.5rem; }
    .stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 1rem; margin-bottom: 1.5rem; }
    .stat-card { background: var(--surface-card); padding: 1.25rem; border-radius: 12px; border: 1px solid var(--border); text-align: center; }
    .stat-card.pending { border-left: 4px solid var(--warning); }
    .stat-card.published { border-left: 4px solid var(--success); }
    .stat-card.failed { border-left: 4px solid var(--danger); }
    .stat-card.dead-letter { border-left: 4px solid #8b5cf6; }
    .stat-num { font-size: 1.75rem; font-weight: 700; color: var(--ink); }
    .stat-label { font-size: 0.85rem; color: var(--muted); margin-top: 0.25rem; }
    .filter-bar { margin-bottom: 1rem; }
    .filter-item select { padding: 0.4rem 0.8rem; border-radius: 6px; border: 1px solid var(--border); }
    .table-container { background: var(--surface-card); border-radius: 12px; border: 1px solid var(--border); overflow: auto; }
    .data-table { width: 100%; border-collapse: collapse; text-align: left; }
    .data-table th, .data-table td { padding: 0.85rem 1rem; border-bottom: 1px solid var(--border); }
    .data-table th { background: var(--surface); color: var(--muted); font-size: 0.85rem; }
    .badge { padding: 0.25rem 0.5rem; border-radius: 6px; font-size: 0.8rem; font-weight: 600; }
    .badge-pending { background: var(--warning-soft); color: var(--warning-text); }
    .badge-published { background: var(--success-soft); color: var(--success); }
    .badge-failed { background: var(--danger-soft); color: var(--danger); }
    .btn { padding: 0.6rem 1rem; border-radius: 8px; border: none; cursor: pointer; font-weight: 500; }
    .btn-sm { padding: 0.35rem 0.65rem; font-size: 0.8rem; }
    .btn-primary { background: var(--gold); color: var(--ink); }
    .btn-secondary { background: var(--surface); color: var(--ink); border: 1px solid var(--border); }
    .text-xs { font-size: 0.75rem; }
    .font-mono { font-family: monospace; }
    .font-bold { font-weight: 600; }
    .text-center { text-align: center; }
    .error-cell { max-width: 250px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: var(--danger); font-size: 0.85rem; }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SystemOutboxPage implements OnInit {
  readonly i18n = inject(I18nService);
  private readonly outboxService = inject(SystemOutboxService);

  readonly events = signal<OutboxEventSummary[]>([]);
  readonly stats = signal<OutboxStatsResponse | null>(null);
  readonly loading = signal(true);

  statusFilter = '';

  ngOnInit() {
    this.refresh();
  }

  refresh() {
    this.loadStats();
    this.loadEvents();
  }

  loadStats() {
    this.outboxService.getStats().subscribe({
      next: (data) => this.stats.set(data),
      error: () => this.stats.set(null),
    });
  }

  loadEvents() {
    this.loading.set(true);
    this.outboxService.listEvents({ status: this.statusFilter || undefined }).subscribe({
      next: (data) => {
        this.events.set(data.items);
        this.loading.set(false);
      },
      error: () => {
        this.events.set([]);
        this.loading.set(false);
      },
    });
  }

  retry(id: string) {
    this.outboxService.retryEvent(id).subscribe({
      next: () => this.refresh(),
    });
  }
}
