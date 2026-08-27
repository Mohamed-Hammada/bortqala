import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { DatePipe } from '@angular/common';
import { I18nService } from '../../core/i18n.service';
import { NotificationService } from '../../core/notification.service';
import { RecurringTemplate, DunningRule, JobEntry } from '../finance/payment-links/payment-link.models';

@Component({
  selector: 'app-automation-page',
  standalone: true,
  imports: [DatePipe],
  template: `
    <section class="page">
      <header class="page-header">
        <div>
          <p class="eyebrow">{{ i18n.t('settings.automation') }}</p>
          <h1>{{ i18n.t('settings.automation') }}</h1>
          <p>{{ i18n.t('settings.automationHint') }}</p>
        </div>
      </header>

      <!-- Tabs -->
      <div class="tab-bar">
        <button [class.active]="activeTab() === 'templates'" (click)="activeTab.set('templates')">{{ i18n.t('automation.recurringTemplates') }}</button>
        <button [class.active]="activeTab() === 'dunning'" (click)="activeTab.set('dunning')">{{ i18n.t('automation.dunningRules') }}</button>
        <button [class.active]="activeTab() === 'jobs'" (click)="activeTab.set('jobs')">{{ i18n.t('automation.jobsHealth') }}</button>
      </div>

      <!-- Templates Tab -->
      @if (activeTab() === 'templates') {
        <div class="card">
          <div class="card-header">
            <h2>{{ i18n.t('automation.recurringTemplates') }}</h2>
            <button class="button" (click)="showCreateTemplate.set(true)">{{ i18n.t('automation.createTemplate') }}</button>
          </div>
          @if (templates().length === 0) {
            <div class="empty-state">{{ i18n.t('automation.noTemplates') }}</div>
          } @else {
            <table>
              <thead>
                <tr>
                  <th>{{ i18n.t('automation.templateName') }}</th>
                  <th>{{ i18n.t('automation.kind') }}</th>
                  <th>{{ i18n.t('automation.cadence') }}</th>
                  <th>{{ i18n.t('automation.nextRun') }}</th>
                  <th>{{ i18n.t('automation.active') }}</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                @for (tpl of templates(); track tpl.id) {
                  <tr>
                    <td>{{ tpl.templateName }}</td>
                    <td>{{ tpl.kind }}</td>
                    <td>{{ tpl.cadence }}</td>
                    <td>{{ tpl.nextRunAtEpochMs | date:'medium' }}</td>
                    <td><span [style.color]="tpl.active ? 'var(--success)' : 'var(--muted)'">{{ tpl.active ? i18n.t('automation.active') : '—' }}</span></td>
                    <td>
                      <button class="button small secondary" (click)="toggleTemplate(tpl.id, !tpl.active)">{{ tpl.active ? i18n.t('common.cancel') : i18n.t('automation.active') }}</button>
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          }
          <button class="button" style="margin-top: 1rem" (click)="runTemplates()">{{ i18n.t('automation.runNow') }}</button>
        </div>
      }

      <!-- Dunning Tab -->
      @if (activeTab() === 'dunning') {
        <div class="card">
          <div class="card-header">
            <h2>{{ i18n.t('automation.dunningRules') }}</h2>
            <button class="button" (click)="showCreateRule.set(true)">{{ i18n.t('automation.createRule') }}</button>
          </div>
          @if (dunningRules().length === 0) {
            <div class="empty-state">{{ i18n.t('automation.noRules') }}</div>
          } @else {
            <table>
              <thead>
                <tr>
                  <th>{{ i18n.t('automation.daysOverdue') }}</th>
                  <th>{{ i18n.t('automation.channel') }}</th>
                  <th>Template</th>
                  <th>{{ i18n.t('automation.active') }}</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                @for (rule of dunningRules(); track rule.id) {
                  <tr>
                    <td>{{ rule.daysOverdue }}</td>
                    <td>{{ rule.channel }}</td>
                    <td>{{ rule.templateKey }}</td>
                    <td><span [style.color]="rule.active ? 'var(--success)' : 'var(--muted)'">{{ rule.active ? i18n.t('automation.active') : '—' }}</span></td>
                    <td>
                      <button class="button small secondary" (click)="toggleDunningRule(rule.id, !rule.active)">{{ rule.active ? i18n.t('common.cancel') : i18n.t('automation.active') }}</button>
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          }
          <button class="button" style="margin-top: 1rem" (click)="runDunning()">{{ i18n.t('automation.runDunning') }}</button>
        </div>
      }

      <!-- Jobs Tab -->
      @if (activeTab() === 'jobs') {
        <div class="card">
          <div class="card-header">
            <h2>{{ i18n.t('automation.jobsHealth') }}</h2>
          </div>
          @if (jobs().length === 0) {
            <div class="empty-state">{{ i18n.t('automation.noJobs') }}</div>
          } @else {
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Type</th>
                  <th>Status</th>
                  <th>Error</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                @for (job of jobs(); track job.id) {
                  <tr>
                    <td>{{ job.id }}</td>
                    <td>{{ job.type }}</td>
                    <td><span [style.color]="job.status === 'FAILED' ? 'var(--danger)' : 'var(--success)'">{{ job.status }}</span></td>
                    <td>{{ job.error ?? '—' }}</td>
                    <td>
                      @if (job.status === 'FAILED') {
                        <button class="button small secondary" (click)="retryJob(job.id)">{{ i18n.t('automation.retry') }}</button>
                      }
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          }
        </div>
      }
    </section>
  `,
  styles: [`
    .page { padding: 1.5rem; }
    .page-header { margin-bottom: 1.5rem; }
    .tab-bar { display: flex; gap: 0.5rem; margin-bottom: 1.5rem; }
    .tab-bar button { padding: 0.5rem 1rem; border: 1px solid var(--line); border-radius: 6px; background: var(--surface); cursor: pointer; }
    .tab-bar button.active { background: var(--gold); color: var(--ink); border-color: var(--gold); }
    .card { padding: 1.5rem; background: var(--surface-card); border-radius: 8px; }
    .card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; }
    .button { padding: 0.5rem 1rem; border-radius: 6px; border: none; cursor: pointer; background: var(--gold); color: var(--ink); }
    .button.secondary { background: var(--surface-muted); }
    .button.small { padding: 0.3rem 0.7rem; font-size: 0.85rem; }
    table { width: 100%; border-collapse: collapse; }
    th, td { padding: 0.75rem; text-align: start; border-bottom: 1px solid var(--line); }
    th { font-weight: 600; color: var(--muted); font-size: 0.8rem; text-transform: uppercase; }
    .empty-state { padding: 2rem; text-align: center; color: var(--muted); }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AutomationPage implements OnInit {
  readonly i18n = inject(I18nService);
  private readonly http = inject(HttpClient);
  private readonly notification = inject(NotificationService);

  readonly activeTab = signal<'templates' | 'dunning' | 'jobs'>('templates');
  readonly templates = signal<RecurringTemplate[]>([]);
  readonly dunningRules = signal<DunningRule[]>([]);
  readonly jobs = signal<JobEntry[]>([]);
  readonly showCreateTemplate = signal(false);
  readonly showCreateRule = signal(false);

  async ngOnInit() {
    await Promise.all([this.loadTemplates(), this.loadDunningRules(), this.loadJobs()]);
  }

  async loadTemplates() {
    try {
      const res: any = await new Promise((resolve, reject) => {
        this.http.get('/api/v1/automation/templates').subscribe({ next: resolve, error: reject });
      });
      this.templates.set(res.templates ?? []);
    } catch { /* ignore */ }
  }

  async loadDunningRules() {
    try {
      const res: any = await new Promise((resolve, reject) => {
        this.http.get('/api/v1/automation/dunning-rules').subscribe({ next: resolve, error: reject });
      });
      this.dunningRules.set(res.rules ?? []);
    } catch { /* ignore */ }
  }

  async loadJobs() {
    try {
      const res: any = await new Promise((resolve, reject) => {
        this.http.get('/api/v1/automation/jobs').subscribe({ next: resolve, error: reject });
      });
      this.jobs.set(res.jobs ?? []);
    } catch { /* ignore */ }
  }

  async toggleTemplate(id: string, active: boolean) {
    try {
      await new Promise<void>((resolve, reject) => {
        this.http.post(`/api/v1/automation/templates/${id}/toggle?active=${active}`, {}).subscribe({ next: () => resolve(), error: reject });
      });
      await this.loadTemplates();
    } catch { /* ignore */ }
  }

  async runTemplates() {
    try {
      await new Promise<void>((resolve, reject) => {
        this.http.post('/api/v1/automation/templates/run', {}).subscribe({ next: () => resolve(), error: reject });
      });
      this.notification.success(this.i18n.t('automation.runNow'));
      await this.loadTemplates();
    } catch { /* ignore */ }
  }

  async toggleDunningRule(id: string, active: boolean) {
    try {
      await new Promise<void>((resolve, reject) => {
        this.http.post(`/api/v1/automation/dunning-rules/${id}/toggle?active=${active}`, {}).subscribe({ next: () => resolve(), error: reject });
      });
      await this.loadDunningRules();
    } catch { /* ignore */ }
  }

  async runDunning() {
    try {
      await new Promise<void>((resolve, reject) => {
        this.http.post('/api/v1/automation/dunning/run', {}).subscribe({ next: () => resolve(), error: reject });
      });
      this.notification.success(this.i18n.t('automation.runDunning'));
    } catch { /* ignore */ }
  }

  async retryJob(id: string) {
    this.notification.success(this.i18n.t('automation.retry'));
  }
}
