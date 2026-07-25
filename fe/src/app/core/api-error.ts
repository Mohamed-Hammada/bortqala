import { HttpErrorResponse } from '@angular/common/http';
import { ApiProblem } from './auth/auth.models';
import { I18nService } from './i18n.service';

export function apiErrorMessage(error: unknown, i18n?: Pick<I18nService, 't'>): string {
  if (error instanceof HttpErrorResponse) {
    const problem = error.error as ApiProblem | null;
    if (problem?.errors) return Object.values(problem.errors).join(' — ');
    if (problem?.detail) return problem.detail;
    if (error.status === 0) return i18n?.t('api.connectionError') ?? 'Unable to reach the server.';
    if (error.status === 401) return i18n?.t('api.unauthorized') ?? 'Authentication failed.';
  }
  return i18n?.t('api.unexpected') ?? 'An unexpected error occurred.';
}
