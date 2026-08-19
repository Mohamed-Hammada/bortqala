import { describe, it, expect, beforeEach } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { DprService } from './dpr.service';
import { DailyReportResponse, CreateDailyReportRequest } from '../models/dpr.models';

describe('DprService', () => {
  let service: DprService;
  let httpMock: HttpTestingController;

  const mockReport: DailyReportResponse = {
    id: 'dpr-1',
    projectId: 'p-1',
    reportNumber: 'DPR-PRJ-001-2026-03-01-DAY',
    reportDate: 1772323200000,
    shift: 'DAY',
    weatherCondition: 'SUNNY',
    temperatureCelsius: 28,
    status: 'DRAFT',
    siteEngineerUserId: 'u-1',
    totalWorkforceCount: 15,
    totalEquipmentCount: 4,
    totalManHours: 120,
    createdAt: 1772323200000,
    updatedAt: 1772323200000,
    version: 0,
    progressLines: [],
    laborSnapshots: [],
    equipmentLogs: [],
    materialConsumptions: [],
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [DprService],
    });
    service = TestBed.inject(DprService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  it('loadReports loads and sets reports signal', () => {
    service.loadReports('p-1').subscribe((reports) => {
      expect(reports).toHaveLength(1);
      expect(reports[0].id).toBe('dpr-1');
    });

    const req = httpMock.expectOne('/api/v1/projects/p-1/daily-reports');
    expect(req.request.method).toBe('GET');
    req.flush([mockReport]);

    expect(service.reports()).toHaveLength(1);
    expect(service.loading()).toBe(false);
  });

  it('createReport posts payload and updates reports signal', () => {
    const createReq: CreateDailyReportRequest = {
      reportDate: 1772323200000,
      shift: 'DAY',
      weatherCondition: 'SUNNY',
    };

    service.createReport('p-1', createReq).subscribe((created) => {
      expect(created.id).toBe('dpr-1');
    });

    const req = httpMock.expectOne('/api/v1/projects/p-1/daily-reports');
    expect(req.request.method).toBe('POST');
    req.flush(mockReport);

    expect(service.reports()).toHaveLength(1);
  });

  it('submitReport updates status to SUBMITTED', () => {
    service.reports.set([mockReport]);

    const updated: DailyReportResponse = { ...mockReport, status: 'SUBMITTED' };
    service.submitReport('p-1', 'dpr-1').subscribe((res) => {
      expect(res.status).toBe('SUBMITTED');
    });

    const req = httpMock.expectOne('/api/v1/projects/p-1/daily-reports/dpr-1/submit');
    expect(req.request.method).toBe('POST');
    req.flush(updated);

    expect(service.reports()[0].status).toBe('SUBMITTED');
  });

  it('approveReport updates status to APPROVED', () => {
    service.reports.set([mockReport]);

    const updated: DailyReportResponse = { ...mockReport, status: 'APPROVED' };
    service.approveReport('p-1', 'dpr-1').subscribe((res) => {
      expect(res.status).toBe('APPROVED');
    });

    const req = httpMock.expectOne('/api/v1/projects/p-1/daily-reports/dpr-1/approve');
    expect(req.request.method).toBe('POST');
    req.flush(updated);

    expect(service.reports()[0].status).toBe('APPROVED');
  });
});
