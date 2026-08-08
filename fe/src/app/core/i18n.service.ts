import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

export type SupportedLocale = 'ar-EG' | 'en-US';
interface TranslationBundle {
  locale: SupportedLocale;
  messages: Record<string, string>;
}

const LOCALE_STORAGE_KEY = 'bemo-erp-locale';

const REQUIRED_COPY: Record<SupportedLocale, Record<string, string>> = {
  'ar-EG': {
    'nav.settingsHint': 'إعدادات النظام والتفضيلات',
    'review.aiHeading': 'توصيات تشغيلية مبنية على القواعد',
    'review.aiSummary': 'اقتراحات آلية مبنية على حالات الحضور الحالية: بصمة واحدة {singlePunch}، بدون بصمة {noPunch}، إدخال يدوي {manualEntry}، وجدول مفقود {missingSchedule}.',
    'review.progressLabel': 'تقدم المراجعة',
    'review.unresolved': 'غير محلولة',
    'review.allFilterLabel': 'كل السجلات',
    'operations.unit': 'رمز الوحدة',
    'operations.uom': 'اسم وحدة القياس',
    'dashboard.noReport': 'لا يوجد تقرير شهري معتمد لهذه الفترة',
    'dashboard.noReportHint': 'قد توجد تقارير نصف شهرية. افتح شاشة التقارير لمراجعة الفترات المتاحة.',
  },
  'en-US': {
    'nav.settingsHint': 'System settings and preferences',
    'review.aiHeading': 'Rule-based operational recommendations',
    'review.aiSummary': 'Deterministic suggestions based on current attendance states: single punch {singlePunch}, no punch {noPunch}, manual entry {manualEntry}, missing schedule {missingSchedule}.',
    'review.progressLabel': 'Review progress',
    'review.unresolved': 'Unresolved',
    'review.allFilterLabel': 'All rows',
    'operations.unit': 'Unit code',
    'operations.uom': 'Unit name',
    'dashboard.noReport': 'No approved monthly report for this period',
    'dashboard.noReportHint': 'Half-month reports may exist. Open Reports to review available periods.',
  },
};

@Injectable({ providedIn: 'root' })
export class I18nService {
  private readonly httpClient = inject(HttpClient);
  private readonly cache = new Map<SupportedLocale, Record<string, string>>();
  readonly locale = signal<SupportedLocale>(this.storedLocale());
  readonly messages = signal<Record<string, string>>({});

  t(key: string, params?: Record<string, string | number>, fallback?: string): string {
    const locale = this.locale();
    const message = REQUIRED_COPY[locale][key] ?? this.messages()[key] ?? fallback ?? key;
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
      const bundle = await firstValueFrom(this.httpClient.get<TranslationBundle>(`/api/v1/i18n/${supported}`));
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
