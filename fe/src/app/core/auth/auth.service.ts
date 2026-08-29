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
const LOGOUT_EVENT_KEY = 'bemo-erp-logout-event';

type LogoutScope = 'CURRENT_BROWSER' | 'ALL_DEVICES';

interface LogoutBroadcast {
  userId: string;
  scope: LogoutScope;
  occurredAt: number;
  eventId: string;
}

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
      const appId = this.token() ? this.app()?.id ?? null : null;
      void this.i18nService.use(preferences.locale, appId);
    });

    if (typeof window !== 'undefined') {
      window.addEventListener('storage', this.handleStorageEvent);
    }
  }

  login(appCode: string, username: string, password: string) {
    return this.httpClient
      .post<LoginResponse>('/api/v1/auth/login', { appCode, username, password }, { withCredentials: true })
      .pipe(tap((session) => { this.session.set(session); this.persistStoredSession(session); }));
  }

  demoLogin(secret: string) {
    return this.httpClient
      .post<LoginResponse>('/api/v1/auth/demo-login', { secret }, { withCredentials: true })
      .pipe(tap((session) => { this.session.set(session); this.persistStoredSession(session); }));
  }

  tryRefresh(): Promise<boolean> {
    if (!this.sessionRestorable()) return Promise.resolve(false);
    if (this.refreshPromise) return this.refreshPromise;
    this.refreshPromise = this.doRefresh().finally(() => { this.refreshPromise = null; });
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
      ).subscribe({ next: () => resolve(true), error: () => resolve(false) });
    });
  }

  /**
   * Backwards-compatible default: logging out means logging this account out
   * from every tab in the current browser, but not from other devices.
   */
  logout(): void {
    this.logoutCurrentBrowser();
  }

  logoutCurrentBrowser(): void {
    const userId = this.user()?.id;
    this.httpClient.post('/api/v1/auth/logout', {}, { withCredentials: true }).subscribe({ error: () => undefined });
    this.completeLocalLogout(userId, 'CURRENT_BROWSER');
  }

  logoutAllDevices(): Observable<void> {
    const userId = this.user()?.id;
    return this.httpClient
      .post<void>('/api/v1/auth/sessions/revoke-all', {}, { withCredentials: true })
      .pipe(
        tap(() => this.completeLocalLogout(userId, 'ALL_DEVICES')),
        catchError((error) => throwError(() => error)),
      );
  }

  expireSession(): void { this.clearSession(); }
  sessionRestorable(): boolean { return localStorage.getItem(STORAGE_KEY) !== null; }

  changePassword(currentPassword: string, newPassword: string): Observable<void> {
    const payload: ChangePasswordRequest = { currentPassword, newPassword };
    return this.httpClient.post<void>('/api/v1/auth/change-password', payload, { withCredentials: true })
      .pipe(tap(() => this.clearSession()), catchError((error) => throwError(() => error)));
  }

  updatePreferences(preferences: Pick<UserPreferences, 'theme' | 'tableDensity' | 'locale' | 'excelTableStyle' | 'defaultPage'>) {
    return this.httpClient.put<UserPreferences>('/api/v1/auth/preferences', preferences)
      .pipe(tap((updated) => this.replacePreferences(updated)));
  }
  updateNavigationPreferences(preferences: NavigationPreferences) {
    return this.httpClient.put<UserPreferences>('/api/v1/auth/preferences/navigation', preferences)
      .pipe(tap((updated) => this.replacePreferences(updated)));
  }
  updateDashboardPreferences(preferences: DashboardPreferences) {
    return this.httpClient.put<UserPreferences>('/api/v1/auth/preferences/dashboard', preferences)
      .pipe(tap((updated) => this.replacePreferences(updated)));
  }
  refreshPreferences() {
    return this.httpClient.get<UserPreferences>('/api/v1/auth/preferences')
      .pipe(tap((updated) => this.replacePreferences(updated)));
  }
  appSettings() { return this.httpClient.get<AppSettings>('/api/v1/admin/app-settings'); }
  fetchMe() { return this.httpClient.get<MeResponse>('/api/v1/users/me'); }
  updateAppSettings(payload: Omit<AppSettings, 'updatedAt'>) {
    return this.httpClient.put<AppSettings>('/api/v1/admin/app-settings', payload)
      .pipe(tap((settings) => this.replaceDashboardPolicy(settings.adminDashboardCustomizationEnabled)));
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

  readonly permissions = signal<Set<string>>(new Set());
  readonly branchScopes = signal<Set<string>>(new Set());
  readonly costCenterScopes = signal<Set<string>>(new Set());

  loadMyPermissions(): void {
    if (!this.authenticated()) {
      this.permissions.set(new Set());
      this.branchScopes.set(new Set());
      this.costCenterScopes.set(new Set());
      return;
    }
    this.httpClient.get<{ permissions: string[]; branchScopes: string[]; costCenterScopes: string[] }>('/api/v1/access/me/permissions')
      .subscribe({
        next: (res) => {
          this.permissions.set(new Set(res.permissions || []));
          this.branchScopes.set(new Set(res.branchScopes || []));
          this.costCenterScopes.set(new Set(res.costCenterScopes || []));
        },
        error: () => {
          // If fails or offline, fallback to empty or admin bypass
        },
      });
  }

  hasPermission(permission: string): boolean {
    if (!permission) return true;
    if (this.isSuperAdmin() || this.hasAnyRole(['ADMIN'])) return true;
    const current = this.permissions();
    return current.has('*') || current.has(permission.trim());
  }

  hasAnyPermission(permissions: string[]): boolean {
    if (!permissions || permissions.length === 0) return true;
    if (this.isSuperAdmin() || this.hasAnyRole(['ADMIN'])) return true;
    const current = this.permissions();
    if (current.has('*')) return true;
    return permissions.some((p) => current.has(p.trim()));
  }

  hasBranchAccess(branchId: string): boolean {
    if (!branchId) return true;
    if (this.isSuperAdmin() || this.hasAnyRole(['ADMIN'])) return true;
    const scopes = this.branchScopes();
    if (scopes.size === 0 || scopes.has('*')) return true;
    return scopes.has(branchId.trim());
  }

  hasCostCenterAccess(costCenterId: string): boolean {
    if (!costCenterId) return true;
    if (this.isSuperAdmin() || this.hasAnyRole(['ADMIN'])) return true;
    const scopes = this.costCenterScopes();
    if (scopes.size === 0 || scopes.has('*')) return true;
    return scopes.has(costCenterId.trim());
  }

  isSuperAdmin(): boolean { return this.user()?.roles.includes('SUPER_ADMIN') ?? false; }

  hasAnyRole(roles: readonly RoleCode[]): boolean {
    const assigned = this.user()?.roles ?? [];
    if (assigned.includes('SUPER_ADMIN') || assigned.includes('ADMIN')) return true;
    return roles.some((role) => assigned.includes(role));
  }

  hasMenuAccess(menuId: string): boolean {
    const user = this.user();
    if (!user) return false;

    // SUPER_ADMIN is the unrestricted system owner: it can see every implemented
    // module regardless of tenant feature flags or per-user menu assignment.
    if (user.roles.includes('SUPER_ADMIN')) return true;

    // All other roles, including ADMIN, respect tenant feature availability.
    const activeFeatures = user.activeFeatures ?? [];
    if (menuId === 'payroll' && !activeFeatures.includes('payroll.enabled')) return false;
    if ((menuId === 'sales' || menuId === 'pos' || menuId === 'crm') && !activeFeatures.includes('sales.enabled')) return false;
    if (menuId === 'production' && !activeFeatures.includes('manufacturing.enabled')) return false;
    if (menuId === 'quality' && !activeFeatures.includes('quality.enabled')) return false;
    if (menuId === 'procurement'
        && !activeFeatures.includes('procurement.enabled')
        && !activeFeatures.includes('purchasing.enabled')) return false;
    if (menuId === 'export-shipments' && !activeFeatures.includes('agri.enabled')) return false;
    if (!activeFeatures.includes('finance.enabled')
        && (menuId === 'accounts' || menuId === 'journal-entries' || menuId === 'banks'
          || menuId === 'tax-currency' || menuId === 'fiscal-periods' || menuId === 'budgets'
          || menuId === 'fixed-assets' || menuId === 'payment-links' || menuId === 'eta-tax')) return false;
    if (!activeFeatures.includes('workforce.contractorAccounts.enabled')
        && (menuId === 'workforce-accounts' || menuId === 'workforce-settlements' || menuId === 'workforce-client-billing')) return false;
    if ((menuId === 'clinic-patients' || menuId === 'clinic-queue' || menuId === 'clinic-commissions' || menuId === 'clinic-appointments' || menuId === 'clinic-pharmacy' || menuId === 'clinic-lab' || menuId === 'clinic-insurance' || menuId === 'hospital-ops' || menuId === 'dental-charting')
        && !activeFeatures.includes('medical.enabled')) return false;

    // ADMIN bypasses per-user menu assignment only after feature availability checks.
    if (user.roles.includes('ADMIN')) return true;
    if (user.menuAccessMode === 'ALL') return true;
    return user.allowedMenus?.includes(menuId) ?? false;
  }

  private readonly handleStorageEvent = (event: StorageEvent): void => {
    if (event.key !== LOGOUT_EVENT_KEY || !event.newValue) return;

    try {
      const logout = JSON.parse(event.newValue) as Partial<LogoutBroadcast>;
      const currentUserId = this.user()?.id;
      if (!currentUserId || logout.userId !== currentUserId) return;

      // A logout emitted by another tab must affect only tabs for the same user.
      this.clearSession();
    } catch {
      // Ignore malformed/legacy localStorage values.
    }
  };

  private completeLocalLogout(userId: string | undefined, scope: LogoutScope): void {
    if (userId) this.broadcastLogout(userId, scope);
    this.clearSession();
  }

  private broadcastLogout(userId: string, scope: LogoutScope): void {
    const eventId =
      typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function'
        ? crypto.randomUUID()
        : `${Date.now()}-${Math.random().toString(36).slice(2)}`;
    const logout: LogoutBroadcast = {
      userId,
      scope,
      occurredAt: Date.now(),
      eventId,
    };
    localStorage.setItem(LOGOUT_EVENT_KEY, JSON.stringify(logout));
  }

  private clearSession(): void {
    localStorage.removeItem(STORAGE_KEY);
    this.i18nService.invalidate();
    this.session.set(null);
  }

  private persistStoredSession(session: LoginResponse): void {
    const stored: StoredSession = {
      expiresAt: session.expiresAt, mustChangePassword: session.mustChangePassword,
      app: session.app, user: session.user, preferences: session.preferences,
    };
    localStorage.setItem(STORAGE_KEY, JSON.stringify(stored));
  }

  private readStoredSession(): LoginResponse | null {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (!raw) return null;
      const stored = JSON.parse(raw) as StoredSession;
      return {
        accessToken: '', tokenType: 'Bearer', expiresAt: stored.expiresAt,
        mustChangePassword: stored.mustChangePassword, app: stored.app, user: stored.user,
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
