import { ComponentFixture, TestBed } from '@angular/core/testing';
import { describe, expect, beforeEach, it } from 'vitest';
import { I18nService } from '../../../core/i18n.service';
import { DialogStateService } from '../../../core/shell/dialog-state.service';
import { ModalDialogComponent } from './modal-dialog.component';

describe('ModalDialogComponent × DialogStateService (WP-13)', () => {
  let state: DialogStateService;

  function createOpen(): { fixture: ComponentFixture<ModalDialogComponent>; closed: number[] } {
    const fixture = TestBed.createComponent(ModalDialogComponent);
    const closed: number[] = [];
    fixture.componentInstance.close.subscribe(() => closed.push(1));
    fixture.componentRef.setInput('isOpen', true);
    fixture.detectChanges();
    return { fixture, closed };
  }

  beforeEach(() => {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [
        DialogStateService,
        { provide: I18nService, useValue: { locale: () => 'ar-EG', t: (_k: string, _p?: unknown, f?: string) => f ?? _k } },
      ],
    });
    state = TestBed.inject(DialogStateService);
  });

  it('registers depth while open and releases on close', () => {
    const { fixture } = createOpen();
    expect(state.openModalCount()).toBe(1);
    expect(state.modalOpen()).toBe(true);

    fixture.componentRef.setInput('isOpen', false);
    fixture.detectChanges();
    expect(state.openModalCount()).toBe(0);
    expect(state.modalOpen()).toBe(false);
  });

  it('Escape closes the dialog via the close output', () => {
    const { closed } = createOpen();
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }));
    expect(closed.length).toBe(1);
  });

  it('a background (non-topmost) dialog ignores Escape while a second stays open', () => {
    const background = createOpen();
    const foreground = createOpen();

    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }));

    expect(background.closed.length).toBe(0); // non-topmost ignored it
    expect(foreground.closed.length).toBe(1); // topmost consumed it
    expect(state.openModalCount()).toBe(2); // parents keep both mounted
  });

  it('ngOnDestroy releases its depth slot', () => {
    const { fixture } = createOpen();
    fixture.destroy();
    expect(state.modalOpen()).toBe(false);
  });

  it('restores focus to the trigger element when closed', async () => {
    const trigger = document.createElement('button');
    trigger.id = 'test-trigger';
    document.body.appendChild(trigger);
    trigger.focus();
    expect(document.activeElement).toBe(trigger);

    const { fixture } = createOpen();
    expect(state.modalOpen()).toBe(true);

    fixture.componentRef.setInput('isOpen', false);
    fixture.detectChanges();

    await Promise.resolve();
    await Promise.resolve();
    expect(document.activeElement).toBe(trigger);
    trigger.remove();
  });

  it('ignores Escape when preventEscapeClose is true', () => {
    const fixture = TestBed.createComponent(ModalDialogComponent);
    const closed: number[] = [];
    fixture.componentInstance.close.subscribe(() => closed.push(1));
    fixture.componentRef.setInput('isOpen', true);
    fixture.componentRef.setInput('preventEscapeClose', true);
    fixture.detectChanges();

    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }));
    expect(closed.length).toBe(0);
    fixture.destroy();
  });
});
