import { HttpErrorResponse } from '@angular/common/http';
import { ApiError } from './auth/auth.models';
import { I18nService } from './i18n.service';

function translateRawMsg(msg: string, i18n?: Pick<I18nService, 't'>): string {
  if (!msg) return msg;
  const lang = ((i18n as any)?.lang?.() ?? 'ar-EG') as 'ar-EG' | 'en-US';
  return msg;
}

export function apiErrorMessage(error: unknown, i18n?: Pick<I18nService, 't'>): string {
  if (error instanceof HttpErrorResponse) {
    const apiError = error.error as ApiError | null;
    if (apiError?.fieldErrors?.length) {
      const msgs = apiError.fieldErrors.map((fieldError) => translateRawMsg(fieldError.message ?? '', i18n));
      return msgs.join(' — ');
    }
    if (apiError?.localizedMessage) return translateRawMsg(apiError.localizedMessage, i18n);
    if (apiError?.message) return translateRawMsg(apiError.message, i18n);
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
      return body.fieldErrors.map((fieldError) => translateRawMsg(fieldError.message ?? '')).join(' — ');
    }
    if (body.localizedMessage) return translateRawMsg(body.localizedMessage);
    if (body.message) return translateRawMsg(body.message);
    if (body.detail) return translateRawMsg(body.detail);
  }
  return fallback;
}
