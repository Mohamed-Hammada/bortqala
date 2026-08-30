import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { OutboxPageResponse, OutboxStatsResponse } from './system-outbox.models';

@Injectable({ providedIn: 'root' })
export class SystemOutboxService {
  private readonly http = inject(HttpClient);

  listEvents(params?: { status?: string; page?: number; size?: number }): Observable<OutboxPageResponse> {
    let httpParams = new HttpParams();
    if (params?.status) httpParams = httpParams.set('status', params.status);
    if (params?.page != null) httpParams = httpParams.set('page', params.page.toString());
    if (params?.size != null) httpParams = httpParams.set('size', params.size.toString());

    return this.http.get<OutboxPageResponse>('/api/v1/system/outbox/events', { params: httpParams });
  }

  getStats(): Observable<OutboxStatsResponse> {
    return this.http.get<OutboxStatsResponse>('/api/v1/system/outbox/stats');
  }

  retryEvent(id: string): Observable<void> {
    return this.http.post<void>(`/api/v1/system/outbox/events/${id}/retry`, {});
  }
}
