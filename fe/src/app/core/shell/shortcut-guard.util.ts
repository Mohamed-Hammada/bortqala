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
  paletteOpen?: boolean;
  quickNavOpen: boolean;
  shortcutHelpOpen: boolean;
  logoutOptionsOpen: boolean;
  /** At least one page-level modal dialog is open (DialogStateService). */
  modalOpen: boolean;
  chordWaiting: boolean;
}

export type ShortcutAction =
  | 'OPEN_QUICK_NAV'
  | 'OPEN_PALETTE'
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

  // Suppress all global shortcuts while a page modal is open (BUG-1/BUG-2/SHORTCUT-001)
  if (gate.modalOpen) return 'IGNORE';

  // Ctrl/Cmd+K opens/toggles the command palette — never while typing in inputs
  if (modifier && !gate.alt && lowerKey === 'k') {
    return gate.typing ? 'IGNORE' : 'OPEN_PALETTE';
  }

  // Ctrl/Cmd+/ opens quick-nav
  if (modifier && !gate.alt && lowerKey === '/') {
    return 'OPEN_QUICK_NAV';
  }

  if (key === 'Escape') {
    // A page modal owns Escape exclusively; the shell must not react (BUG-5).
    if (gate.modalOpen) return 'IGNORE';
    if (
      gate.paletteOpen ||
      gate.quickNavOpen ||
      gate.shortcutHelpOpen ||
      gate.logoutOptionsOpen ||
      gate.chordWaiting
    ) {
      return 'ESCAPE_SHELL_PANEL';
    }
    return 'IGNORE';
  }

  // Everything below is suppressed while typing or with active modifiers
  if (!gate.typing && !modifier && !gate.alt && key === '/') return 'OPEN_QUICK_NAV';
  if (!gate.typing && !modifier && !gate.alt && key === '?') return 'OPEN_HELP';

  if (
    gate.typing ||
    modifier ||
    gate.alt ||
    gate.paletteOpen ||
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
