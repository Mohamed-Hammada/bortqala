import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { apiErrorMessage } from '../../core/api-error';
import { I18nService } from '../../core/i18n.service';
import { AuthUser, RoleCode } from '../../core/auth/auth.models';
import { UserPolicyAssignmentItem } from '../../core/auth/security-policy.models';

export interface UserCategory {
  id: string;
  code: string;
  name: string;
  scope: 'EMPLOYEE' | 'WORKER' | 'BOTH';
}

export interface UserPayload {
  username: string;
  displayName: string;
  password: string | null;
  roles: RoleCode[];
  allowedMenus?: string[];
  canViewSalary?: boolean;
  dashboardCustomizationEnabled?: boolean;
  active: boolean;
  version: number | null;
  categoryId?: string | null;
  accessChangeReason?: string;
  policyAssignments?: UserPolicyAssignmentItem[];
}

@Injectable()
export class UsersStore {
  private readonly i18n = inject(I18nService);
  private readonly http = inject(HttpClient);
  readonly items = signal<AuthUser[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly categories = signal<UserCategory[]>([]);
  readonly categoriesLoading = signal(false);

  async load() {
    this.loading.set(true);
    try {
      this.items.set(await firstValueFrom(this.http.get<AuthUser[]>('/api/v1/users')));
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    } finally {
      this.loading.set(false);
    }
  }

  async loadCategories() {
    this.categoriesLoading.set(true);
    try {
      this.categories.set(await firstValueFrom(this.http.get<UserCategory[]>('/api/v1/auth/user-categories')));
    } catch {
      this.categories.set([]);
    } finally {
      this.categoriesLoading.set(false);
    }
  }

  async save(id: string | null, payload: UserPayload) {
    this.loading.set(true);
    this.error.set(null);
    try {
      await firstValueFrom(
        id
          ? this.http.put<AuthUser>(`/api/v1/users/${id}`, payload)
          : this.http.post<AuthUser>('/api/v1/users', payload),
      );
      await this.load();
      return true;
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
      return false;
    } finally {
      this.loading.set(false);
    }
  }
}
