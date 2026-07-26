import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

export type SupportedLocale = 'ar-EG' | 'en-US';
interface TranslationBundle {
  locale: SupportedLocale;
  messages: Record<string, string>;
}

const LOCALE_STORAGE_KEY = 'hr-platform-locale';

const DEFAULT_FALLBACKS: Record<SupportedLocale, Record<string, string>> = {
  'ar-EG': {
    'api.connectionError': 'تعذر الاتصال بالخادم. تأكد أن الـ backend يعمل.',
    'api.unauthorized': 'انتهت الجلسة أو بيانات الدخول غير صحيحة.',
    'api.unexpected': 'حدث خطأ غير متوقع. حاول مرة أخرى.',
    'login.invalidCredentials': 'اسم المستخدم أو كلمة المرور غير صحيحة.',
    'login.sessionExpired': 'انتهت الجلسة. برجاء تسجيل الدخول مرة أخرى.',
    'settings.dense': 'عالية الكثافة',
    'settings.sessionMinutesHint': '💡 ملاحظة: تنطبق المدة الجديدة بالدقائق للمستخدمين بعد تسجيل الخروج وإعادة تسجيل الدخول مرة أخرى.',
    'warning.manualConfirmationRequired': 'مطلوب تأكيد الحضور يدويًا.',
    'warning.noBiometricPunch': 'لم يتم تسجيل أي بصمات.',
    'warning.singlePunchPolicy': 'تم احتساب الحضور من بصمة واحدة حسب سياسة الفئة.',
    'warning.singlePunchIncomplete': 'تم تسجيل بصمة واحدة فقط وتتطلب المراجعة.',
    'warning.missingScheduleRule': 'لا توجد مواعيد عمل سارية لهذا اليوم.',
  },
  'en-US': {
    'api.connectionError': 'Unable to reach the server. Make sure the backend is running.',
    'api.unauthorized': 'The session expired or the credentials are invalid.',
    'api.unexpected': 'An unexpected error occurred. Please try again.',
    'login.invalidCredentials': 'The username or password is incorrect.',
    'login.sessionExpired': 'Session expired. Please log in again.',
    'settings.dense': 'High Density',
    'settings.sessionMinutesHint': '💡 Note: The updated session duration in minutes will take effect after logging out and logging in again.',
    'warning.manualConfirmationRequired': 'Manual attendance confirmation is required.',
    'warning.noBiometricPunch': 'No biometric punch found.',
    'warning.singlePunchPolicy': 'Presence counted from one punch by category policy.',
    'warning.singlePunchIncomplete': 'One punch is incomplete and requires review.',
    'warning.missingScheduleRule': 'No effective schedule rule for this workday.',
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
    const message =
      this.messages()[key] ?? DEFAULT_FALLBACKS[locale]?.[key] ?? fallback ?? key;
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
    return localStorage.getItem(LOCALE_STORAGE_KEY) === 'en-US' ? 'en-US' : 'ar-EG';
  }
}
