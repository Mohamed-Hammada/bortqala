import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

export interface NotificationAppSummary { id: string; code: string; name: string; }
export interface NotificationUserSummary { username: string; displayName: string; active: boolean; }
export interface ExcelPreview {
  totalRows: number; validCount: number; duplicateCount: number; notFoundCount: number; inactiveCount: number;
  validUsernames: string[]; duplicateUsernames: string[]; notFoundUsernames: string[]; inactiveUsernames: string[];
}
export interface BulkSendResult { bulkId: string; requested: number; created: number; skippedMissing: number; skippedInactive: number; }
export interface BulkSendPayload {
  targetAppId: string; mode: 'USERS' | 'EXCEL' | 'APP'; usernames: string[]; titleAr: string; titleEn: string;
  messageAr: string; messageEn: string; notificationType: string; priority: 'INFO' | 'MEDIUM' | 'HIGH' | 'CRITICAL'; actionLink: string | null;
}

@Injectable({ providedIn: 'root' })
export class NotificationAdminService {
  private readonly http = inject(HttpClient);
  apps() { return this.http.get<NotificationAppSummary[]>('/api/v1/notifications/admin/apps'); }
  users(appId: string, q = '') { return this.http.get<NotificationUserSummary[]>('/api/v1/notifications/admin/users', { params: { appId, q } }); }
  previewExcel(appId: string, file: File) {
    const form = new FormData(); form.append('file', file);
    return this.http.post<ExcelPreview>('/api/v1/notifications/admin/excel/preview', form, { params: { appId } });
  }
  send(payload: BulkSendPayload) { return this.http.post<BulkSendResult>('/api/v1/notifications/admin/send', payload); }
}
