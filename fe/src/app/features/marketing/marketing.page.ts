import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { I18nService } from '../../core/i18n.service';
import { NotificationService } from '../../core/notification.service';
import { MarketingService } from './marketing.service';
import { Campaign, Survey, SurveyQuestion } from './marketing.models';
import { ReportDataset, SavedReport, ReportColumn } from './marketing.models';

@Component({
  selector: 'app-marketing-page',
  standalone: true,
  imports: [],
  templateUrl: './marketing.page.html',
  styleUrl: './marketing.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MarketingPage implements OnInit {
  readonly i18n = inject(I18nService);
  private readonly notification = inject(NotificationService);
  private readonly service = inject(MarketingService);

  readonly loading = signal(true);
  readonly activeTab = signal<'campaigns' | 'surveys' | 'builder'>('campaigns');
  readonly campaigns = signal<Campaign[]>([]);
  readonly surveys = signal<Survey[]>([]);

  readonly showCreateCampaign = signal(false);
  readonly showCreateSurvey = signal(false);
  readonly newCampaignName = signal('');
  readonly newCampaignChannel = signal('EMAIL');
  readonly newCampaignSubject = signal('');
  readonly newCampaignBodyAr = signal('');
  readonly newCampaignBodyEn = signal('');
  readonly newSurveyTitle = signal('');
  readonly newSurveyDesc = signal('');
  readonly sendingCampaignId = signal<string | null>(null);

  readonly datasets = signal<ReportDataset[]>([]);
  readonly selectedDataset = signal('');
  readonly dimensions = signal<string[]>([]);
  readonly measures = signal<string[]>([]);
  readonly queryResult = signal<{ columns: ReportColumn[]; rows: Record<string, unknown>[]; totalRows: number } | null>(null);
  readonly datasetFields = signal<{ name: string; labelAr: string; labelEn: string; type: string; dimension: boolean }[]>([]);
  readonly savedReports = signal<SavedReport[]>([]);
  readonly savingReport = signal(false);
  readonly reportName = signal('');

  ngOnInit() { this.loadAll(); this.loadDatasets(); }

  async loadAll() {
    this.loading.set(true);
    try {
      if (this.activeTab() === 'campaigns') this.campaigns.set(await this.service.listCampaigns());
      else this.surveys.set(await this.service.listSurveys());
    } catch { this.notification.error(this.i18n.t('common.loadError')); }
    finally { this.loading.set(false); }
  }

  async loadDatasets() {
    try {
      this.datasets.set(await this.service.listDatasets());
      this.savedReports.set(await this.service.listSavedReports());
    } catch { /* ignore */ }
  }

  async selectDataset(code: string) {
    this.selectedDataset.set(code);
    this.dimensions.set([]);
    this.measures.set([]);
    this.queryResult.set(null);
    const ds = this.datasets().find(d => d.code === code);
    if (!ds) return;
    try {
      const resp = await fetch(`/api/v1/report-builder/datasets/${code}`);
      const data = await resp.json();
      this.datasetFields.set(data.fields);
    } catch { /* ignore */ }
  }

  toggleField(name: string, dimension: boolean) {
    if (dimension) {
      this.dimensions.update(d => d.includes(name) ? d.filter(x => x !== name) : [...d, name]);
    } else {
      this.measures.update(m => m.includes(name) ? m.filter(x => x !== name) : [...m, name]);
    }
  }

  async runQuery() {
    if (!this.selectedDataset()) return;
    try {
      this.queryResult.set(await this.service.runQuery({
        datasetCode: this.selectedDataset(),
        dimensions: this.dimensions(),
        measures: this.measures(),
        limit: 1000,
      }));
    } catch { this.notification.error(this.i18n.t('common.error')); }
  }

  async saveReport() {
    if (!this.reportName() || !this.selectedDataset()) return;
    this.savingReport.set(true);
    try {
      await this.service.saveReport(this.reportName(), this.selectedDataset(),
        JSON.stringify({ dimensions: this.dimensions(), measures: this.measures() }));
      this.notification.success(this.i18n.t('reportBuilder.reportSaved'));
      this.savedReports.set(await this.service.listSavedReports());
    } catch { this.notification.error(this.i18n.t('common.error')); }
    finally { this.savingReport.set(false); }
  }

  async createCampaign() {
    if (!this.newCampaignName()) return;
    try {
      await this.service.createCampaign({
        name: this.newCampaignName(), channel: this.newCampaignChannel(),
        subject: this.newCampaignSubject(), bodyAr: this.newCampaignBodyAr(), bodyEn: this.newCampaignBodyEn(),
      });
      this.showCreateCampaign.set(false);
      this.newCampaignName.set('');
      this.notification.success(this.i18n.t('marketing.campaignCreated'));
      await this.loadAll();
    } catch { this.notification.error(this.i18n.t('common.error')); }
  }

  async sendCampaign(id: string) {
    this.sendingCampaignId.set(id);
    try {
      await this.service.sendCampaign(id);
      this.notification.success(this.i18n.t('marketing.campaignSent'));
      await this.loadAll();
    } catch { this.notification.error(this.i18n.t('common.error')); }
    finally { this.sendingCampaignId.set(null); }
  }

  async createSurvey() {
    if (!this.newSurveyTitle()) return;
    try {
      await this.service.createSurvey(this.newSurveyTitle(), this.newSurveyDesc());
      this.showCreateSurvey.set(false);
      this.newSurveyTitle.set('');
      this.notification.success(this.i18n.t('marketing.surveyCreated'));
      await this.loadAll();
    } catch { this.notification.error(this.i18n.t('common.error')); }
  }

  statusColor(s: string): string {
    return ({ DRAFT: 'var(--muted)', SCHEDULED: 'var(--gold)', SENDING: 'var(--warning-text)', SENT: 'var(--success)', FAILED: 'var(--danger)' })[s] ?? 'var(--muted)';
  }
}
