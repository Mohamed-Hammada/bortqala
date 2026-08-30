import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, map, tap } from 'rxjs';
import {
  EnrolledDevice,
  EnrollDeviceRequest,
  SigningChallenge,
  VerificationResult,
} from './device-signing.models';

@Injectable({ providedIn: 'root' })
export class DeviceSigningService {
  private readonly http = inject(HttpClient);
  private readonly devicesSignal = signal<EnrolledDevice[]>([]);

  readonly devices = this.devicesSignal.asReadonly();

  loadDevices(): Observable<EnrolledDevice[]> {
    return this.http
      .get<{ devices: EnrolledDevice[] }>('/api/v1/auth/devices')
      .pipe(
        map((res) => res.devices || []),
        tap((list) => this.devicesSignal.set(list))
      );
  }

  enrollDevice(request: EnrollDeviceRequest): Observable<EnrolledDevice> {
    return this.http
      .post<EnrolledDevice>('/api/v1/auth/devices/enroll', request)
      .pipe(
        tap((device) => {
          this.devicesSignal.update((prev) => [device, ...prev]);
        })
      );
  }

  revokeDevice(deviceId: string, reason?: string): Observable<void> {
    return this.http
      .post<void>(`/api/v1/auth/devices/${deviceId}/revoke`, { reason: reason || 'REVOKED_BY_USER' })
      .pipe(
        tap(() => {
          this.devicesSignal.update((prev) =>
            prev.map((d) => (d.id === deviceId ? { ...d, status: 'REVOKED' as const } : d))
          );
        })
      );
  }

  requestChallenge(
    deviceId: string,
    operationType: string,
    payload: string
  ): Observable<SigningChallenge> {
    return this.http.post<SigningChallenge>('/api/v1/auth/devices/challenge', {
      deviceId,
      operationType,
      payload,
    });
  }

  verifySignature(
    challengeId: string,
    signature: string,
    payload: string
  ): Observable<VerificationResult> {
    return this.http.post<VerificationResult>('/api/v1/auth/devices/verify', {
      challengeId,
      signature,
      payload,
    });
  }
}
