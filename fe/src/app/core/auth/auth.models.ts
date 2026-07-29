export type RoleCode = 'SUPER_ADMIN' | 'ADMIN' | 'HR_MANAGER' | 'HR_REVIEWER' | 'VIEWER';
export type ThemePreference = 'LIGHT' | 'DARK' | 'SYSTEM';
export type TableDensity = 'COMFORTABLE' | 'COMPACT' | 'DENSE';
export type ExcelTableStyle = 'GOLD' | 'BLUE' | 'GREEN' | 'GRAY';

export interface UserPreferences {
  theme: ThemePreference;
  tableDensity: TableDensity;
  locale: string;
  excelTableStyle: ExcelTableStyle;
  defaultPageSize?: number;
  defaultPage?: string;
  showFavorites: boolean;
  showRecentlyUsed: boolean;
  maxRecentlyUsed: number;
  favoriteMenuIds: string[];
  recentMenuIds: string[];
  dashboardWidgetIds: DashboardWidgetId[];
  dashboardAnimationsEnabled: boolean;
  dashboardLayoutCustomizationAllowed?: boolean;
  updatedAt: number | null;
}

export type DashboardWidgetId =
  | 'summary'
  | 'report'
  | 'attendance-chart'
  | 'insights'
  | 'units'
  | 'departments'
  | 'categories'
  | 'imports';

export interface DashboardPreferences {
  widgetIds: DashboardWidgetId[];
  animationsEnabled: boolean;
}

export interface NavigationPreferences {
  showFavorites: boolean;
  showRecentlyUsed: boolean;
  maxRecentlyUsed: number;
  favoriteMenuIds: string[];
  recentMenuIds: string[];
}

export interface NotificationPreferences {
  emailApprovals: boolean;
  emailPayroll: boolean;
  pushApprovals: boolean;
  pushPayroll: boolean;
}

export interface AuthUser {
  id: string;
  username: string;
  displayName: string;
  roles: RoleCode[];
  allowedMenus?: string[];
  canViewSalary?: boolean;
  categoryId?: string | null;
  dashboardCustomizationEnabled?: boolean;
  active: boolean;
  version: number;
}

export interface LoginResponse {
  accessToken: string;
  tokenType: 'Bearer';
  expiresAt: number;
  app: { id: string; code: string; name: string; adminDashboardCustomizationEnabled?: boolean };
  user: AuthUser;
  preferences: UserPreferences;
}

export interface AppSettings {
  sessionTimeoutMinutes: number;
  sessionTimeoutEnabled: boolean;
  showReportPresets: boolean;
  automaticProcurementNumbering: boolean;
  adminDashboardCustomizationEnabled: boolean;
  minPasswordLength?: number;
  requireUppercase?: boolean;
  requireLowercase?: boolean;
  requireNumbers?: boolean;
  requireSpecialChars?: boolean;
  disallowSpaces?: boolean;
  maxPasswordLength?: number;
  passwordExpiryDays?: number;
  passwordHistoryCount?: number;
  updatedAt: number;
}

export interface ApiProblem {
  title?: string;
  detail?: string;
  errors?: Record<string, string>;
}
