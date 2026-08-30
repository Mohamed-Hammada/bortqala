import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  EtaConfig,
  EtaDocumentType,
  EtaItemMapping,
  EtaSubmission,
  EtaSubmissionStatus,
  EtaSummary,
  SaveEtaConfigRequest,
  SaveEtaItemMappingRequest,
} from './eta-tax.models';

@Injectable({ providedIn: 'root' })
export class EtaTaxService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/compliance/eta';

  getConfig(): Observable<EtaConfig | null> {
    return this.http.get<EtaConfig | null>(`${this.baseUrl}/config`);
  }

  saveConfig(req: SaveEtaConfigRequest): Observable<EtaConfig> {
    return this.http.post<EtaConfig>(`${this.baseUrl}/config`, req);
  }

  getSummary(): Observable<EtaSummary> {
    return this.http.get<EtaSummary>(`${this.baseUrl}/summary`);
  }

  getSubmissions(status?: EtaSubmissionStatus, documentType?: EtaDocumentType): Observable<EtaSubmission[]> {
    let params = new HttpParams();
    if (status) params = params.set('status', status);
    if (documentType) params = params.set('documentType', documentType);
    return this.http.get<EtaSubmission[]>(`${this.baseUrl}/submissions`, { params });
  }

  queueInvoice(invoiceId: string, documentType: EtaDocumentType): Observable<EtaSubmission> {
    return this.http.post<EtaSubmission>(`${this.baseUrl}/submissions/queue`, { invoiceId, documentType });
  }

  submitToEta(submissionId: string): Observable<EtaSubmission> {
    return this.http.post<EtaSubmission>(`${this.baseUrl}/submissions/${submissionId}/submit`, {});
  }

  cancelDocument(submissionId: string, reason: string): Observable<EtaSubmission> {
    return this.http.post<EtaSubmission>(`${this.baseUrl}/submissions/${submissionId}/cancel`, { reason });
  }

  getItemMappings(): Observable<EtaItemMapping[]> {
    return this.http.get<EtaItemMapping[]>(`${this.baseUrl}/item-mappings`);
  }

  saveItemMapping(req: SaveEtaItemMappingRequest): Observable<EtaItemMapping> {
    return this.http.post<EtaItemMapping>(`${this.baseUrl}/item-mappings`, req);
  }
}
