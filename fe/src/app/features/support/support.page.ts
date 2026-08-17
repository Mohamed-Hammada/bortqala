import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { I18nService } from '../../core/i18n.service';
import { NotificationService } from '../../core/notification.service';
import { AuthService } from '../../core/auth/auth.service';
import { apiErrorMessage } from '../../core/api-error';

interface Ticket {
  id: string;
  ticketNo: string;
  priority: string;
  category: string;
  moduleCode: string;
  screen: string;
  businessImpact: string;
  description: string;
  status: string;
  assignedTeam: string;
  slaDueAt: number;
  createdBy: string;
  createdAt: number;
  updatedAt: number;
  resolvedAt: number;
  version: number;
}

interface FeedbackItem {
  id: string;
  type: string;
  moduleCode: string;
  message: string;
  rating: number | null;
  route: string;
  status: string;
  createdBy: string;
  createdAt: number;
  replayed: boolean;
}

interface Reason {
  key: string;
  points: number;
  status: string;
  actionRoute: string;
}

interface Health {
  score: number;
  band: string;
  dimensions: Record<string, number>;
  reasons: Reason[];
  operationId: string;
  calculatedAt: number;
  replayed: boolean;
}

type Tab = 'tickets' | 'feedback' | 'health';

@Component({
  selector: 'app-support-page',
  imports: [FormsModule, RouterLink],
  templateUrl: './support.page.html',
  styleUrl: './support.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SupportPage {
  private readonly http = inject(HttpClient);
  readonly i18n = inject(I18nService);
  readonly auth = inject(AuthService);
  private readonly notifications = inject(NotificationService);

  readonly tickets = signal<Ticket[]>([]);
  readonly feedbackItems = signal<FeedbackItem[]>([]);
  readonly health = signal<Health | null>(null);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  readonly tab = signal<Tab>('tickets');

  priority = 'MEDIUM';
  category = 'QUESTION';
  moduleCode = 'GENERAL';
  screen = location.pathname;
  impact = '';
  description = '';

  feedbackType = 'GENERAL';
  feedbackMessage = '';
  rating = 5;

  nextStatus: Record<string, string> = {};
  comments: Record<string, string> = {};

  private readonly reasonMax: Record<string, number> = {
    'health.activation': 20,
    'health.usage': 25,
    'health.adoption': 20,
    'health.dataQuality': 10,
    'health.operational': 10,
    'health.support': 5,
    'health.commercial': 10,
  };

  constructor() {
    void this.load();
  }

  isAdmin(): boolean {
    return this.auth.hasAnyRole(['SUPER_ADMIN', 'ADMIN']);
  }

  async load(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      this.tickets.set(await firstValueFrom(this.http.get<Ticket[]>('/api/v1/support/tickets')));
      if (this.isAdmin()) {
        const [health, feedback] = await Promise.all([
          firstValueFrom(this.http.get<Health | null>('/api/v1/support/health')),
          firstValueFrom(this.http.get<FeedbackItem[]>('/api/v1/support/feedback')),
        ]);
        this.health.set(health);
        this.feedbackItems.set(feedback);
      }
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    } finally {
      this.loading.set(false);
    }
  }

  async create(): Promise<void> {
    if (this.saving()) return;
    this.saving.set(true);
    this.error.set(null);
    try {
      await firstValueFrom(
        this.http.post('/api/v1/support/tickets', {
          priority: this.priority,
          category: this.category,
          moduleCode: this.moduleCode,
          screen: this.screen,
          businessImpact: this.impact,
          description: this.description,
          operationId: crypto.randomUUID(),
        }),
      );
      this.impact = '';
      this.description = '';
      this.notifications.success(this.i18n.t('support.created'));
      await this.load();
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    } finally {
      this.saving.set(false);
    }
  }

  async update(ticket: Ticket): Promise<void> {
    const status = this.nextStatus[ticket.id];
    const comment = this.comments[ticket.id];
    if (!status || !comment?.trim() || this.saving()) return;

    this.saving.set(true);
    this.error.set(null);
    try {
      await firstValueFrom(
        this.http.put(`/api/v1/support/tickets/${ticket.id}`, {
          status,
          assignedTeam: 'SUPPORT',
          comment: comment.trim(),
          operationId: crypto.randomUUID(),
          expectedVersion: ticket.version,
        }),
      );
      delete this.comments[ticket.id];
      await this.load();
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    } finally {
      this.saving.set(false);
    }
  }

  async feedback(): Promise<void> {
    if (!this.feedbackMessage.trim() || this.saving()) return;
    this.saving.set(true);
    this.error.set(null);
    try {
      await firstValueFrom(
        this.http.post('/api/v1/support/feedback', {
          type: this.feedbackType,
          moduleCode: this.moduleCode,
          message: this.feedbackMessage.trim(),
          rating: this.feedbackType === 'RATING' ? this.rating : null,
          route: location.pathname,
          applicationVersion: 'web',
          browser: navigator.userAgent,
          correlationId: crypto.randomUUID(),
          operationId: crypto.randomUUID(),
        }),
      );
      this.feedbackMessage = '';
      this.notifications.success(this.i18n.t('support.feedbackSent'));

      if (this.isAdmin()) {
        this.feedbackItems.set(
          await firstValueFrom(this.http.get<FeedbackItem[]>('/api/v1/support/feedback')),
        );
      }
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    } finally {
      this.saving.set(false);
    }
  }

  async calculate(): Promise<void> {
    if (this.saving()) return;
    this.saving.set(true);
    this.error.set(null);
    try {
      this.health.set(
        await firstValueFrom(
          this.http.post<Health>('/api/v1/support/health/calculate', {
            operationId: crypto.randomUUID(),
          }),
        ),
      );
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    } finally {
      this.saving.set(false);
    }
  }

  key(prefix: string, value: string): string {
    return `${prefix}.${value.toLowerCase().replaceAll('_', '')}`;
  }

  bandKey(band: string): string {
    return `support.health.band.${band.toLowerCase().replaceAll('_', '')}`;
  }

  maxFor(reason: Reason): number {
    return this.reasonMax[reason.key] ?? 100;
  }

  percentage(reason: Reason): number {
    const max = this.maxFor(reason);
    return max <= 0 ? 0 : Math.min(100, Math.max(0, Math.round((reason.points / max) * 100)));
  }

  format(value: number): string {
    return value
      ? new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' }).format(value)
      : '—';
  }
}
