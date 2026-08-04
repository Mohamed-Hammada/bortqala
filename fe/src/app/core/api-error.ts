import { HttpErrorResponse } from '@angular/common/http';
import { ApiError } from './auth/auth.models';
import { I18nService } from './i18n.service';

function byCode(code: string | undefined, fallback: string, i18n?: Pick<I18nService, 't'>): string {
  if (!code || !i18n) return fallback;
  const translated = i18n.t(code);
  return translated === code ? fallback : translated;
}

export function apiErrorMessage(error: unknown, i18n?: Pick<I18nService, 't'>): string {
  if (error instanceof HttpErrorResponse) {
    const apiError = error.error as ApiError | null;
    if (apiError?.fieldErrors?.length) {
      const msgs = apiError.fieldErrors.map((fieldError) =>
        byCode(fieldError.code, fieldError.message ?? '', i18n),
      );
      return msgs.join(' — ');
    }
    if (apiError?.code) {
      return byCode(apiError.code, apiError.localizedMessage ?? apiError.message ?? '', i18n);
    }
    if (apiError?.localizedMessage) return apiError.localizedMessage;
    if (apiError?.message) return apiError.message;
    if (error.status === 0) return i18n?.t('api.connectionError') ?? 'Unable to reach the server.';
    if (error.status === 401) return i18n?.t('api.unauthorized') ?? 'Authentication failed.';
  }
  return i18n?.t('api.unexpected') ?? 'An unexpected error occurred.';
}

export function apiErrorDetail(error: unknown, fallback: string): string {
  const body: ApiError | undefined =
    error instanceof HttpErrorResponse ? (error.error as ApiError) : (error as { error?: ApiError } | null)?.error;
  if (body) {
    if (body.fieldErrors?.length) {
      return body.fieldErrors.map((fieldError) => fieldError.message ?? '').join(' — ');
    }
    if (body.localizedMessage) return body.localizedMessage;
    if (body.message) return body.message;
    if (body.detail) return body.detail;
  }
  return fallback;
}
