import { DOCUMENT } from '@angular/common';
import { Injectable, OnDestroy, inject } from '@angular/core';
import type { UserPreferences } from './auth/auth.models';

@Injectable({ providedIn: 'root' })
export class ThemeService implements OnDestroy {
  private readonly document = inject(DOCUMENT);
  private systemThemeQuery?: MediaQueryList;
  private systemThemeListener?: (event: MediaQueryListEvent) => void;

  apply(preferences: UserPreferences): void {
    const root = this.document.documentElement;
    const requestedTheme = String(preferences.theme ?? 'light').toLowerCase();

    root.dataset['themePreference'] = requestedTheme;

    if (requestedTheme === 'system') {
      this.bindSystemTheme(root);
      this.applyResolvedTheme(root, this.systemThemeQuery?.matches ? 'dark' : 'light');
    } else {
      this.unbindSystemTheme();
      this.applyResolvedTheme(root, requestedTheme === 'dark' ? 'dark' : 'light');
    }

    root.dataset['density'] = String(preferences.tableDensity ?? 'comfortable').toLowerCase();

    const locale = String(preferences.locale ?? 'ar');
    const isArabic = locale.toLowerCase().startsWith('ar');
    root.lang = isArabic ? 'ar' : locale;
    root.dir = isArabic ? 'rtl' : 'ltr';
  }

  ngOnDestroy(): void {
    this.unbindSystemTheme();
  }

  private bindSystemTheme(root: HTMLElement): void {
    this.unbindSystemTheme();

    const view = this.document.defaultView;
    if (!view?.matchMedia) {
      return;
    }

    this.systemThemeQuery = view.matchMedia('(prefers-color-scheme: dark)');
    this.systemThemeListener = (event: MediaQueryListEvent) => {
      if (root.dataset['themePreference'] !== 'system') {
        return;
      }
      this.applyResolvedTheme(root, event.matches ? 'dark' : 'light');
    };

    this.systemThemeQuery.addEventListener('change', this.systemThemeListener);
  }

  private unbindSystemTheme(): void {
    if (this.systemThemeQuery && this.systemThemeListener) {
      this.systemThemeQuery.removeEventListener('change', this.systemThemeListener);
    }
    this.systemThemeQuery = undefined;
    this.systemThemeListener = undefined;
  }

  private applyResolvedTheme(root: HTMLElement, theme: 'light' | 'dark'): void {
    root.dataset['theme'] = theme;
    root.dataset['resolvedTheme'] = theme;
  }
}
