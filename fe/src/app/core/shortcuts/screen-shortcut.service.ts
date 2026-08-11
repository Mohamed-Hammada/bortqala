import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import {
  ReplaceScreenShortcutsRequest,
  ScreenShortcut,
  ScreenShortcutProfile,
} from './screen-shortcut.models';

@Injectable({ providedIn: 'root' })
export class ScreenShortcutService {
  private readonly http = inject(HttpClient);

  readonly profile = signal<ScreenShortcutProfile | null>(null);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);

  readonly runtimeShortcuts = computed(() =>
    (this.profile()?.shortcuts ?? []).filter(
      (item) => item.enabled && item.availabilityStatus === 'AVAILABLE',
    ),
  );

  private readonly channel =
    typeof BroadcastChannel === 'undefined'
      ? null
      : new BroadcastChannel('bemo-screen-shortcuts');

  constructor() {
    this.channel?.addEventListener('message', (event) => {
      if (event.data?.type === 'SHORTCUT_PROFILE_CHANGED') {
        this.profile.set(event.data.profile);
      }
    });
  }

  async load(): Promise<ScreenShortcutProfile | null> {
    this.loading.set(true);
    this.error.set(null);

    try {
      const profile = await firstValueFrom(
        this.http.get<ScreenShortcutProfile>(
          '/api/v1/auth/preferences/shortcuts',
        ),
      );
      this.profile.set(profile);
      return profile;
    } catch {
      this.error.set('shortcuts.loadFailed');
      return null;
    } finally {
      this.loading.set(false);
    }
  }

  async replace(
    request: ReplaceScreenShortcutsRequest,
  ): Promise<ScreenShortcutProfile> {
    this.saving.set(true);
    this.error.set(null);

    try {
      const profile = await firstValueFrom(
        this.http.put<ScreenShortcutProfile>(
          '/api/v1/auth/preferences/shortcuts',
          request,
        ),
      );
      this.profile.set(profile);
      this.broadcastChange(profile);
      return profile;
    } finally {
      this.saving.set(false);
    }
  }

  async reset(): Promise<ScreenShortcutProfile> {
    this.saving.set(true);
    this.error.set(null);

    try {
      const profile = await firstValueFrom(
        this.http.post<ScreenShortcutProfile>(
          '/api/v1/auth/preferences/shortcuts/reset',
          {},
        ),
      );
      this.profile.set(profile);
      this.broadcastChange(profile);
      return profile;
    } finally {
      this.saving.set(false);
    }
  }

  findByCode(secondKeyCode: string): ScreenShortcut | undefined {
    return this.runtimeShortcuts().find(
      (item) => item.secondKeyCode === secondKeyCode,
    );
  }

  private broadcastChange(profile: ScreenShortcutProfile): void {
    if (typeof BroadcastChannel === 'undefined') return;
    const channel = new BroadcastChannel('bemo-screen-shortcuts');
    channel.postMessage({ type: 'SHORTCUT_PROFILE_CHANGED', profile });
    channel.close();
  }
}
