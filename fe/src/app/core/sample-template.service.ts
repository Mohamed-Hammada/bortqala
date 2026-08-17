import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { downloadBlob } from './download';

@Injectable({ providedIn: 'root' })
export class SampleTemplateService {
  private readonly http = inject(HttpClient);

  async download(endpoint: string, fileName: string): Promise<void> {
    const blob = await firstValueFrom(this.http.get(endpoint, { responseType: 'blob' }));
    downloadBlob(blob, fileName);
  }

  attendance() { return this.download('/api/v1/attendance/imports/sample-template?format=xlsx', 'biometric-attendance-sample.xlsx'); }
  smartImport(entityType: 'EMPLOYEE_MASTER'|'CHART_OF_ACCOUNTS'|'BUSINESS_PARTIES'|'INVENTORY_ITEMS'|'BOM_MASTER', fileName: string) { return this.download(`/api/v1/smart-import/${entityType}/sample-template`, fileName); }
  workforceWorkers() { return this.download('/api/v1/workforce/imports/sample-template?type=WORKERS', 'contractor-workers-sample.xlsx'); }
  workforceAttendance() { return this.download('/api/v1/workforce/imports/sample-template?type=ATTENDANCE', 'workforce-attendance-sample.xlsx'); }
  bankStatement() { return this.download('/api/v1/finance/bank-reconciliation/sample-template', 'bank-statement-sample.xlsx'); }
  translations() { return this.download('/api/v1/i18n/admin/translations/sample-template', 'translations-sample.xlsx'); }
  supplierDocuments() { return this.download('/api/v1/parties/documents/sample-template', 'supplier-document-requirements.xlsx'); }
}
