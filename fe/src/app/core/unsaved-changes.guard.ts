import { inject } from '@angular/core';
import { CanDeactivateFn } from '@angular/router';
import { ConfirmDialogService } from './confirm-dialog.service';

export interface ComponentWithUnsavedChanges {
  hasUnsavedChanges(): boolean;
}

export const unsavedChangesGuard: CanDeactivateFn<ComponentWithUnsavedChanges> = async (component) => {
  if (component.hasUnsavedChanges && component.hasUnsavedChanges()) {
    const confirmService = inject(ConfirmDialogService);
    return confirmService.confirmOptions({
      titleKey: 'common.unsavedTitle',
      messageKey: 'common.unsavedMessage',
      confirmKey: 'common.discard',
      danger: true,
    });
  }
  return true;
};
