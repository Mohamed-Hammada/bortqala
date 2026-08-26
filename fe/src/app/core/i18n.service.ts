import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

export type SupportedLocale = 'ar-EG' | 'en-US';
interface TranslationBundle {
  locale: SupportedLocale;
  appId: string | null;
  messages: Record<string, string>;
}

const LOCALE_STORAGE_KEY = 'bemo-erp-locale';

const REQUIRED_COPY: Record<SupportedLocale, Record<string, string>> = {
  'ar-EG': {
    'nav.settingsHint': 'إعدادات النظام والتفضيلات',
    'auth.logoutTitle': 'تسجيل الخروج',
    'auth.logoutHint': 'اختر نطاق تسجيل الخروج لهذا الحساب.',
    'auth.logoutCurrentBrowser': 'تسجيل الخروج من هذا المتصفح',
    'auth.logoutCurrentBrowserHint': 'سيتم تسجيل خروج هذا الحساب من جميع علامات التبويب في هذا المتصفح فقط.',
    'auth.logoutAllDevices': 'تسجيل الخروج من جميع الأجهزة',
    'auth.logoutAllDevicesHint': 'سيتم إلغاء جميع جلسات هذا الحساب على كل الأجهزة والمتصفحات.',
    'auth.logoutAllDevicesWorking': 'جارٍ تسجيل الخروج من جميع الأجهزة…',
    'auth.logoutAllDevicesError': 'تعذر تسجيل الخروج من جميع الأجهزة. تحقق من الاتصال وحاول مرة أخرى.',
    'review.aiHeading': 'توصيات تشغيلية مبنية على القواعد',
    'review.aiSummary': 'اقتراحات آلية مبنية على حالات الحضور الحالية: بصمة واحدة {singlePunch}، بدون بصمة {noPunch}، إدخال يدوي {manualEntry}، وجدول مفقود {missingSchedule}.',
    'review.progressLabel': 'تقدم المراجعة',
    'review.unresolved': 'غير محلولة',
    'review.allFilterLabel': 'كل السجلات',
    'operations.unit': 'رمز الوحدة',
    'operations.uom': 'اسم وحدة القياس',
    'dashboard.noReport': 'لا يوجد تقرير شهري معتمد لهذه الفترة',
    'dashboard.noReportHint': 'قد توجد تقارير نصف شهرية. افتح شاشة التقارير لمراجعة الفترات المتاحة.',
    'imports.viewFullHistory': 'عرض سجل الاستيراد بالكامل',
    'workspace.projects': 'إدارة المشاريع والإنشاءات',
    'nav.projects': 'سجل المشاريع وهيكل العمل',
    'nav.projectsHint': 'إدارة سجل المشاريع وهيكل تجزئة العمل وجداول الكميات والمقايسات WBS / BOQ',
    'common.critical': 'حرجة',
    'common.noResults': 'لا توجد نتائج',
    'common.notice': 'إشعار',
    'common.urgency': 'درجة الأهمية',
    'common.warning': 'تحذير',
    'platformAdmin.description': 'إدارة المنصة والمستأجرين',
    'nav.etaTax': 'الضرائب والفاتورة الإلكترونية (ETA)',
    'nav.etaTaxHint': 'إدارة الفواتير والإيصالات الإلكترونية والربط مع الضرائب المصرية',
    'nav.pos': 'نقطة البيع (POS)',
    'nav.posHint': 'نظام الكاشير والبيع السريع عبر الباركود وتسوية الورديات',
    'nav.crm': 'إدارة العملاء (CRM)',
    'nav.crmHint': 'متابعة مسار صفقات المبيعات والمحادثات الموحدة والروبوت الذكي',
    'auth.pushPromptTitle': 'تفعيل الإشعارات على هذا الجهاز؟',
    'auth.pushPromptHint': 'احصل على تنبيهات فورية للموافقات والرواتب والملاحظات المهمة حتى لو كان التطبيق مغلقاً.',
    'auth.pushPromptEnable': '🔔 تفعيل الإشعارات',
    'auth.pushPromptNotNow': 'ليس الآن',
    'auth.pushPromptNeverAsk': 'عدم السؤال مرة أخرى على هذا الجهاز',
    'auth.pushPromptDeniedHint': 'الإشعارات محظورة في إعدادات المتصفح لهذا الموقع. فعّلها من إعدادات الموقع ثم أعد المحاولة.',
    'dashboard.peakTitle': 'ساعات الحضور الأكثر ازدحاماً',
    'dashboard.peakHint': 'التوقيت الذي تحدث فيه معظم البصمات لكل فئة خلال الفترة المحددة.',
    'dashboard.peakHour': 'الساعة',
    'dashboard.peakEmpty': 'لا توجد بيانات بصمة في الفترة المحددة بعد.',
    'procurement.settlementDiscount': 'خصم التسوية',
    'procurement.settlementDiscountHint': 'اختياري. المبلغ النقدي مضافاً إليه هذا الخصم يجب أن يساوي أو يقل عن الرصيد المتبقي؛ يُقيَّد الخصم في دفتر الحساب ويُغلق الفاتورة.',
    'workspace.expenses': 'المصروفات',
    'nav.expenses': 'المصروفات',
    'nav.expensesHint': 'مطالبات مصروفات الموظفين والاستردادات',
    'common.created': 'تم الإنشاء',
    'common.submit': 'تقديم',
  },
  'en-US': {
    'nav.settingsHint': 'System settings and preferences',
    'auth.logoutTitle': 'Sign out',
    'auth.logoutHint': 'Choose where this account should be signed out.',
    'auth.logoutCurrentBrowser': 'Sign out from this browser',
    'auth.logoutCurrentBrowserHint': 'Signs this account out from every tab in this browser only.',
    'auth.logoutAllDevices': 'Sign out from all devices',
    'auth.logoutAllDevicesHint': 'Revokes every session for this account on all browsers and devices.',
    'auth.logoutAllDevicesWorking': 'Signing out from all devices…',
    'auth.logoutAllDevicesError': 'Could not sign out from all devices. Check the connection and try again.',
    'auth.pushPromptTitle': 'Enable notifications on this device?',
    'auth.pushPromptHint': 'Get instant alerts for approvals, payroll and important notices even when the app is closed.',
    'auth.pushPromptEnable': '🔔 Enable notifications',
    'auth.pushPromptNotNow': 'Not now',
    'auth.pushPromptNeverAsk': "Don't ask again on this device",
    'auth.pushPromptDeniedHint': 'Notifications are blocked in your browser settings for this site. Enable them in site settings and try again.',
    'dashboard.peakTitle': 'Peak clock-in hours',
    'dashboard.peakHint': 'When most punches happen for each category across the selected period.',
    'dashboard.peakHour': 'Hour',
    'dashboard.peakEmpty': 'No punch data in the selected period yet.',
    'procurement.settlementDiscount': 'Settlement discount',
    'procurement.settlementDiscountHint': 'Optional. Cash paid plus this discount must equal or stay below the outstanding balance; the discount is booked to the partner ledger and closes the invoice.',
    'review.aiHeading': 'Rule-based operational recommendations',
    'review.aiSummary': 'Deterministic suggestions based on current attendance states: single punch {singlePunch}, no punch {noPunch}, manual entry {manualEntry}, missing schedule {missingSchedule}.',
    'review.progressLabel': 'Review progress',
    'review.unresolved': 'Unresolved',
    'review.allFilterLabel': 'All rows',
    'operations.unit': 'Unit code',
    'operations.uom': 'Unit name',
    'dashboard.noReport': 'No approved monthly report for this period',
    'dashboard.noReportHint': 'Half-month reports may exist. Open Reports to review available periods.',
    'imports.viewFullHistory': 'View full import history',
    'users.manageCategories': 'Manage categories / Add a category',
    'workspace.projects': 'Projects & Construction',
    'nav.projects': 'Projects & WBS Register',
    'nav.projectsHint': 'Manage project register, work breakdown structures, and BOQ items',
    'common.critical': 'Critical',
    'common.noResults': 'No results found',
    'common.notice': 'Notice',
    'common.urgency': 'Urgency',
    'common.warning': 'Warning',
    'platformAdmin.description': 'Platform & Tenant Administration',
    'nav.etaTax': 'Egypt E-Invoicing (ETA)',
    'nav.etaTaxHint': 'Egyptian Tax Authority e-invoicing and e-receipt compliance',
    'nav.pos': 'Point of Sale (POS)',
    'nav.posHint': 'Point of sale cashier retail engine and shift reconciliation',
    'nav.crm': 'CRM & Omnichannel',
    'nav.crmHint': 'CRM sales pipeline, leads and WhatsApp/FB omnichannel chatbot',
    'workspace.expenses': 'Expenses',
    'nav.expenses': 'Expenses',
    'nav.expensesHint': 'Employee expense claims and reimbursements',
    'common.created': 'Created',
    'common.submit': 'Submit',
  },
};

