#!/usr/bin/env python3
"""
Apply the Bemo ERP logout-scope fix to repository branch fm_bemo_consolidated.

Run from the repository root:
    python apply_logout_fix.py --check
    python apply_logout_fix.py

The script validates all expected source fragments before writing any file.
It is intentionally scoped to the six files required for the logout behavior.
"""

from __future__ import annotations

import argparse
from pathlib import Path
import sys

BASE_COMMIT = "da9374aaf5a94a09c5ef3d97a2f03241218fbf8e"

Patch = tuple[str, str]

PATCHES: dict[str, list[Patch]] = {
    "fe/src/app/core/auth/auth.service.ts": [
        (
            """const STORAGE_KEY = 'bemo-erp-session';
const DEFAULT_PREFERENCES: UserPreferences = {""",
            """const STORAGE_KEY = 'bemo-erp-session';
const LOGOUT_EVENT_KEY = 'bemo-erp-logout-event';

type LogoutScope = 'CURRENT_BROWSER' | 'ALL_DEVICES';

interface LogoutBroadcast {
  userId: string;
  scope: LogoutScope;
  occurredAt: number;
  eventId: string;
}

const DEFAULT_PREFERENCES: UserPreferences = {""",
        ),
        (
            """  constructor() {
    effect(() => {
      const preferences = this.preferences();
      this.themeService.apply(preferences);
      void this.i18nService.use(preferences.locale);
    });
  }""",
            """  constructor() {
    effect(() => {
      const preferences = this.preferences();
      this.themeService.apply(preferences);
      void this.i18nService.use(preferences.locale);
    });

    if (typeof window !== 'undefined') {
      window.addEventListener('storage', this.handleStorageEvent);
    }
  }""",
        ),
        (
            """  logout(): void {
    this.httpClient.post('/api/v1/auth/logout', {}, { withCredentials: true }).subscribe({ error: () => undefined });
    this.clearSession();
  }
  expireSession(): void { this.clearSession(); }""",
            """  /**
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

  expireSession(): void { this.clearSession(); }""",
        ),
        (
            """  private clearSession(): void { localStorage.removeItem(STORAGE_KEY); this.session.set(null); }""",
            """  private readonly handleStorageEvent = (event: StorageEvent): void => {
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

  private clearSession(): void { localStorage.removeItem(STORAGE_KEY); this.session.set(null); }""",
        ),
    ],
    "fe/src/app/core/shell/app-shell.component.ts": [
        (
            """  readonly selectedQuickNavIndex = signal(0);
  readonly chordWaiting = signal(false);
  readonly globalShortcuts = GLOBAL_SHORTCUTS;""",
            """  readonly selectedQuickNavIndex = signal(0);
  readonly chordWaiting = signal(false);
  readonly logoutOptionsOpen = signal(false);
  readonly logoutAllDevicesBusy = signal(false);
  readonly logoutError = signal('');
  readonly globalShortcuts = GLOBAL_SHORTCUTS;""",
        ),
        (
            """    effect(() => {
      const preferences = this.authService.preferences();
      this.favorites.set([...preferences.favoriteMenuIds]);
      this.recentIds.set([...preferences.recentMenuIds]);
    }, { allowSignalWrites: true });
    this.router.events""",
            """    effect(() => {
      const preferences = this.authService.preferences();
      this.favorites.set([...preferences.favoriteMenuIds]);
      this.recentIds.set([...preferences.recentMenuIds]);
    }, { allowSignalWrites: true });
    effect(() => {
      if (this.authService.user() === null) {
        queueMicrotask(() => void this.router.navigate(['/login']));
      }
    });
    this.router.events""",
        ),
        (
            """    if (
      event.key === 'Escape'
      && (this.quickNavOpen() || this.shortcutHelpOpen() || this.chordWaiting())
    ) {""",
            """    if (event.key === 'Escape' && this.logoutOptionsOpen()) {
      event.preventDefault();
      this.closeLogoutOptions();
      return;
    }

    if (
      event.key === 'Escape'
      && (this.quickNavOpen() || this.shortcutHelpOpen() || this.chordWaiting())
    ) {""",
        ),
        (
            """      || this.quickNavOpen()
      || this.shortcutHelpOpen()
    ) return;""",
            """      || this.quickNavOpen()
      || this.shortcutHelpOpen()
      || this.logoutOptionsOpen()
    ) return;""",
        ),
        (
            """  logout(): void {
    this.authService.logout();
    void this.router.navigate(['/login']);
  }""",
            """  logout(): void {
    this.logoutError.set('');
    this.logoutOptionsOpen.set(true);
  }

  closeLogoutOptions(): void {
    if (this.logoutAllDevicesBusy()) return;
    this.logoutOptionsOpen.set(false);
    this.logoutError.set('');
  }

  logoutCurrentBrowser(): void {
    if (this.logoutAllDevicesBusy()) return;
    this.logoutOptionsOpen.set(false);
    this.authService.logoutCurrentBrowser();
    void this.router.navigate(['/login']);
  }

  logoutAllDevices(): void {
    if (this.logoutAllDevicesBusy()) return;
    this.logoutAllDevicesBusy.set(true);
    this.logoutError.set('');

    this.authService.logoutAllDevices().subscribe({
      next: () => {
        this.logoutAllDevicesBusy.set(false);
        this.logoutOptionsOpen.set(false);
        void this.router.navigate(['/login']);
      },
      error: () => {
        this.logoutAllDevicesBusy.set(false);
        this.logoutError.set(this.i18n.t('auth.logoutAllDevicesError'));
      },
    });
  }""",
        ),
    ],
    "fe/src/app/core/shell/app-shell.component.html": [
        (
            """</div>

<!-- Action Center Notifications Dropdown Panel -->""",
            """</div>

@if (logoutOptionsOpen()) {
  <div class="shortcut-overlay" (click)="closeLogoutOptions()">
    <section
      class="shortcut-dialog"
      role="dialog"
      aria-modal="true"
      [attr.aria-label]="i18n.t('auth.logoutTitle')"
      [attr.aria-busy]="logoutAllDevicesBusy()"
      (click)="$event.stopPropagation()"
      style="max-width: 520px; width: min(520px, calc(100vw - 2rem));"
    >
      <header class="shortcut-dialog-header">
        <div>
          <h2>{{ i18n.t('auth.logoutTitle') }}</h2>
          <p>{{ i18n.t('auth.logoutHint') }}</p>
        </div>
        <button
          type="button"
          class="shortcut-close"
          (click)="closeLogoutOptions()"
          [disabled]="logoutAllDevicesBusy()"
          [attr.aria-label]="i18n.t('action.cancel')"
        >✕</button>
      </header>

      <div style="display: grid; gap: 0.75rem;">
        <button
          type="button"
          class="button secondary"
          (click)="logoutCurrentBrowser()"
          [disabled]="logoutAllDevicesBusy()"
          style="display: grid; gap: 0.25rem; text-align: start; min-height: 64px;"
        >
          <strong>{{ i18n.t('auth.logoutCurrentBrowser') }}</strong>
          <span>{{ i18n.t('auth.logoutCurrentBrowserHint') }}</span>
        </button>

        <button
          type="button"
          class="button"
          (click)="logoutAllDevices()"
          [disabled]="logoutAllDevicesBusy()"
          style="display: grid; gap: 0.25rem; text-align: start; min-height: 64px;"
        >
          <strong>
            {{
              logoutAllDevicesBusy()
                ? i18n.t('auth.logoutAllDevicesWorking')
                : i18n.t('auth.logoutAllDevices')
            }}
          </strong>
          <span>{{ i18n.t('auth.logoutAllDevicesHint') }}</span>
        </button>
      </div>

      @if (logoutError()) {
        <p role="alert" style="margin: 0.75rem 0 0; color: var(--danger, #b91c1c);">
          {{ logoutError() }}
        </p>
      }
    </section>
  </div>
}

<!-- Action Center Notifications Dropdown Panel -->""",
        ),
    ],
    "fe/src/app/core/i18n.service.ts": [
        (
            """    'nav.settingsHint': 'إعدادات النظام والتفضيلات',""",
            """    'nav.settingsHint': 'إعدادات النظام والتفضيلات',
    'auth.logoutTitle': 'تسجيل الخروج',
    'auth.logoutHint': 'اختر نطاق تسجيل الخروج لهذا الحساب.',
    'auth.logoutCurrentBrowser': 'تسجيل الخروج من هذا المتصفح',
    'auth.logoutCurrentBrowserHint': 'سيتم تسجيل خروج هذا الحساب من جميع علامات التبويب في هذا المتصفح فقط.',
    'auth.logoutAllDevices': 'تسجيل الخروج من جميع الأجهزة',
    'auth.logoutAllDevicesHint': 'سيتم إلغاء جميع جلسات هذا الحساب على كل الأجهزة والمتصفحات.',
    'auth.logoutAllDevicesWorking': 'جارٍ تسجيل الخروج من جميع الأجهزة…',
    'auth.logoutAllDevicesError': 'تعذر تسجيل الخروج من جميع الأجهزة. تحقق من الاتصال وحاول مرة أخرى.',""",
        ),
        (
            """    'nav.settingsHint': 'System settings and preferences',""",
            """    'nav.settingsHint': 'System settings and preferences',
    'auth.logoutTitle': 'Sign out',
    'auth.logoutHint': 'Choose where this account should be signed out.',
    'auth.logoutCurrentBrowser': 'Sign out from this browser',
    'auth.logoutCurrentBrowserHint': 'Signs this account out from every tab in this browser only.',
    'auth.logoutAllDevices': 'Sign out from all devices',
    'auth.logoutAllDevicesHint': 'Revokes every session for this account on all browsers and devices.',
    'auth.logoutAllDevicesWorking': 'Signing out from all devices…',
    'auth.logoutAllDevicesError': 'Could not sign out from all devices. Check the connection and try again.',""",
        ),
    ],
    "be/src/main/java/com/bemo/hr/shared/security/AuthController.java": [
        (
            """    @PostMapping("/auth/logout")
    ResponseEntity<Void> logout(HttpServletResponse servletResponse,
                                @CookieValue(name = "${hr.security.refresh-cookie-name:bemo_refresh}", required = false) String refreshCookie) {
        try {
            if (refreshCookie != null && !refreshCookie.isBlank()) {
                authService.logout(refreshCookie);
            }
        } finally {
            clearRefreshCookie(servletResponse);
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/auth/change-password")""",
            """    @PostMapping("/auth/logout")
    ResponseEntity<Void> logout(HttpServletResponse servletResponse,
                                @CookieValue(name = "${hr.security.refresh-cookie-name:bemo_refresh}", required = false) String refreshCookie) {
        try {
            if (refreshCookie != null && !refreshCookie.isBlank()) {
                authService.logout(refreshCookie);
            }
        } finally {
            clearRefreshCookie(servletResponse);
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/auth/sessions/revoke-all")
    @PreAuthorize("isAuthenticated()")
    ResponseEntity<Void> logoutAllDevices(HttpServletResponse servletResponse,
                                          Authentication authentication) {
        try {
            authService.revokeOwnSessions(authentication.getName());
        } finally {
            clearRefreshCookie(servletResponse);
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/auth/change-password")""",
        ),
    ],
    "be/src/main/java/com/bemo/hr/shared/security/AuthService.java": [
        (
            """    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void logout(String cookieValue) {
        if (cookieValue == null || cookieValue.isBlank()) return;
        RefreshCookieCodec.Decoded decoded = refreshCookieCodec.decode(cookieValue);
        TenantContext.set(decoded.appId());
        try {
            refreshTokenService.revoke(decoded.appId(), decoded.rawToken(),
                    refreshTokenService.usernameFor(decoded.appId(), decoded.rawToken()));
        } finally {
            TenantContext.clear();
        }
    }

    @Transactional
    public void changePassword(String username, AuthApi.ChangePasswordRequest request) {""",
            """    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void logout(String cookieValue) {
        if (cookieValue == null || cookieValue.isBlank()) return;
        RefreshCookieCodec.Decoded decoded = refreshCookieCodec.decode(cookieValue);
        TenantContext.set(decoded.appId());
        try {
            refreshTokenService.revoke(decoded.appId(), decoded.rawToken(),
                    refreshTokenService.usernameFor(decoded.appId(), decoded.rawToken()));
        } finally {
            TenantContext.clear();
        }
    }

    @Transactional
    public void revokeOwnSessions(String username) {
        String appId = TenantContext.require();
        var user = requireByUsername(appId, username);
        user.bumpTokenVersion();
        refreshTokenService.revokeAllForUser(appId, user.getId(), username);
        auditService.record("SESSIONS_REVOKED_SELF", "USER", user.getId(), username,
                "User signed out from all devices", null);
    }

    @Transactional
    public void changePassword(String username, AuthApi.ChangePasswordRequest request) {""",
        ),
    ],
}


