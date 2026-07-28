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
  updatedAt: number | null;
}

export interface AuthUser {
  id: string;
  username: string;
  displayName: string;
  roles: RoleCode[];
  allowedMenus?: string[];
  canViewSalary?: boolean;
  active: boolean;
  version: number;
}

export interface LoginResponse {
  accessToken: string;
  tokenType: 'Bearer';
  expiresAt: number;
  app: { id: string; code: string; name: string };
  user: AuthUser;
  preferences: UserPreferences;
}

export interface AppSettings {
  sessionTimeoutMinutes: number;
  sessionTimeoutEnabled: boolean;
  showReportPresets: boolean;
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
