import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { FixedAssetsPage } from './fixed-assets.page';
import { I18nService } from '../../../core/i18n.service';
import { NotificationService } from '../../../core/notification.service';
import { DepreciationRunResponse, FixedAssetResponse } from './fixed-assets.models';

describe('FixedAssetsPage', () => {
  let httpMock: HttpTestingController;
  let page: FixedAssetsPage;

  const asset = (overrides: Partial<FixedAssetResponse> = {}): FixedAssetResponse => ({
    id: 'asset-1',
    name: 'Delivery van',
    category: 'VEHICLE',
    acquisitionDate: Date.UTC(2026, 0, 15),
    acquisitionCost: 12000,
    salvageValue: 0,
    usefulLifeMonths: 12,
    monthlyCharge: 1000,
    accumulatedDepreciation: 2000,
    netBookValue: 10000,
    lastPostedYearMonth: '2026-03',
    status: 'ACTIVE',
    disposalDate: null,
    disposalProceeds: null,
    branchId: null,
    costCenterId: null,
    version: 2,
    ...overrides,
  });

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FixedAssetsPage],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: I18nService, useValue: { t: (key: string) => key } },
        {
          provide: NotificationService,
          useValue: { success: () => undefined, error: () => undefined, warning: () => undefined },
        },
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(FixedAssetsPage);
    page = fixture.componentInstance;

    flushInitialLoad();
  });

  afterEach(() => {
    TestBed.resetTestingModule();
    httpMock.verify();
  });

  function flushInitialLoad() {
    httpMock.expectOne((req) => req.url === '/api/v1/fixed-assets').flush([asset()]);
  }

  async function yieldMicrotasks(): Promise<void> {
    await Promise.resolve();
    await Promise.resolve();
  }

  it('loads assets on init', () => {
    expect(page.assets()).toHaveLength(1);
    expect(page.loading()).toBe(false);
    expect(page.openAssetsCount()).toBe(1);
  });

  it('creates an asset with a UTC-epoch acquisition date', async () => {
    page.assetForm.patchValue({
      name: 'Lathe',
      category: 'MACHINERY',
      acquisitionDate: '2026-01-15',
      acquisitionCost: 10000,
      salvageValue: 1000,
      usefulLifeMonths: 36,
    });
    const submitting = page.submitAsset();

    const req = httpMock.expectOne((r) => r.url === '/api/v1/fixed-assets' && r.method === 'POST');
    expect(req.request.body).toEqual({
      name: 'Lathe',
      category: 'MACHINERY',
      acquisitionDate: Date.UTC(2026, 0, 15),
      acquisitionCost: 10000,
      salvageValue: 1000,
      usefulLifeMonths: 36,
    });
    req.flush(asset({ id: 'asset-2', name: 'Lathe', category: 'MACHINERY' }));
    await yieldMicrotasks();
    flushInitialLoad();
    await submitting;

    expect(page.drawerOpen()).toBe(false);
  });

  it('updates an existing asset via PUT', async () => {
    page.openEdit(asset({ name: 'Delivery van' }));
    expect(page.editingId()).toBe('asset-1');
    page.assetForm.patchValue({ name: 'Van 2', acquisitionCost: 13000 });
    const submitting = page.submitAsset();

    const req = httpMock.expectOne((r) => r.url === '/api/v1/fixed-assets/asset-1' && r.method === 'PUT');
    expect(req.request.body).toMatchObject({ name: 'Van 2', acquisitionCost: 13000 });
    req.flush(asset({ name: 'Van 2', acquisitionCost: 13000 }));
    await yieldMicrotasks();
    flushInitialLoad();
    await submitting;
  });

  it('disposes an asset and reloads the register', async () => {
    page.openDispose(asset());
    expect(page.disposingAsset()?.id).toBe('asset-1');
    page.disposeForm.patchValue({ disposalDate: '2026-06-30', proceeds: 9000 });
    const submitting = page.submitDispose();

    const req = httpMock.expectOne(
      (r) => r.url === '/api/v1/fixed-assets/asset-1/dispose' && r.method === 'POST',
    );
    expect(req.request.body).toEqual({
      disposalDate: Date.UTC(2026, 5, 30),
      proceeds: 9000,
    });
    req.flush(asset({ status: 'DISPOSED', disposalDate: Date.UTC(2026, 5, 30), disposalProceeds: 9000 }));
    await yieldMicrotasks();
    flushInitialLoad();
    await yieldMicrotasks();
    await submitting;

    expect(page.disposingAsset()).toBeNull();
  });

  it('runs depreciation for a month and surfaces the result summary', async () => {
    page.runForm.patchValue({ yearMonth: '2026-02' });
    const run: DepreciationRunResponse = {
      yearMonth: '2026-02',
      postedCount: 1,
      resultCount: 1,
      totalCharge: 1000,
      results: [
        { assetId: 'asset-1', assetName: 'Delivery van', charge: 1000, outcome: 'POSTED', entryNumber: 'JV-100' },
      ],
    };
    const submitting = page.submitRun();

    const req = httpMock.expectOne(
      (r) => r.url.includes('/api/v1/fixed-assets/run-depreciation') && r.method === 'POST',
    );
    expect(req.request.urlWithParams).toContain('yearMonth=2026-02');
    req.flush(run);
    await yieldMicrotasks();
    flushInitialLoad();
    await yieldMicrotasks();
    await submitting;

    expect(page.runResult()?.postedCount).toBe(1);
    expect(page.runResult()?.results[0].entryNumber).toBe('JV-100');
  });

  it('openRun defaults the month picker to the previous month', () => {
    page.openRun();
    const now = new Date();
    const expected = `${now.getUTCFullYear()}-${String(now.getUTCMonth()).padStart(2, '0')}`;
    expect(page.runForm.controls.yearMonth.value).toBe(expected);
  });

  it('maps categories and statuses to translation keys', () => {
    expect(page.categoryLabel('VEHICLE')).toBe('category.vehicle');
    expect(page.categoryLabel('OTHER')).toBe('category.other');
    expect(page.statusLabel('ACTIVE')).toBe('status.active');
    expect(page.statusLabel('FULLY_DEPRECIATED')).toBe('status.fullyDepreciated');
    expect(page.statusLabel('DISPOSED')).toBe('status.disposed');
    expect(page.outcomeLabel('ALREADY_POSTED')).toBe('outcome.ALREADY_POSTED');
    expect(page.outcomeLabel('SOMETHING_ELSE')).toBe('SOMETHING_ELSE');
  });

  it('blocks submit when the form is invalid', () => {
    page.openNew();
    page.submitAsset();
    httpMock.expectNone(() => true);
    expect(page.drawerOpen()).toBe(true);
  });
});
