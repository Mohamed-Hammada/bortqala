import { CanDeactivateFn } from '@angular/router';

export interface ComponentWithUnsavedChanges {
  hasUnsavedChanges(): boolean;
}

export const unsavedChangesGuard: CanDeactivateFn<ComponentWithUnsavedChanges> = (component) => {
  if (component.hasUnsavedChanges && component.hasUnsavedChanges()) {
    return confirm('⚠️ تتبيه: لديك تغييرات غير محفوظة! هل أنت تأكد من رغبتك في الخروج دون حفظ البيانات؟');
  }
  return true;
};
