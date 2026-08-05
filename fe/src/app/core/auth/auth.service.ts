import { HttpClient } from '@angular/common/http';
import { Injectable, computed, effect, inject, signal } from '@angular/core';
import { Observable, catchError, tap, throwError } from 'rxjs';
import { ThemeService } from '../theme.service';
import { I18nService } from '../i18n.service';
import {
  AppSettings,
  AuthUser,
  ChangePasswordRequest,
  DashboardPreferences,
  LoginResponse,
  MeResponse,
  NavigationPreferences,
  RefreshResponse,
  RoleCode,
  UserPreferences,
} from './auth.models';

const STORAGE_KEY = 'bemo-erp-session';
const DEFAULT_PREFERENCES: UserPreferences = {
  theme: 'SYSTEM',
  tableDensity: 'COMFORTABLE',
  locale: 'ar-EG',
  excelTableStyle: 'GOLD',
  defaultPage: '/dashboard',
  showFavorites: true,
  showRecentlyUsed: true,
  maxRecentlyUsed: 4,
  favoriteMenuIds: [],
  recentMenuIds: [],
  dashboardWidgetIds: ['summary', 'report', 'attendance-chart', 'insights', 'units', 'departments', 'categories', 'imports'],
  dashboardAnimationsEnabled: true,
  dashboardLayoutCustomizationAllowed: true,
  updatedAt: null,
};

interface StoredSession {
  expiresAt: number;
  mustChangePassword: boolean;
  app: LoginResponse['app'];
  user: AuthUser;
  preferences: UserPreferences;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly httpClient = inject(HttpClient);
  private readonly themeService = inject(ThemeService);
  private readonly i18nService = inject(I18nService);
  private readonly session = signal<LoginResponse | null>(this.readStoredSession());
  private refreshPromise: Promise<boolean> | null = null;

