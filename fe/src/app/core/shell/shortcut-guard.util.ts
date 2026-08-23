/**
 * Pure decision core for the global keyboard handler (app-shell BUG-1..BUG-5).
 *
 * The shell maps the returned action to its handlers; all ordering rules live
 * here so they can be unit-tested exhaustively without booting the shell.
 */
export interface ShortcutGate {
  /** Event target is an editable element (input/textarea/select/contenteditable). */
  typing: boolean;
  ctrl: boolean;
  meta: boolean;
  alt: boolean;
  /** Physical key code (event.code) — layout-independent chord matching. */
  code: string;
  quickNavOpen: boolean;
  shortcutHelpOpen: boolean;
  logoutOptionsOpen: boolean;
  /** At least one page-level modal dialog is open (DialogStateService). */
  modalOpen: boolean;
  chordWaiting: boolean;
}

export type ShortcutAction =
  | 'OPEN_QUICK_NAV'
  | 'OPEN_HELP'
  | 'ESCAPE_SHELL_PANEL'
  | 'CHORD_RESOLVE'
  | 'CHORD_START'
  | 'IGNORE';

export function resolveShortcutAction(
  key: string,
  lowerKey: string,
  gate: ShortcutGate,
): ShortcutAction {
  const modifier = gate.ctrl || gate.meta;

  // Ctrl/Cmd+K or Ctrl/Cmd+/ opens quick-nav — but never above a page modal.
  if (modifier && !gate.alt && (lowerKey === 'k' || lowerKey === '/')) {
    return gate.modalOpen ? 'IGNORE' : 'OPEN_QUICK_NAV';
  }

  if (key === 'Escape') {
    // A page modal owns Escape exclusively; the shell must not react (BUG-5).
    if (gate.modalOpen) return 'IGNORE';
    if (gate.quickNavOpen || gate.shortcutHelpOpen || gate.logoutOptionsOpen || gate.chordWaiting) {
      return 'ESCAPE_SHELL_PANEL';
    }
    return 'IGNORE';
  }

  // Everything below is suppressed while a page modal is open (BUG-1/BUG-2):
  // identical behaviour regardless of whether focus sits on a button or input.
  if (gate.modalOpen) return 'IGNORE';

  if (!gate.typing && !modifier && !gate.alt && key === '/') return 'OPEN_QUICK_NAV';
  if (!gate.typing && !modifier && !gate.alt && key === '?') return 'OPEN_HELP';

  if (
    gate.typing ||
    modifier ||
    gate.alt ||
    gate.quickNavOpen ||
    gate.shortcutHelpOpen ||
    gate.logoutOptionsOpen
  ) {
    return 'IGNORE';
  }

  if (gate.chordWaiting) return 'CHORD_RESOLVE';
  if (gate.code === 'KeyG') return 'CHORD_START';
  return 'IGNORE';
}
