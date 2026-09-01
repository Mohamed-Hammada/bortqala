import { describe, expect, it } from 'vitest';
import { resolveShortcutAction, type ShortcutGate } from './shortcut-guard.util';

function gate(overrides: Partial<ShortcutGate> = {}): ShortcutGate {
  return {
    typing: false,
    ctrl: false,
    meta: false,
    alt: false,
    code: '',
    quickNavOpen: false,
    shortcutHelpOpen: false,
    logoutOptionsOpen: false,
    modalOpen: false,
    chordWaiting: false,
    ...overrides,
  };
}

describe('resolveShortcutAction', () => {
  it('opens command palette via Ctrl+K / Cmd+K and quick-nav via Ctrl+/ or plain /', () => {
    expect(resolveShortcutAction('k', 'k', gate({ ctrl: true, code: 'KeyK' }))).toBe('OPEN_PALETTE');
    expect(resolveShortcutAction('k', 'k', gate({ meta: true, code: 'KeyK' }))).toBe('OPEN_PALETTE');
    expect(resolveShortcutAction('k', 'k', gate({ ctrl: true, typing: true }))).toBe('IGNORE');
    expect(resolveShortcutAction('/', '/', gate({ ctrl: true }))).toBe('OPEN_QUICK_NAV');
    expect(resolveShortcutAction('/', '/', gate())).toBe('OPEN_QUICK_NAV');
  });

  it('suppresses every shortcut while a page modal is open (BUG-1/BUG-2/SHORTCUT-001)', () => {
    const blocked = gate({ modalOpen: true });
    expect(resolveShortcutAction('k', 'k', { ...blocked, ctrl: true })).toBe('IGNORE');
    expect(resolveShortcutAction('k', 'k', { ...blocked, meta: true })).toBe('IGNORE');
    expect(resolveShortcutAction('/', '/', blocked)).toBe('IGNORE');
    expect(resolveShortcutAction('?', '?', blocked)).toBe('IGNORE');
    expect(resolveShortcutAction('g', 'g', { ...blocked, code: 'KeyG' })).toBe('IGNORE');
  });

  it('lets the page modal own Escape exclusively (BUG-5)', () => {
    const withModal = gate({ modalOpen: true, paletteOpen: true, quickNavOpen: true, chordWaiting: true });
    expect(resolveShortcutAction('Escape', 'escape', withModal)).toBe('IGNORE');
  });

  it('Escape closes shell panels / clears chord only when no modal is open', () => {
    expect(resolveShortcutAction('Escape', 'escape', gate({ paletteOpen: true }))).toBe('ESCAPE_SHELL_PANEL');
    expect(resolveShortcutAction('Escape', 'escape', gate({ quickNavOpen: true }))).toBe('ESCAPE_SHELL_PANEL');
    expect(resolveShortcutAction('Escape', 'escape', gate({ shortcutHelpOpen: true }))).toBe('ESCAPE_SHELL_PANEL');
    expect(resolveShortcutAction('Escape', 'escape', gate({ logoutOptionsOpen: true }))).toBe('ESCAPE_SHELL_PANEL');
    expect(resolveShortcutAction('Escape', 'escape', gate({ chordWaiting: true }))).toBe('ESCAPE_SHELL_PANEL');
    expect(resolveShortcutAction('Escape', 'escape', gate())).toBe('IGNORE');
  });

  it('? opens help only outside typing and without modifiers', () => {
    expect(resolveShortcutAction('?', '?', gate())).toBe('OPEN_HELP');
    expect(resolveShortcutAction('?', '?', gate({ typing: true }))).toBe('IGNORE');
    expect(resolveShortcutAction('?', '?', gate({ ctrl: true }))).toBe('IGNORE');
  });

  it('starts the G-chord on the physical KeyG regardless of layout key', () => {
    expect(resolveShortcutAction('g', 'g', gate({ code: 'KeyG' }))).toBe('CHORD_START');
    // Arabic keyboard produces a different key but same physical code.
    expect(resolveShortcutAction('ل', 'ل', gate({ code: 'KeyG' }))).toBe('CHORD_START');
  });

  it('resolves the second chord key and ignores typing/modifiers/panels otherwise', () => {
    expect(resolveShortcutAction('d', 'd', gate({ code: 'KeyD', chordWaiting: true }))).toBe('CHORD_RESOLVE');
    expect(resolveShortcutAction('d', 'd', gate({ code: 'KeyD', typing: true }))).toBe('IGNORE');
    expect(resolveShortcutAction('d', 'd', gate({ code: 'KeyD', alt: true }))).toBe('IGNORE');
    expect(resolveShortcutAction('x', 'x', gate({ code: 'KeyX' }))).toBe('IGNORE');
  });
});
