export type SettingsTab =
  | 'appearance'
  | 'reports'
  | 'shortcuts'
  | 'session'
  | 'security'
  | 'business';

export interface SettingsSubmenuItem {
  tab: SettingsTab;
  labelKey: string;
  fallbackAr: string;
  adminOnly?: boolean;
}

export interface SettingsSubmenuGroup {
  labelKey: string;
  fallbackAr: string;
  items: readonly SettingsSubmenuItem[];
  adminOnly?: boolean;
}

export const SETTINGS_SUBMENU_GROUPS: readonly SettingsSubmenuGroup[] = [
  {
    labelKey: 'settings.groupPersonal',
    fallbackAr: 'الإعدادات الشخصية',
    items: [
      { tab: 'appearance', labelKey: 'settings.tabAppearance', fallbackAr: 'المظهر واللغة' },
      { tab: 'reports', labelKey: 'settings.tabReports', fallbackAr: 'التقارير والتصدير' },
      { tab: 'shortcuts', labelKey: 'settings.tabShortcuts', fallbackAr: 'الاختصارات' },
    ],
  },
  {
    labelKey: 'settings.groupSecurityAdmin',
    fallbackAr: 'الأمان وإعدادات النظام',
    adminOnly: true,
    items: [
      { tab: 'session', labelKey: 'settings.tabSession', fallbackAr: 'الجلسة', adminOnly: true },
      { tab: 'security', labelKey: 'settings.tabSecurity', fallbackAr: 'الأمان', adminOnly: true },
      {
        tab: 'business',
        labelKey: 'settings.tabBusinessConfiguration',
        fallbackAr: 'إعدادات الأعمال',
        adminOnly: true,
      },
    ],
  },
] as const;

/** Backward-compatible redirects for links/bookmarks created before the IA split. */
export const MOVED_SETTINGS_TAB_ROUTES: Readonly<Record<string, string>> = {
  risk: '/partner-risk',
  onboarding: '/admin/setup-readiness',
  analytics: '/admin/product-insights',
  subscription: '/platform-admin?tab=subscription',
  trial: '/platform-admin?tab=trial',
  industry: '/platform-admin?tab=industry',
  entitlements: '/platform-admin?tab=entitlements',
  translations: '/platform-admin?tab=translations',
};

export function isSettingsTab(value: string | null): value is SettingsTab {
  return value === 'appearance'
    || value === 'reports'
    || value === 'shortcuts'
    || value === 'session'
    || value === 'security'
    || value === 'business';
}