@Injectable({ providedIn: 'root' })
export class I18nService {
  private readonly httpClient = inject(HttpClient);
  private readonly cache = new Map<string, Record<string, string>>();
  private activeScope: string | null = null;
  readonly locale = signal<SupportedLocale>(this.storedLocale());
  readonly messages = signal<Record<string, string>>({});

  t(key: string, params?: Record<string, string | number>, fallback?: string): string {
    const locale = this.locale();
    const message = this.messages()[key] ?? REQUIRED_COPY[locale][key] ?? fallback ?? key;
    if (!params) return message;
    return Object.entries(params).reduce(
      (resolved, [name, value]) => resolved.replaceAll(`{${name}}`, String(value)),
      message,
    );
  }

  async use(locale: string, appId: string | null = null): Promise<void> {
    const supported: SupportedLocale = locale.toLowerCase().startsWith('en') ? 'en-US' : 'ar-EG';
    const scopeKey = this.scopeKey(supported, appId);
    this.activeScope = scopeKey;
    this.locale.set(supported);
    localStorage.setItem(LOCALE_STORAGE_KEY, supported);

    if (typeof document !== 'undefined') {
      document.documentElement.setAttribute('lang', supported.startsWith('en') ? 'en' : 'ar');
      document.documentElement.setAttribute('dir', supported.startsWith('en') ? 'ltr' : 'rtl');
    }

    const cached = this.cache.get(scopeKey);
    if (cached) {
      this.messages.set(cached);
      return;
    }
    try {
      const bundle = await firstValueFrom(this.httpClient.get<TranslationBundle>(`/api/v1/i18n/${supported}`));
      const responseScopeKey = this.scopeKey(supported, bundle.appId);
      this.cache.set(responseScopeKey, bundle.messages);
      if (bundle.appId === appId && this.activeScope === scopeKey) this.messages.set(bundle.messages);
    } catch {
      if (this.activeScope === scopeKey) this.messages.set({});
    }
  }

  invalidate(locale?: string, appId?: string | null): void {
    if (locale) {
      const supported: SupportedLocale = locale.toLowerCase().startsWith('en') ? 'en-US' : 'ar-EG';
      this.cache.delete(this.scopeKey(supported, appId ?? null));
      return;
    }
    this.cache.clear();
  }

  private scopeKey(locale: SupportedLocale, appId: string | null): string {
    return `${appId ?? 'DEFAULT'}:${locale}`;
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
