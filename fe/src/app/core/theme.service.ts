import { DOCUMENT } from '@angular/common';
import { Injectable, inject } from '@angular/core';
import type { UserPreferences } from './auth/auth.models';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly document = inject(DOCUMENT);

  apply(preferences: UserPreferences): void {
    const root = this.document.documentElement;
    root.dataset['theme'] = preferences.theme.toLowerCase();
    root.dataset['density'] = preferences.tableDensity.toLowerCase();
    root.lang = preferences.locale.toLowerCase().startsWith('ar') ? 'ar' : preferences.locale;
    root.dir = root.lang === 'ar' ? 'rtl' : 'ltr';
  }
}
