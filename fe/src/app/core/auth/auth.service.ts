import { HttpClient } from '@angular/common/http';
import { Injectable, computed, effect, inject, signal } from '@angular/core';
import { tap } from 'rxjs';
import { ThemeService } from '../theme.service';
import { I18nService } from '../i18n.service';
import { AppSettings, AuthUser, LoginResponse, RoleCode, UserPreferences } from './auth.models';

const STORAGE_KEY = 'hr-platform-session';
const DEFAULT_PREFERENCES: UserPreferences = {
  theme: 'SYSTEM',
  tableDensity: 'COMFORTABLE',
  locale: 'ar-EG',
  excelTableStyle: 'GOLD',
  updatedAt: null,
};

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly httpClient = inject(HttpClient);
  private readonly themeService = inject(ThemeService);
  private readonly i18nService = inject(I18nService);
  private readonly session = signal<LoginResponse | null>(this.readSession());

  readonly user = computed(() => this.session()?.user ?? null);
  readonly app = computed(() => this.session()?.app ?? null);
  readonly token = computed(() => this.session()?.accessToken ?? null);
  readonly preferences = computed(() => this.session()?.preferences ?? DEFAULT_PREFERENCES);
  readonly authenticated = computed(() => {
    const session = this.session();
    return !!session && new Date(session.expiresAt).getTime() > Date.now();
  });

  constructor() {
    effect(() => {
      const preferences = this.preferences();
      this.themeService.apply(preferences);
      void this.i18nService.use(preferences.locale);
    });
  }

  login(appCode: string, username: string, password: string) {
    return this.httpClient
      .post<LoginResponse>('/api/v1/auth/login', { appCode, username, password })
      .pipe(
        tap((session) => {
          localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
          this.session.set(session);
        }),
      );
  }

  logout(): void {
    localStorage.removeItem(STORAGE_KEY);
    this.session.set(null);
  }

  expireSession(): void {
    this.logout();
  }

  updatePreferences(
    preferences: Pick<UserPreferences, 'theme' | 'tableDensity' | 'locale' | 'excelTableStyle'>,
  ) {
    return this.httpClient.put<UserPreferences>('/api/v1/auth/preferences', preferences).pipe(
      tap((updated) => {
        const session = this.session();
        if (!session) return;
        const next = { ...session, preferences: updated };
        localStorage.setItem(STORAGE_KEY, JSON.stringify(next));
        this.session.set(next);
      }),
    );
  }

  appSettings() {
    return this.httpClient.get<AppSettings>('/api/v1/admin/app-settings');
  }

  updateAppSettings(sessionTimeoutMinutes: number) {
    return this.httpClient.put<AppSettings>('/api/v1/admin/app-settings', {
      sessionTimeoutMinutes,
    });
  }

  hasAnyRole(roles: readonly RoleCode[]): boolean {
    const assigned = this.user()?.roles ?? [];
    return roles.some((role) => assigned.includes(role));
  }

  private readSession(): LoginResponse | null {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (!raw) return null;
      const session = JSON.parse(raw) as LoginResponse;
      if (new Date(session.expiresAt).getTime() <= Date.now()) {
        localStorage.removeItem(STORAGE_KEY);
        return null;
      }
      return {
        ...session,
        preferences: { ...DEFAULT_PREFERENCES, ...(session.preferences ?? {}) },
      };
    } catch {
      localStorage.removeItem(STORAGE_KEY);
      return null;
    }
  }
}
