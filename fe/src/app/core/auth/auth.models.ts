export type RoleCode = 'ADMIN' | 'HR_MANAGER' | 'HR_REVIEWER' | 'VIEWER';
export type ThemePreference = 'LIGHT' | 'DARK' | 'SYSTEM';
export type TableDensity = 'COMFORTABLE' | 'COMPACT';

export interface UserPreferences {
  theme: ThemePreference;
  tableDensity: TableDensity;
  locale: string;
  updatedAt: number | null;
}

export interface AuthUser {
  id: string;
  username: string;
  displayName: string;
  roles: RoleCode[];
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
  updatedAt: number;
}

export interface ApiProblem {
  title?: string;
  detail?: string;
  errors?: Record<string, string>;
}
