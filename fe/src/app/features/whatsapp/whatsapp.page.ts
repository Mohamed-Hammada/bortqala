import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { I18nService } from '../../core/i18n.service';
import { NotificationService } from '../../core/notification.service';
import { OutboundLogEntry, WhatsAppSettings } from '../finance/payment-links/payment-link.models';

@Component({
  selector: 'app-whatsapp-page',
  standalone: true,
  imports: [],
  template: `
    <section class="page">
      <header class="page-header">
        <div>
          <p class="eyebrow">{{ i18n.t('settings.whatsapp') }}</p>
          <h1>{{ i18n.t('settings.whatsapp') }}</h1>
          <p>{{ i18n.t('settings.whatsappHint') }}</p>
        </div>
      </header>

      <!-- Provider Status -->
      <div class="card settings-card">
        <div class="settings-sections">
          <fieldset class="settings-group">
            <legend class="group-title">{{ i18n.t('settings.whatsappProvider') }}</legend>
            @if (settings()?.configured) {
              <span class="status-badge" style="color: var(--success)">{{ i18n.t('settings.whatsappConfigured') }}</span>
            } @else {
              <span class="status-badge" style="color: var(--muted)">{{ i18n.t('settings.whatsappNotConfigured') }}</span>
            }
          </fieldset>

          <fieldset class="settings-group">
            <legend class="group-title">{{ i18n.t('settings.whatsappTestSend') }}</legend>
            <div class="form-row">
              <input type="tel" [value]="testPhone()" (input)="testPhone.set($any($event.target).value)" [placeholder]="i18n.t('settings.whatsappTestPhone')" />
              <button class="button" [disabled]="sendingTest()" (click)="sendTest()">{{ sendingTest() ? i18n.t('common.executing') : i18n.t('settings.whatsappTestSend') }}</button>
            </div>
          </fieldset>
        </div>
      </div>

      <!-- Log -->
      <h2>{{ i18n.t('settings.whatsappLogTitle') }}</h2>
      @if (logs().length === 0) {
        <div class="empty-state">{{ i18n.t('common.noResults') }}</div>
      } @else {
        <div class="table-card">
          <table>
            <thead>
              <tr>
                <th>{{ i18n.t('settings.whatsappTestPhone') }}</th>
                <th>Template</th>
                <th>Status</th>
                <th>Error</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              @for (log of logs(); track log.id) {
                <tr>
                  <td>{{ log.phoneNumber }}</td>
                  <td>{{ log.templateKey }}</td>
                  <td><span class="status-badge" [style.color]="log.status === 'SENT' || log.status === 'DELIVERED' ? 'var(--success)' : log.status === 'FAILED' ? 'var(--danger)' : 'var(--muted)'">{{ log.status }}</span></td>
                  <td>{{ log.errorMessage ?? '—' }}</td>
                  <td>
                    @if (log.status === 'FAILED') {
                      <button class="button small secondary" (click)="resend(log.id)">{{ i18n.t('settings.whatsappResendFailed') }}</button>
                    }
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    </section>
  `,
  styles: [`
    .page { padding: 1.5rem; }
    .page-header { margin-bottom: 1.5rem; }
    .card { padding: 1.5rem; margin-bottom: 1.5rem; }
    .settings-group { margin-bottom: 1rem; }
    .group-title { font-weight: 600; margin-bottom: 0.5rem; }
    .form-row { display: flex; gap: 0.75rem; align-items: center; }
    .form-row input { flex: 1; padding: 0.5rem; border-radius: 6px; border: 1px solid var(--line); background: var(--input-bg); }
    .button { padding: 0.5rem 1rem; border-radius: 6px; border: none; cursor: pointer; background: var(--gold); color: var(--ink); }
    .button.secondary { background: var(--surface-muted); }
    .button.small { padding: 0.3rem 0.7rem; font-size: 0.85rem; }
    .button:disabled { opacity: 0.5; cursor: not-allowed; }
    .table-card { overflow-x: auto; }
    table { width: 100%; border-collapse: collapse; }
    th, td { padding: 0.75rem; text-align: start; border-bottom: 1px solid var(--line); }
    th { font-weight: 600; color: var(--muted); font-size: 0.8rem; text-transform: uppercase; }
    .status-badge { font-weight: 600; }
    .empty-state { padding: 2rem; text-align: center; color: var(--muted); }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WhatsAppPage implements OnInit {
  readonly i18n = inject(I18nService);
  private readonly http = inject(HttpClient);
  private readonly notification = inject(NotificationService);

  readonly settings = signal<WhatsAppSettings | null>(null);
  readonly logs = signal<OutboundLogEntry[]>([]);
  readonly testPhone = signal('');
  readonly sendingTest = signal(false);

  async ngOnInit() {
    await this.loadSettings();
    await this.loadLogs();
  }

  async loadSettings() {
    try {
      const res = await fetch('/api/v1/whatsapp/settings');
      this.settings.set(await res.json());
    } catch { /* ignore */ }
  }

  async loadLogs() {
    try {
      const res: any = await new Promise((resolve, reject) => {
        this.http.get('/api/v1/whatsapp/logs').subscribe({ next: resolve, error: reject });
      });
      this.logs.set(res.entries ?? []);
    } catch { /* ignore */ }
  }

  async sendTest() {
    if (!this.testPhone()) return;
    this.sendingTest.set(true);
    try {
      await new Promise<void>((resolve, reject) => {
        this.http.post('/api/v1/whatsapp/test-send', { phoneNumber: this.testPhone() }).subscribe({ next: () => resolve(), error: reject });
      });
      this.notification.success(this.i18n.t('settings.whatsappTestSend'));
      await this.loadLogs();
    } catch {
      this.notification.error(this.i18n.t('common.error'));
    } finally {
      this.sendingTest.set(false);
    }
  }

  async resend(logId: string) {
    try {
      await new Promise<void>((resolve, reject) => {
        this.http.post('/api/v1/whatsapp/resend', { logId }).subscribe({ next: () => resolve(), error: reject });
      });
      await this.loadLogs();
    } catch {
      this.notification.error(this.i18n.t('common.error'));
    }
  }
}
