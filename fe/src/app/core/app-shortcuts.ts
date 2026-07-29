export interface AppShortcut {
  labelKey: string;
  keys: string;
}

export interface MenuShortcut extends AppShortcut {
  menuId: string;
  path: string;
  chordKey: string;
}

export const GLOBAL_SHORTCUTS: readonly AppShortcut[] = [
  { labelKey: 'shortcuts.openQuickNav', keys: 'Ctrl + K' },
  { labelKey: 'shortcuts.showHelp', keys: '?' },
  { labelKey: 'shortcuts.closeDialog', keys: 'Esc' },
  { labelKey: 'shortcuts.submitForm', keys: 'Enter' },
  { labelKey: 'shortcuts.nextField', keys: 'Tab' },
];

export const MENU_SHORTCUTS: readonly MenuShortcut[] = [
  { menuId: 'dashboard', labelKey: 'nav.dashboard', path: '/dashboard', chordKey: 'd', keys: 'G → D' },
  { menuId: 'employees', labelKey: 'nav.employees', path: '/employees', chordKey: 'e', keys: 'G → E' },
  { menuId: 'imports', labelKey: 'nav.imports', path: '/imports', chordKey: 'i', keys: 'G → I' },
  { menuId: 'reports', labelKey: 'nav.reports', path: '/reports', chordKey: 'r', keys: 'G → R' },
  { menuId: 'operations', labelKey: 'nav.operations', path: '/operations', chordKey: 'o', keys: 'G → O' },
  { menuId: 'procurement', labelKey: 'nav.procurement', path: '/trade/procurement', chordKey: 'p', keys: 'G → P' },
  { menuId: 'parties', labelKey: 'nav.parties', path: '/parties', chordKey: 'c', keys: 'G → C' },
  { menuId: 'settings', labelKey: 'nav.settings', path: '/settings', chordKey: 's', keys: 'G → S' },
];

export function shortcutForMenu(menuId: string): MenuShortcut | undefined {
  return MENU_SHORTCUTS.find((shortcut) => shortcut.menuId === menuId);
}

