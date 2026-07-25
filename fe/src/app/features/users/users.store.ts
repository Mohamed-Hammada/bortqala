import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { apiErrorMessage } from '../../core/api-error';
import { I18nService } from '../../core/i18n.service';
import { AuthUser, RoleCode } from '../../core/auth/auth.models';
export interface UserPayload {
  username: string;
  displayName: string;
  password: string | null;
  roles: RoleCode[];
  active: boolean;
  version: number | null;
}
@Injectable()
export class UsersStore {
  private readonly i18n = inject(I18nService);
  private readonly http = inject(HttpClient);
  readonly items = signal<AuthUser[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
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
