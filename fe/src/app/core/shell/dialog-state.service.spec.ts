import { TestBed } from '@angular/core/testing';
import { describe, expect, it } from 'vitest';
import { DialogStateService } from './dialog-state.service';

describe('DialogStateService', () => {
  function service(): DialogStateService {
    return TestBed.inject(DialogStateService);
  }

  it('tracks nested modal depth and never goes negative', () => {
    const s = service();
    expect(s.modalOpen()).toBe(false);
    s.modalOpened();
    s.modalOpened();
    expect(s.openModalCount()).toBe(2);
    expect(s.modalOpen()).toBe(true);
    s.modalClosed();
    expect(s.modalOpen()).toBe(true);
    s.modalClosed();
    s.modalClosed(); // extra close must not underflow
    expect(s.openModalCount()).toBe(0);
    expect(s.modalOpen()).toBe(false);
  });
});