  readonly user = computed(() => this.session()?.user ?? null);
  readonly app = computed(() => this.session()?.app ?? null);
  readonly token = computed(() => this.session()?.accessToken ?? null);
  readonly preferences = computed(() => this.session()?.preferences ?? DEFAULT_PREFERENCES);
  readonly mustChangePassword = computed(() => this.session()?.mustChangePassword ?? false);
  readonly authenticated = computed(() => {
    const session = this.session();
    return !!session && !!session.accessToken && new Date(session.expiresAt).getTime() > Date.now();
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
      .post<LoginResponse>('/api/v1/auth/login', { appCode, username, password }, { withCredentials: true })
      .pipe(
        tap((session) => {
          this.session.set(session);
          this.persistStoredSession(session);
        }),
      );
  }

  demoLogin(secret: string) {
    return this.httpClient
      .post<LoginResponse>('/api/v1/auth/demo-login', { secret }, { withCredentials: true })
      .pipe(
        tap((session) => {
          this.session.set(session);
          this.persistStoredSession(session);
        }),
      );
  }

  tryRefresh(): Promise<boolean> {
    if (!this.sessionRestorable()) return Promise.resolve(false);
    if (this.refreshPromise) return this.refreshPromise;
    this.refreshPromise = this.doRefresh().finally(() => {
      this.refreshPromise = null;
    });
    return this.refreshPromise;
  }

  private doRefresh(): Promise<boolean> {
    return new Promise((resolve) => {
      this.httpClient.post<RefreshResponse>('/api/v1/auth/refresh', {}, { withCredentials: true }).pipe(
        tap((refreshed) => {
          const session = this.session();
          if (session) {
            const next = { ...session, accessToken: refreshed.accessToken, expiresAt: new Date(refreshed.expiresAt).getTime() };
            this.session.set(next);
            this.persistStoredSession(next);
          }
        }),
      ).subscribe({
        next: () => resolve(true),
        error: () => resolve(false),
      });
    });
  }

  logout(): void {
    this.httpClient.post('/api/v1/auth/logout', {}, { withCredentials: true }).subscribe({
      error: () => undefined,
    });
    this.clearSession();
  }

  expireSession(): void {
    this.clearSession();
  }

  sessionRestorable(): boolean {
    return localStorage.getItem(STORAGE_KEY) !== null;
  }

  changePassword(currentPassword: string, newPassword: string): Observable<void> {
    const payload: ChangePasswordRequest = { currentPassword, newPassword };
    return this.httpClient
      .post<void>('/api/v1/auth/change-password', payload, { withCredentials: true })
      .pipe(
        tap(() => this.clearSession()),
        catchError((error) => throwError(() => error)),
      );
  }

  updatePreferences(
    preferences: Pick<UserPreferences, 'theme' | 'tableDensity' | 'locale' | 'excelTableStyle' | 'defaultPage'>,
  ) {
    return this.httpClient.put<UserPreferences>('/api/v1/auth/preferences', preferences).pipe(
      tap((updated) => this.replacePreferences(updated)),
    );
  }

  updateNavigationPreferences(preferences: NavigationPreferences) {
    return this.httpClient.put<UserPreferences>('/api/v1/auth/preferences/navigation', preferences).pipe(
      tap((updated) => this.replacePreferences(updated)),
    );
  }

  updateDashboardPreferences(preferences: DashboardPreferences) {
    return this.httpClient.put<UserPreferences>('/api/v1/auth/preferences/dashboard', preferences).pipe(
      tap((updated) => this.replacePreferences(updated)),
    );
  }

  refreshPreferences() {
    return this.httpClient.get<UserPreferences>('/api/v1/auth/preferences').pipe(
      tap((updated) => this.replacePreferences(updated)),
    );
  }

  appSettings() {
    return this.httpClient.get<AppSettings>('/api/v1/admin/app-settings');
  }

  fetchMe() {
    return this.httpClient.get<MeResponse>('/api/v1/users/me');
  }

  updateAppSettings(payload: Omit<AppSettings, 'updatedAt'>) {
    return this.httpClient.put<AppSettings>('/api/v1/admin/app-settings', payload).pipe(
      tap((settings) => this.replaceDashboardPolicy(settings.adminDashboardCustomizationEnabled)),
    );
  }

  readonly canCustomizeDashboard = computed(() => {
    const effectivePreference = this.preferences().dashboardLayoutCustomizationAllowed;
    if (effectivePreference !== undefined) return effectivePreference;
    const user = this.user();
    if (!user) return false;
    if (user.roles.includes('SUPER_ADMIN')) return true;
    if (user.dashboardCustomizationEnabled === false) return false;
    if (user.roles.includes('ADMIN')) return this.app()?.adminDashboardCustomizationEnabled !== false;
    return true;
  });

  readonly canViewSalary = computed(() => {
    const u = this.user();
    if (!u) return false;
    if (u.roles.includes('SUPER_ADMIN')) return true;
    return u.canViewSalary ?? true;
  });

  isSuperAdmin(): boolean {
    return this.user()?.roles.includes('SUPER_ADMIN') ?? false;
  }

  hasAnyRole(roles: readonly RoleCode[]): boolean {
    const assigned = this.user()?.roles ?? [];
    if (assigned.includes('SUPER_ADMIN') || assigned.includes('ADMIN')) return true;
    return roles.some((role) => assigned.includes(role));
  }

  hasMenuAccess(menuId: string): boolean {
    const user = this.user();
    if (!user) return false;

    // Feature toggles
    const activeFeatures = user.activeFeatures ?? [];
    if (menuId === 'payroll' && !activeFeatures.includes('payroll.enabled')) return false;
    if (menuId === 'sales' && !activeFeatures.includes('sales.enabled')) return false;
    if (menuId === 'production' && !activeFeatures.includes('manufacturing.enabled')) return false;
    if (menuId === 'quality' && !activeFeatures.includes('quality.enabled')) return false;
    
    if (!activeFeatures.includes('finance.enabled') && (menuId === 'accounts' || menuId === 'journal-entries' || menuId === 'banks' || menuId === 'tax-currency' || menuId === 'fiscal-periods')) return false;
    if (!activeFeatures.includes('workforce.contractorAccounts.enabled') && (menuId === 'workforce-accounts' || menuId === 'workforce-settlements')) return false;

    if (user.roles.includes('SUPER_ADMIN') || user.roles.includes('ADMIN')) return true;
    if (!user.allowedMenus || user.allowedMenus.length === 0) return true;
    return user.allowedMenus.includes(menuId);
  }

  private clearSession(): void {
    localStorage.removeItem(STORAGE_KEY);
    this.session.set(null);
  }

  private persistStoredSession(session: LoginResponse): void {
    const stored: StoredSession = {
      expiresAt: session.expiresAt,
      mustChangePassword: session.mustChangePassword,
      app: session.app,
      user: session.user,
      preferences: session.preferences,
    };
    localStorage.setItem(STORAGE_KEY, JSON.stringify(stored));
  }

  private readStoredSession(): LoginResponse | null {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (!raw) return null;
      const stored = JSON.parse(raw) as StoredSession;
      return {
        accessToken: '',
        tokenType: 'Bearer',
        expiresAt: stored.expiresAt,
        mustChangePassword: stored.mustChangePassword,
        app: stored.app,
        user: stored.user,
        preferences: { ...DEFAULT_PREFERENCES, ...(stored.preferences ?? {}) },
      };
    } catch {
      localStorage.removeItem(STORAGE_KEY);
      return null;
    }
  }

  private replacePreferences(updated: UserPreferences): void {
    const session = this.session();
    if (!session) return;
    const next = { ...session, preferences: { ...DEFAULT_PREFERENCES, ...updated } };
    this.session.set(next);
    this.persistStoredSession(next);
  }

  private replaceDashboardPolicy(enabled: boolean): void {
    const session = this.session();
    if (!session) return;
    const next = { ...session, app: { ...session.app, adminDashboardCustomizationEnabled: enabled } };
    this.session.set(next);
    this.persistStoredSession(next);
  }
}
