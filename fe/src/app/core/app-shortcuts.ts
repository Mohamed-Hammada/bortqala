export interface AppShortcut {
  labelKey: string;
  keys: string;
}

export interface DefaultScreenShortcut {
  pageCode: string;
  secondKeyCode: string;
}

export const GLOBAL_SHORTCUTS: readonly AppShortcut[] = [
  { labelKey: 'shortcuts.openQuickNav', keys: 'Ctrl + K / /' },
  { labelKey: 'shortcuts.showHelp', keys: '?' },
  { labelKey: 'shortcuts.closeDialog', keys: 'Esc' },
  { labelKey: 'shortcuts.submitForm', keys: 'Enter' },
  { labelKey: 'shortcuts.nextField', keys: 'Tab' },
];

export const DEFAULT_SCREEN_SHORTCUTS: readonly DefaultScreenShortcut[] = [
  { pageCode: 'DASHBOARD', secondKeyCode: 'KeyD' },
  { pageCode: 'EMPLOYEES', secondKeyCode: 'KeyE' },
  { pageCode: 'IMPORTS', secondKeyCode: 'KeyI' },
  { pageCode: 'REPORTS', secondKeyCode: 'KeyR' },
  { pageCode: 'WORKFORCE_WORKERS', secondKeyCode: 'KeyW' },
  { pageCode: 'WORKFORCE_ATTENDANCE', secondKeyCode: 'KeyA' },
  { pageCode: 'PROCUREMENT', secondKeyCode: 'KeyP' },
  { pageCode: 'JOURNAL_ENTRIES', secondKeyCode: 'KeyJ' },
  { pageCode: 'USERS', secondKeyCode: 'KeyU' },
  { pageCode: 'SETTINGS', secondKeyCode: 'KeyS' },
];
