import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { I18nService } from '../../core/i18n.service';
import { NotificationService } from '../../core/notification.service';
import { MarketingService } from '../marketing/marketing.service';
import { ReportDataset, SavedReport, ReportColumn } from '../marketing/marketing.models';

@Component({
  selector: 'app-report-builder-page',
  standalone: true,
  templateUrl: './report-builder.page.html',
  styleUrl: './report-builder.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ReportBuilderPage implements OnInit {
  readonly i18n = inject(I18nService);
  private readonly notification = inject(NotificationService);
  private readonly service = inject(MarketingService);

  readonly loading = signal(true);
  readonly datasets = signal<ReportDataset[]>([]);
  readonly selectedDataset = signal('');
  readonly datasetFields = signal<{ name: string; labelAr: string; labelEn: string; type: string; dimension: boolean }[]>([]);
  readonly dimensions = signal<string[]>([]);
  readonly measures = signal<string[]>([]);
  readonly queryResult = signal<{ columns: ReportColumn[]; rows: Record<string, unknown>[]; totalRows: number } | null>(null);
  readonly savedReports = signal<SavedReport[]>([]);
  readonly savingReport = signal(false);
  readonly reportName = signal('');
  readonly limit = signal(1000);

  ngOnInit() { this.loadDatasets(); this.loadSaved(); }

  async loadDatasets() {
    this.loading.set(true);
    try {
      this.datasets.set(await this.service.listDatasets());
    } catch { this.notification.error(this.i18n.t('common.loadError')); }
    finally { this.loading.set(false); }
  }

  async loadSaved() {
    try { this.savedReports.set(await this.service.listSavedReports()); } catch { /* ignore */ }
  }

  async selectDataset(code: string) {
    this.selectedDataset.set(code);
    this.dimensions.set([]);
    this.measures.set([]);
    this.queryResult.set(null);
    if (!code) return;
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
        limit: this.limit(),
      }));
    } catch { this.notification.error(this.i18n.t('common.error')); }
  }

  async saveReport() {
    if (!this.reportName() || !this.selectedDataset()) return;
    this.savingReport.set(true);
    try {
      await this.service.saveReport(this.reportName(), this.selectedDataset(),
        JSON.stringify({ dimensions: this.dimensions(), measures: this.measures(), limit: this.limit() }));
      this.notification.success(this.i18n.t('reportBuilder.reportSaved'));
      this.savedReports.set(await this.service.listSavedReports());
    } catch { this.notification.error(this.i18n.t('common.error')); }
    finally { this.savingReport.set(false); }
  }

  async deleteReport(id: string) {
    try {
      await this.service.deleteReport(id);
      await this.loadSaved();
    } catch { this.notification.error(this.i18n.t('common.error')); }
  }
}
