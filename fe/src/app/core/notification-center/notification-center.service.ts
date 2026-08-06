import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

export interface BusinessNotificationItem {
  id: string;
  recipientUsername: string;
  titleAr: string;
  titleEn: string;
  messageAr: string;
  messageEn: string;
  notificationType: string;
  priority: string;
  actionLink?: string;
  isRead: boolean;
  readAt?: number;
  createdAt: number;
}

@Injectable({ providedIn: 'root' })
export class NotificationCenterService {
  private readonly http = inject(HttpClient);

  readonly unreadCount = signal(0);
  readonly notifications = signal<BusinessNotificationItem[]>([]);
  readonly loading = signal(false);
  readonly panelOpen = signal(false);

  async loadUnreadCount(): Promise<void> {
    try {
      const res = await firstValueFrom(this.http.get<{ unreadCount: number }>('/api/v1/notifications/unread-count'));
      this.unreadCount.set(res.unreadCount);
    } catch {
      // Ignore background errors
    }
  }

  async loadNotifications(): Promise<void> {
    this.loading.set(true);
    try {
      const list = await firstValueFrom(this.http.get<BusinessNotificationItem[]>('/api/v1/notifications'));
      this.notifications.set(list);
      this.unreadCount.set(list.filter(n => !n.isRead).length);
    } catch {
      // Ignore background errors
    } finally {
      this.loading.set(false);
    }
  }

  async markAsRead(id: string): Promise<void> {
    try {
      await firstValueFrom(this.http.post(`/api/v1/notifications/${id}/read`, {}));
      this.notifications.update(items =>
        items.map(item => item.id === id ? { ...item, isRead: true, readAt: Date.now() } : item)
      );
      this.unreadCount.update(c => Math.max(0, c - 1));
    } catch {
      // Ignore background errors
    }
  }

  async markAllAsRead(): Promise<void> {
    try {
      await firstValueFrom(this.http.post('/api/v1/notifications/read-all', {}));
      this.notifications.update(items => items.map(item => ({ ...item, isRead: true, readAt: Date.now() })));
      this.unreadCount.set(0);
    } catch {
      // Ignore background errors
    }
  }

  togglePanel(): void {
    const next = !this.panelOpen();
    this.panelOpen.set(next);
    if (next) {
      this.loadNotifications();
    }
  }

  closePanel(): void {
    this.panelOpen.set(false);
  }
}
