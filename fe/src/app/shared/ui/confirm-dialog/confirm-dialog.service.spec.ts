import { TestBed } from '@angular/core/testing';
import { ConfirmDialogService } from '../../../core/confirm-dialog.service';

describe('ConfirmDialogService', () => {
  let service: ConfirmDialogService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ConfirmDialogService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('closes with true when the dialog is confirmed without an action', async () => {
    const promise = service.confirmOptions({ messageKey: 'common.unsavedMessage' });
    service.proceed();
    await expect(promise).resolves.toBe(true);
    expect(service.confirmState()).toBeNull();
  });

  it('resolves false when the dialog is cancelled', async () => {
    const promise = service.confirmOptions({ messageKey: 'common.unsavedMessage' });
    service.cancel();
    await expect(promise).resolves.toBe(false);
    expect(service.confirmState()).toBeNull();
  });

  it('runs the action after confirm and closes the dialog', async () => {
    let ran = false;
    const promise = service.confirmAndRun(
      { messageKey: 'common.unsavedMessage' },
      async () => {
        ran = true;
      },
    );
    service.proceed();
    await promise;
    expect(ran).toBe(true);
    expect(service.confirmState()).toBeNull();
  });

  it('keeps the dialog open and exposes the error when the action fails', async () => {
    service.confirmAndRun(
      { messageKey: 'common.unsavedMessage' },
      async () => {
        throw new Error('boom');
      },
    );
    service.proceed();
    await new Promise((resolve) => setTimeout(resolve, 0));
    expect(service.confirmState()).not.toBeNull();
    expect(service.confirmState()?.error).toContain('unexpected');
  });

  it('blocks cancellation while the action is running', () => {
    let resolveAction!: () => void;
    const promise = service.confirmAndRun(
      { messageKey: 'common.unsavedMessage' },
      () => new Promise<void>((resolve) => (resolveAction = resolve)),
    );
    service.proceed();
    service.cancel();
    expect(service.confirmState()).not.toBeNull();
    resolveAction();
    void promise.then(() => {
      expect(service.confirmState()).toBeNull();
    });
  });
});
