import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, provideRouter } from '@angular/router';
import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { ReportsPage } from './reports.page';
import { I18nService } from '../../core/i18n.service';
import { ReportsStore } from './reports.store';
import { GeneratedPeriod, PeriodOption } from './reports.models';

describe('ReportsPage REM-006 period presets', () => {
  let httpMock: HttpTestingController;
  let page: ReportsPage;
  let fixture: ReturnType<typeof TestBed.createComponent<ReportsPage>>;

  const period: PeriodOption = {
    year: 2026,
    month: 8,
    kind: 'SECOND_HALF',
    start: 1786406400000,
    end: 1787356799999,
  };

  beforeEach(async () => {
    Object.defineProperty(HTMLElement.prototype, 'scrollIntoView', {
      configurable: true,
      value: () => undefined,
    });
    await TestBed.configureTestingModule({
      imports: [ReportsPage],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: I18nService,
          useValue: {
            locale: () => 'ar-EG',
            t: (key: string, params?: Record<string, string>) => params ? `${key}:${params['period']}` : key,
            use: () => undefined,
          },
        },
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(ReportsPage);
    page = fixture.componentInstance;

    flushInitialLoads();
  });

  afterEach(() => {
    TestBed.resetTestingModule();
    httpMock.verify();
  });

  function flushInitialLoads(options: { generated?: GeneratedPeriod[] } = {}) {
    httpMock.expectOne((req) => req.method === 'GET' && req.url === '/api/v1/reports').flush([]);
    httpMock.expectOne((req) => req.method === 'GET' && req.url.includes('/available-periods')).flush([period]);
    httpMock
      .expectOne((req) => req.method === 'GET' && req.url.includes('/generated-periods'))
      .flush(options.generated ?? []);
    httpMock.expectOne((req) => req.method === 'GET' && req.url === '/api/v1/admin/app-settings').flush({});
    httpMock.expectOne((req) => req.method === 'GET' && req.url === '/api/v1/data-exchange/catalog').flush([]);
  }

  it('fills the period form on preset click and does not create a report', () => {
    page.applyPreset(period);

    expect(page.periodForm.getRawValue().periodStart).toBeDefined();
    expect(page.periodForm.getRawValue().payCycle).toBe('HALF_MONTHLY');
    const created = httpMock.match((req) => req.method === 'POST' && req.url.includes('/api/v1/reports'));
    expect(created).toHaveLength(0);
  });

  it('preset card is a non-destructive fill-only shortcut with an action aria label', () => {
    const name = page.periodName(period);
    const aria = page.i18n.t('reports.usePeriodAria', { period: name });
    expect(aria).toContain(name);
    expect(aria).toContain('reports.usePeriodAria');
  });

  const generatedAugust: GeneratedPeriod = {
    from: 1785531600000,
    to: 1788209999999,
    type: 'MONTHLY',
    reportId: 'rep-aug',
  };

  it('WP-06: finalized period disables its chip with the translated tooltip and links to the exact report', () => {
    page.store.generated.set([generatedAugust]);
    page.store.generatedLoading.set(false);
    fixture.detectChanges();

    const chips = Array.from(fixture.nativeElement.querySelectorAll('.periods .preset-item')) as HTMLElement[];
    const locked = chips.find((el) => el.classList.contains('generated'));
    expect(locked).toBeDefined();
    expect(page.generatedFor(period)?.reportId).toBe('rep-aug');

    const button = locked!.querySelector('button') as HTMLButtonElement;
    expect(button.disabled).toBe(true);
    expect(button.getAttribute('title')).toBe('reports.alreadyGenerated');
    expect(locked!.textContent).toContain('reports.alreadyGenerated');

    const link = locked!.querySelector('a.view-existing') as HTMLAnchorElement;
    expect(link.getAttribute('href')).toBe('/reports/rep-aug');
    expect(link.textContent).toContain('reports.viewExisting');
  });

  it('WP-06: without finalized reports every preset stays enabled with no view link (AC-2)', () => {
    fixture.detectChanges();

    const chips = Array.from(fixture.nativeElement.querySelectorAll('.periods .preset-item')) as HTMLElement[];
    expect(chips.length).toBeGreaterThan(0);
    for (const chip of chips) {
      expect(chip.classList.contains('generated')).toBe(false);
      expect(chip.querySelector('a.view-existing')).toBeNull();
      expect((chip.querySelector('button') as HTMLButtonElement).disabled).toBe(false);
    }
  });

  it('WP-06: changing the year refetches the generated-period registry (AC-4)', async () => {
    page.changeYear('2025');
    await Promise.resolve();

    httpMock.expectOne((req) => req.method === 'GET' && req.url === '/api/v1/reports').flush([]);
    httpMock
      .expectOne((req) => req.method === 'GET' && req.url.includes('/available-periods') && req.params.get('year') === '2025')
      .flush([]);
    const generatedReq = httpMock.expectOne(
      (req) => req.method === 'GET' && req.url.includes('/generated-periods') && req.params.get('year') === '2025');
    generatedReq.flush([{ from: 1735689600000, to: 1738367999999, type: 'MONTHLY', reportId: 'rep-jan-25' }]);
    await new Promise((resolve) => setTimeout(resolve, 0));
    expect(page.store.generated().map((item) => item.reportId)).toEqual(['rep-jan-25']);
    expect(page.store.generatedLoading()).toBe(false);
  });
});
