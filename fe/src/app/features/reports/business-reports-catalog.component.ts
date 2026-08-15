import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { I18nService } from '../../core/i18n.service';
import {
  BUSINESS_REPORT_CATALOG,
  BUSINESS_REPORT_MODULES,
  BusinessReportDefinition,
  BusinessReportModule,
} from './business-reports.catalog';

@Component({
  selector: 'app-business-reports-catalog',
  imports: [RouterLink],
  templateUrl: './business-reports-catalog.component.html',
  styleUrl: './business-reports-catalog.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BusinessReportsCatalogComponent {
  readonly i18n = inject(I18nService);
  readonly query = signal('');
  readonly selectedModule = signal<BusinessReportModule | 'all'>('all');
  readonly expandedId = signal<string | null>(null);
  readonly modules = BUSINESS_REPORT_MODULES;
  readonly reports = BUSINESS_REPORT_CATALOG;

  readonly filteredReports = computed(() => {
    this.i18n.locale();
    const module = this.selectedModule();
    const query = this.normalize(this.query());

    return this.reports.filter((report) => {
      if (module !== 'all' && report.module !== module) return false;
      if (!query) return true;
      return this.searchText(report).includes(query);
    });
  });

  setQuery(value: string): void {
    this.query.set(value);
  }

  setModule(module: BusinessReportModule | 'all'): void {
    this.selectedModule.set(module);
  }

  toggleDetails(reportId: string): void {
    this.expandedId.update((current) => (current === reportId ? null : reportId));
  }

  title(report: BusinessReportDefinition): string {
    return this.i18n.locale().startsWith('en') ? report.titleEn : report.titleAr;
  }

  moduleLabel(module: BusinessReportModule): string {
    return this.i18n.t(`reports.catalog.module.${module}`);
  }

  moduleCount(module: BusinessReportModule): number {
    return this.reports.filter((report) => report.module === module).length;
  }

  routeAdjusted(report: BusinessReportDefinition): boolean {
    return !report.sourceRoutes.includes(report.workspaceRoute);
  }

  private searchText(report: BusinessReportDefinition): string {
    return this.normalize([
      report.number,
      report.titleEn,
      report.titleAr,
      report.module,
      report.objective,
      report.trigger,
      report.workspaceRoute,
      ...report.sourceRoutes,
      ...report.entities,
      ...report.filters,
      ...report.metrics,
      ...report.exportFormats,
    ].join(' '));
  }

  private normalize(value: string): string {
    return value.trim().toLocaleLowerCase(this.i18n.locale());
  }
}
