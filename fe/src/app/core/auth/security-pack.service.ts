import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import {
  RoleIpRuleCreateRequest,
  RoleIpRuleResponse,
  SecurityPolicyResponse,
  SecurityPolicyUpdateRequest,
  TotpEnrollResponse,
  TotpStatusResponse,
  TrustedDeviceResponse,
} from './security-pack.models';
import { LoginResponse } from './auth.models';

@Injectable({ providedIn: 'root' })
export class SecurityPackService {
  private readonly http = inject(HttpClient);

  readonly totpStatusSignal = signal<TotpStatusResponse | null>(null);
  readonly policySignal = signal<SecurityPolicyResponse | null>(null);
  readonly devicesSignal = signal<TrustedDeviceResponse[]>([]);
  readonly ipRulesSignal = signal<RoleIpRuleResponse[]>([]);

  readonly totpStatus = this.totpStatusSignal.asReadonly();
  readonly policy = this.policySignal.asReadonly();
  readonly devices = this.devicesSignal.asReadonly();
  readonly ipRules = this.ipRulesSignal.asReadonly();

  // --- TOTP 2FA ---

  getTotpStatus(): Observable<TotpStatusResponse> {
    return this.http
      .get<TotpStatusResponse>('/api/v1/auth/2fa/status')
      .pipe(tap((status) => this.totpStatusSignal.set(status)));
  }

  enrollTotp(): Observable<TotpEnrollResponse> {
    return this.http.post<TotpEnrollResponse>('/api/v1/auth/2fa/enroll', {});
  }

  activateTotp(code: string): Observable<void> {
    return this.http
      .post<void>('/api/v1/auth/2fa/activate', { code })
      .pipe(tap(() => this.getTotpStatus().subscribe()));
  }

  disableTotp(password: string): Observable<void> {
    return this.http
      .post<void>('/api/v1/auth/2fa/disable', { password })
      .pipe(tap(() => this.getTotpStatus().subscribe()));
  }

  regenerateBackupCodes(codeOrPassword: string): Observable<string[]> {
    return this.http.post<string[]>('/api/v1/auth/2fa/backup-codes/regenerate', {
      codeOrPassword,
    });
  }

  verify2fa(challengeToken: string, code: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>('/api/v1/auth/2fa/verify', {
      challengeToken,
      code,
    });
  }

  // --- Password Policy ---

  getPolicy(): Observable<SecurityPolicyResponse> {
    return this.http
      .get<SecurityPolicyResponse>('/api/v1/security/policy')
      .pipe(tap((policy) => this.policySignal.set(policy)));
  }

  updatePolicy(request: SecurityPolicyUpdateRequest): Observable<SecurityPolicyResponse> {
    return this.http
      .put<SecurityPolicyResponse>('/api/v1/security/policy', request)
      .pipe(tap((policy) => this.policySignal.set(policy)));
  }

  // --- Trusted Devices ---

  loadDevices(): Observable<TrustedDeviceResponse[]> {
    return this.http
      .get<TrustedDeviceResponse[]>('/api/v1/security/devices')
      .pipe(tap((devices) => this.devicesSignal.set(devices)));
  }

  revokeDevice(deviceId: string): Observable<void> {
    return this.http
      .post<void>(`/api/v1/security/devices/${deviceId}/revoke`, {})
      .pipe(
        tap(() => {
          this.devicesSignal.update((list) =>
            list.map((d) =>
              d.id === deviceId
                ? { ...d, revoked: true, revokedAt: new Date().toISOString() }
                : d
            )
          );
        })
      );
  }

  // --- Role IP Allowlists ---

  loadIpRules(): Observable<RoleIpRuleResponse[]> {
    return this.http
      .get<RoleIpRuleResponse[]>('/api/v1/security/ip-rules')
      .pipe(tap((rules) => this.ipRulesSignal.set(rules)));
  }

  createIpRule(request: RoleIpRuleCreateRequest): Observable<RoleIpRuleResponse> {
    return this.http
      .post<RoleIpRuleResponse>('/api/v1/security/ip-rules', request)
      .pipe(
        tap((rule) => {
          this.ipRulesSignal.update((prev) => [rule, ...prev]);
        })
      );
  }

  deleteIpRule(ruleId: string): Observable<void> {
    return this.http
      .delete<void>(`/api/v1/security/ip-rules/${ruleId}`)
      .pipe(
        tap(() => {
          this.ipRulesSignal.update((prev) => prev.filter((r) => r.id !== ruleId));
        })
      );
  }
}
