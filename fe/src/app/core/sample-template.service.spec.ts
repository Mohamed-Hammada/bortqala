import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { SampleTemplateService } from './sample-template.service';

describe('SampleTemplateService', () => {
  let service: SampleTemplateService;
  let downloadSpy: any;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), SampleTemplateService],
    });
    service = TestBed.inject(SampleTemplateService);
    downloadSpy = vi.spyOn(service, 'download').mockResolvedValue();
  });

  it('downloads attendance template', async () => {
    await service.attendance();
    expect(downloadSpy).toHaveBeenCalledWith('/api/v1/attendance/imports/sample-template?format=xlsx', 'biometric-attendance-sample.xlsx');
  });

  it('downloads smart import template', async () => {
    await service.smartImport('EMPLOYEE_MASTER', 'employee-master-sample.xlsx');
    expect(downloadSpy).toHaveBeenCalledWith('/api/v1/smart-import/EMPLOYEE_MASTER/sample-template', 'employee-master-sample.xlsx');
  });

  it('downloads workforce workers and attendance templates', async () => {
    await service.workforceWorkers();
    expect(downloadSpy).toHaveBeenCalledWith('/api/v1/workforce/imports/sample-template?type=WORKERS', 'contractor-workers-sample.xlsx');

    await service.workforceAttendance();
    expect(downloadSpy).toHaveBeenCalledWith('/api/v1/workforce/imports/sample-template?type=ATTENDANCE', 'workforce-attendance-sample.xlsx');
  });

  it('downloads bank statement, translations, and supplier documents templates', async () => {
    await service.bankStatement();
    expect(downloadSpy).toHaveBeenCalledWith('/api/v1/finance/bank-reconciliation/sample-template', 'bank-statement-sample.xlsx');

    await service.translations();
    expect(downloadSpy).toHaveBeenCalledWith('/api/v1/i18n/admin/translations/sample-template', 'translations-sample.xlsx');

    await service.supplierDocuments();
    expect(downloadSpy).toHaveBeenCalledWith('/api/v1/parties/documents/sample-template', 'supplier-document-requirements.xlsx');
  });
});
