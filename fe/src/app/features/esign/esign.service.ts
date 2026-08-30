import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  CreatePacketRequest,
  ManifestExport,
  PacketStatus,
  SignaturePacket,
  SignatureStep,
  SignStepRequest,
} from './esign.models';

@Injectable({ providedIn: 'root' })
export class ESignService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/signatures';

  listPackets(status?: PacketStatus): Observable<SignaturePacket[]> {
    const params = status ? new HttpParams().set('status', status) : undefined;
    return this.http.get<SignaturePacket[]>(`${this.baseUrl}/packets`, { params });
  }

  createPacket(req: CreatePacketRequest): Observable<SignaturePacket> {
    return this.http.post<SignaturePacket>(`${this.baseUrl}/packets`, req);
  }

  startRouting(id: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/packets/${id}/start-routing`, {});
  }

  signStep(id: string, stepOrder: number, req: SignStepRequest): Observable<SignatureStep> {
    return this.http.post<SignatureStep>(`${this.baseUrl}/packets/${id}/steps/${stepOrder}/sign`, req);
  }

  declineStep(id: string, stepOrder: number, reason: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/packets/${id}/steps/${stepOrder}/decline`, { reason });
  }

  exportManifest(id: string): Observable<ManifestExport> {
    return this.http.get<ManifestExport>(`${this.baseUrl}/packets/${id}/manifest`);
  }
}