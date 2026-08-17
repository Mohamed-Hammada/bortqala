import { BUSINESS_REPORT_CATALOG, BUSINESS_REPORT_MODULES } from './business-reports.catalog';

describe('BUSINESS_REPORT_CATALOG', () => {
  it('contains all 22 requested report definitions with unique ids', () => {
    expect(BUSINESS_REPORT_CATALOG.length).toBe(22);
    expect(new Set(BUSINESS_REPORT_CATALOG.map((report) => report.id)).size).toBe(22);
  });

  it('covers all ten report modules', () => {
    expect(new Set(BUSINESS_REPORT_CATALOG.map((report) => report.module)).size)
      .toBe(BUSINESS_REPORT_MODULES.length);
  });

  it('keeps each report actionable and searchable', () => {
    for (const report of BUSINESS_REPORT_CATALOG) {
      expect(report.workspaceRoute.startsWith('/')).toBe(true);
      expect(report.sourceRoutes.length).toBeGreaterThan(0);
      expect(report.entities.length).toBeGreaterThan(0);
      expect(report.filters.length).toBeGreaterThan(0);
      expect(report.metrics.length).toBeGreaterThan(0);
      expect(report.trigger.length).toBeGreaterThan(0);
      expect(report.exportFormats.length).toBeGreaterThan(0);
    }
  });

  it('maps source-only routes to current application workspaces', () => {
    const attendance = BUSINESS_REPORT_CATALOG.find((report) => report.id === 'report-3-1');
    const biometric = BUSINESS_REPORT_CATALOG.find((report) => report.id === 'report-3-2');
    const dispatch = BUSINESS_REPORT_CATALOG.find((report) => report.id === 'report-2-2');

    expect(attendance?.workspaceRoute).toBe('/reports');
    expect(biometric?.workspaceRoute).toBe('/imports/device-integrations');
    expect(dispatch?.workspaceRoute).toBe('/workforce/labor-requests');
  });
});
