import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

export type SupportedLocale = 'ar-EG' | 'en-US';
interface TranslationBundle {
  locale: SupportedLocale;
  messages: Record<string, string>;
}

const LOCALE_STORAGE_KEY = 'bemo-erp-locale';

@Injectable({ providedIn: 'root' })
export class I18nService {
  private readonly httpClient = inject(HttpClient);
  private readonly cache = new Map<SupportedLocale, Record<string, string>>();
  readonly locale = signal<SupportedLocale>(this.storedLocale());
  readonly messages = signal<Record<string, string>>({});

  t(key: string, params?: Record<string, string | number>, fallback?: string): string {
    const locale = this.locale();
    const message =
      this.messages()[key] ?? fallback ?? key;
    if (!params) return message;
    return Object.entries(params).reduce(
      (resolved, [name, value]) => resolved.replaceAll(`{${name}}`, String(value)),
      message,
    );
  }

  async use(locale: string): Promise<void> {
    const supported: SupportedLocale = locale.toLowerCase().startsWith('en') ? 'en-US' : 'ar-EG';
    this.locale.set(supported);
    localStorage.setItem(LOCALE_STORAGE_KEY, supported);

    if (typeof document !== 'undefined') {
      document.documentElement.setAttribute('lang', supported.startsWith('en') ? 'en' : 'ar');
      document.documentElement.setAttribute('dir', supported.startsWith('en') ? 'ltr' : 'rtl');
    }

    const cached = this.cache.get(supported);
    if (cached) {
      this.messages.set(cached);
      return;
    }
    try {
      const bundle = await firstValueFrom(
        this.httpClient.get<TranslationBundle>(`/api/v1/i18n/${supported}`),
      );
      this.cache.set(supported, bundle.messages);
      if (this.locale() === supported) this.messages.set(bundle.messages);
    } catch {
      this.messages.set({});
    }
  }

  private storedLocale(): SupportedLocale {
    const loc = localStorage.getItem(LOCALE_STORAGE_KEY) === 'en-US' ? 'en-US' : 'ar-EG';
    if (typeof document !== 'undefined') {
      document.documentElement.setAttribute('lang', loc.startsWith('en') ? 'en' : 'ar');
      document.documentElement.setAttribute('dir', loc.startsWith('en') ? 'ltr' : 'rtl');
    }
    return loc;
  }
}
