import { describe, expect, it } from 'vitest';
import { MENU_SHORTCUTS, shortcutForMenu } from './app-shortcuts';

describe('application shortcuts', () => {
  it('uses unique menu ids, chord keys and paths', () => {
    expect(new Set(MENU_SHORTCUTS.map((item) => item.menuId)).size).toBe(MENU_SHORTCUTS.length);
    expect(new Set(MENU_SHORTCUTS.map((item) => item.chordKey)).size).toBe(MENU_SHORTCUTS.length);
    expect(new Set(MENU_SHORTCUTS.map((item) => item.path)).size).toBe(MENU_SHORTCUTS.length);
  });

  it('finds the procurement shortcut', () => {
    expect(shortcutForMenu('procurement')).toMatchObject({ path: '/trade/procurement', keys: 'G → P' });
  });
});
