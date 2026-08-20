import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { BusinessVertical, ConfigureVerticalRequest, TenantVerticalResponse } from './tenant-setup.models';

@Injectable({ providedIn: 'root' })
export class TenantSetupService {
  private readonly http = inject(HttpClient);

  getVerticalSetup(): Observable<TenantVerticalResponse> {
    return this.http.get<TenantVerticalResponse>('/api/v1/tenant/vertical-setup');
  }

  configureVertical(vertical: BusinessVertical): Observable<TenantVerticalResponse> {
    const payload: ConfigureVerticalRequest = { vertical };
    return this.http.post<TenantVerticalResponse>('/api/v1/tenant/vertical-setup', payload);
  }
}