def transform_file(path: Path, replacements: list[Patch]) -> tuple[str, list[str]]:
    original = path.read_text(encoding="utf-8")
    updated = original
    states: list[str] = []

    for index, (old, new) in enumerate(replacements, start=1):
        if old in updated:
            if updated.count(old) != 1:
                raise RuntimeError(
                    f"{path}: replacement #{index} matched {updated.count(old)} times; expected exactly once."
                )
            updated = updated.replace(old, new, 1)
            states.append("applicable")
        elif new in updated:
            states.append("already-applied")
        else:
            raise RuntimeError(
                f"{path}: replacement #{index} did not match. "
                "The branch may have changed since the patch was prepared."
            )

    return updated, states


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Apply current-browser/all-devices logout behavior to Bemo ERP."
    )
    parser.add_argument(
        "--check",
        action="store_true",
        help="Validate that the patch applies cleanly without writing files.",
    )
    parser.add_argument(
        "--root",
        default=".",
        help="Repository root (default: current directory).",
    )
    args = parser.parse_args()

    root = Path(args.root).resolve()
    planned: dict[Path, str] = {}
    status_rows: list[tuple[str, list[str]]] = []

    try:
        for relative, replacements in PATCHES.items():
            path = root / relative
            if not path.exists():
                raise RuntimeError(f"Missing expected file: {relative}")
            updated, states = transform_file(path, replacements)
            planned[path] = updated
            status_rows.append((relative, states))
    except RuntimeError as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        return 2

    for relative, states in status_rows:
        print(f"{relative}: {', '.join(states)}")

    if args.check:
        print(
            f"\nPatch check passed. Prepared against fm_bemo_consolidated base commit {BASE_COMMIT}."
        )
        return 0

    changed = 0
    for path, updated in planned.items():
        current = path.read_text(encoding="utf-8")
        if current != updated:
            path.write_text(updated, encoding="utf-8")
            changed += 1

    print(f"\nApplied logout-session fix. Files changed: {changed}.")
    if changed == 0:
        print("The fix was already applied.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
