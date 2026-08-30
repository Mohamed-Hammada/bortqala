import { describe, expect, it, beforeEach, afterEach } from 'vitest';
import { getFocusableElements, trapFocusWithin } from './focus-trap.util';

function buildDialog(): { container: HTMLElement; first: HTMLElement; middle: HTMLElement; last: HTMLElement } {
  const container = document.createElement('div');
  const first = document.createElement('button');
  first.textContent = 'First';
  const middle = document.createElement('input');
  const last = document.createElement('a');
  last.href = '#';
  last.textContent = 'Last';
  const disabled = document.createElement('button');
  disabled.disabled = true;
  const hiddenWrap = document.createElement('div');
  hiddenWrap.style.display = 'none';
  const hiddenBtn = document.createElement('button');
  hiddenWrap.appendChild(hiddenBtn);
  container.append(first, middle, last, disabled, hiddenWrap);
  document.body.appendChild(container);
  return { container, first, middle, last };
}

describe('focus-trap.util (WP-13 BUG-3/BUG-8)', () => {
  let built: ReturnType<typeof buildDialog>;

  beforeEach(() => {
    built = buildDialog();
  });

  afterEach(() => {
    built.container.remove();
  });

  it('collects only visible, enabled focusable elements', () => {
    expect(getFocusableElements(built.container).map((el) => el.textContent)).toEqual([
      'First',
      '',
      'Last',
    ]);
  });

  it('wraps forward Tab from the last element back to the first', () => {
    built.last.focus();
    const event = new KeyboardEvent('keydown', { key: 'Tab', cancelable: true });
    expect(trapFocusWithin(built.container, event)).toBe(true);
    expect(event.defaultPrevented).toBe(true);
    expect(document.activeElement).toBe(built.first);
  });

  it('wraps backward Shift+Tab from the first element to the last', () => {
    built.first.focus();
    const event = new KeyboardEvent('keydown', { key: 'Tab', shiftKey: true, cancelable: true });
    expect(trapFocusWithin(built.container, event)).toBe(true);
    expect(document.activeElement).toBe(built.last);
  });

  it('focuses the container itself when nothing focusable exists', () => {
    const empty = document.createElement('div');
    empty.tabIndex = -1;
    document.body.appendChild(empty);
    const event = new KeyboardEvent('keydown', { key: 'Tab', cancelable: true });
    try {
      expect(trapFocusWithin(empty, event)).toBe(true);
      expect(document.activeElement).toBe(empty);
    } finally {
      empty.remove();
    }
  });

  it('leaves interior navigation untouched', () => {
    built.first.focus();
    const event = new KeyboardEvent('keydown', { key: 'Tab', cancelable: true });
    expect(trapFocusWithin(built.container, event)).toBe(false);
    expect(event.defaultPrevented).toBe(false);
  });
});
