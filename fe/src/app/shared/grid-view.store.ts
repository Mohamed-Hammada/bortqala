import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { apiErrorMessage } from '../core/api-error';
import { I18nService } from '../core/i18n.service';
import { GridView, GridViewSaveRequest } from '../features/settings/integrations.models';

@Injectable({ providedIn: 'root' })
export class GridViewStore {
  private readonly http = inject(HttpClient);
  private readonly i18n = inject(I18nService);
  readonly views = signal<GridView[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  async load(pageKey: string) {
    this.loading.set(true);
    this.error.set(null);
    try {
      const result = await firstValueFrom(
        this.http.get<{ views: GridView[] }>(`/api/v1/platform/grid-views?pageKey=${encodeURIComponent(pageKey)}`),
      );
      this.views.set(result.views);
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    } finally {
      this.loading.set(false);
    }
  }

  async save(pageKey: string, request: GridViewSaveRequest): Promise<GridView | null> {
    try {
      const result = await firstValueFrom(
        this.http.post<GridView>('/api/v1/platform/grid-views', { ...request, pageKey }),
      );
      await this.load(pageKey);
      return result;
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
      return null;
    }
  }

  async remove(viewId: string, pageKey: string) {
    try {
      await firstValueFrom(this.http.delete(`/api/v1/platform/grid-views/${viewId}`));
      await this.load(pageKey);
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    }
  }
}
