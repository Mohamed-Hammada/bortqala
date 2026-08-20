import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  BackupSnapshot,
  DiagnosticsResponse,
  DrRecoveryStatus,
  LicenseStatus,
} from './deployment.models';

@Injectable({
  providedIn: 'root',
})
export class DeploymentService {
  private readonly http = inject(HttpClient);

  getDiagnostics(): Observable<DiagnosticsResponse> {
    return this.http.get<DiagnosticsResponse>('/api/v1/platform/diagnostics/health');
  }

  evaluateDiagnostics(): Observable<DiagnosticsResponse> {
    return this.http.post<DiagnosticsResponse>('/api/v1/platform/diagnostics/evaluate', {});
  }

  listBackups(): Observable<BackupSnapshot[]> {
    return this.http.get<BackupSnapshot[]>('/api/v1/platform/backups');
  }

  triggerBackup(payload: { snapshotName?: string; backupType?: string }): Observable<BackupSnapshot> {
    return this.http.post<BackupSnapshot>('/api/v1/platform/backups/trigger', payload);
  }

  verifyDrill(snapshotId: string): Observable<BackupSnapshot> {
    return this.http.post<BackupSnapshot>(`/api/v1/platform/backups/${snapshotId}/verify-drill`, {});
  }

  getDrStatus(): Observable<DrRecoveryStatus> {
    return this.http.get<DrRecoveryStatus>('/api/v1/platform/backups/dr-status');
  }

  getLicenseStatus(): Observable<LicenseStatus> {
    return this.http.get<LicenseStatus>('/api/v1/platform/licensing/status');
  }

  installLicense(payload: {
    licenseKey: string;
    certificatePayload?: string;
    signatureEd25519?: string;
    deviceFingerprintHash?: string;
  }): Observable<LicenseStatus> {
    return this.http.post<LicenseStatus>('/api/v1/platform/licensing/install', payload);
  }

  validateLicense(): Observable<LicenseStatus> {
    return this.http.post<LicenseStatus>('/api/v1/platform/licensing/validate', {});
  }
}
