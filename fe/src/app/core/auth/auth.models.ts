export type RoleCode =
  | 'SUPER_ADMIN'
  | 'ADMIN'
  | 'HR_MANAGER'
  | 'HR_REVIEWER'
  | 'VIEWER'
  | 'FINANCE_MANAGER'
  | 'ACCOUNTANT'
  | 'TREASURY_USER'
  | 'PROCUREMENT_MANAGER'
  | 'PROCUREMENT_USER'
  | 'SALES_MANAGER'
  | 'INVENTORY_MANAGER'
  | 'MANUFACTURING_MANAGER'
  | 'QUALITY_MANAGER'
  | 'PAYROLL_MANAGER'
  | 'WORKFORCE_MANAGER'
  | 'WORKFORCE_REVIEWER'
  | 'WORKFORCE_FINANCE'
  | 'AUDITOR';
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
  activeFeatures?: string[];
}

export interface MeSessionInfo {
  expiresAt: number;
  timeoutMinutes: number;
  timeoutEnabled: boolean;
}

export interface MeResponse {
  id: string;
  username: string;
  displayName: string;
  tenant: { id: string; code: string; name: string };
  roles: RoleCode[];
  scopes: string[];
  canViewSalary: boolean;
  categoryId?: string | null;
  dashboardCustomizationEnabled: boolean;
  active: boolean;
  session: MeSessionInfo;
  version: number;
  activeFeatures?: string[];
}

export interface LoginResponse {
  accessToken: string;
  tokenType: 'Bearer';
  expiresAt: number;
  mustChangePassword: boolean;
  app: { id: string; code: string; name: string; adminDashboardCustomizationEnabled?: boolean };
  user: AuthUser;
  preferences: UserPreferences;
}

export interface RefreshResponse {
  accessToken: string;
  tokenType: 'Bearer';
  expiresAt: number;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

export interface AppSettings {
  sessionTimeoutMinutes: number;
  sessionTimeoutEnabled: boolean;
  showReportPresets: boolean;
  attendanceAnomalyThresholdPercent: number;
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

export interface ApiError {
  code?: string;
  message?: string;
  localizedMessage?: string;
  status?: number;
  path?: string;
  correlationId?: string;
  timestamp?: string;
  detail?: string;
  fieldErrors?: ApiFieldError[];
}

export interface ApiFieldError {
  field: string;
  code?: string;
  message?: string;
}
