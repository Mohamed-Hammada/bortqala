import { HttpErrorResponse } from '@angular/common/http';
import { ApiProblem } from './auth/auth.models';

export function apiErrorMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    const problem = error.error as ApiProblem | null;
    if (problem?.errors) return Object.values(problem.errors).join(' — ');
    if (problem?.detail) return problem.detail;
    if (error.status === 0) return 'تعذر الاتصال بالخادم. تأكد أن الـ backend يعمل.';
    if (error.status === 401) return 'اسم المستخدم أو كلمة المرور غير صحيحة.';
  }
  return 'حدث خطأ غير متوقع. حاول مرة أخرى.';
}
