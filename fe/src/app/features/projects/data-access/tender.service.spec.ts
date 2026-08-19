import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TenderService } from './tender.service';
import { ProjectTender } from '../models/tender.models';

describe('TenderService', () => {
  let service: TenderService;
  let httpMock: HttpTestingController;

  const mockTender: ProjectTender = {
    id: 'tnd-1',
    tenderNumber: 'TND-2026-001',
    title: 'Hospital MEP Tender',
    tenderType: 'INTERNAL',
    submissionDeadline: Date.now() + 86400000 * 10,
    estimatedValue: 20000000,
    currencyCode: 'EGP',
    technicalWeightPercent: 70,
    financialWeightPercent: 30,
    bidBondRequired: true,
    bidBondAmount: 400000,
    bidBondValidityDays: 90,
    status: 'DRAFT',
    boqItemsCount: 5,
    biddersCount: 3,
    createdAt: Date.now(),
    updatedAt: Date.now(),
    version: 1
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        TenderService
      ]
    });
    service = TestBed.inject(TenderService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('loads tenders into signal', () => {
    service.loadTenders().subscribe(res => {
      expect(res.length).toBe(1);
      expect(service.tenders().length).toBe(1);
    });

    const req = httpMock.expectOne('/api/v1/project-tenders');
    expect(req.request.method).toBe('GET');
    req.flush([mockTender]);
  });

  it('creates tender and updates signal', () => {
    service.createTender({
      title: 'Hospital MEP Tender',
      tenderType: 'INTERNAL',
      submissionDeadline: Date.now() + 86400000,
      bidBondRequired: false
    }).subscribe(res => {
      expect(res.id).toBe('tnd-1');
      expect(service.tenders()[0].id).toBe('tnd-1');
    });

    const req = httpMock.expectOne('/api/v1/project-tenders');
    expect(req.request.method).toBe('POST');
    req.flush(mockTender);
  });

  it('publishes tender', () => {
    const published = { ...mockTender, status: 'PUBLISHED' as const };
    service.publishTender('tnd-1').subscribe(res => {
      expect(res.status).toBe('PUBLISHED');
    });

    const req = httpMock.expectOne('/api/v1/project-tenders/tnd-1/publish');
    expect(req.request.method).toBe('POST');
    req.flush(published);
  });
});
