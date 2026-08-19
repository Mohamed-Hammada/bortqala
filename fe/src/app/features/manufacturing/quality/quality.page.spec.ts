import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { I18nService } from '../../../core/i18n.service';
import { QualityPage } from './quality.page';

describe('QualityPage', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        QualityPage,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: I18nService, useValue: { t: (key: string) => key, locale: () => 'en-US' } },
      ],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    try {
      http.verify();
    } finally {
      TestBed.resetTestingModule();
    }
  });

  it('loads inspections on init', async () => {
    const page = TestBed.inject(QualityPage);

    http.expectOne('/api/v1/manufacturing/quality').flush([
      {
        id: 'qi-1',
        inspectionNumber: 'QC-1001',
        inspectionDate: 1000,
        sourceType: 'INCOMING_GRN',
        passedQuantity: 95,
        failedQuantity: 5,
        status: 'PASSED',
        inspectorName: 'Quality Officer',
        notes: 'Passed sample test',
        createdAt: 1000,
      },
    ]);

    await Promise.resolve();
    expect(page.inspections().length).toBe(1);
    expect(page.inspections()[0].inspectionNumber).toBe('QC-1001');
  });

  it('opens new inspection modal and submits', async () => {
    const page = TestBed.inject(QualityPage);
    http.expectOne('/api/v1/manufacturing/quality').flush([]);

    page.openNew();
    expect(page.drawerOpen()).toBe(true);

    page.qiForm.patchValue({
      inspectionNumber: 'QC-1002',
      passedQuantity: 100,
      failedQuantity: 0,
    });

    const submitPromise = page.submitQi();

    const postReq = http.expectOne(r => r.url === '/api/v1/manufacturing/quality' && r.method === 'POST');
    expect(postReq.request.body.inspectionNumber).toBe('QC-1002');
    postReq.flush({});

    await Promise.resolve();
    await Promise.resolve();

    const getReq = http.expectOne(r => r.url === '/api/v1/manufacturing/quality' && r.method === 'GET');
    getReq.flush([]);

    await submitPromise;
    expect(page.drawerOpen()).toBe(false);
  });
});
