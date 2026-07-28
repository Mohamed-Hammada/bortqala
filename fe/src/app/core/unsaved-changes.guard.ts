import { inject } from '@angular/core';
import { CanDeactivateFn } from '@angular/router';
import { ConfirmDialogService } from './confirm-dialog.service';

export interface ComponentWithUnsavedChanges {
  hasUnsavedChanges(): boolean;
}

export const unsavedChangesGuard: CanDeactivateFn<ComponentWithUnsavedChanges> = async (component) => {
  if (component.hasUnsavedChanges && component.hasUnsavedChanges()) {
    const confirmService = inject(ConfirmDialogService);
    return confirmService.confirm('⚠️ تنبيه: لديك تغييرات غير محفوظة! هل أنت متأكد من رغبتك في الخروج دون حفظ البيانات؟');
  }
  return true;
};
