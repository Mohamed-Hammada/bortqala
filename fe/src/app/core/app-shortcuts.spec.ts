import { describe, expect, it } from 'vitest';
import { DEFAULT_SCREEN_SHORTCUTS, GLOBAL_SHORTCUTS } from './app-shortcuts';

describe('application shortcuts', () => {
  it('has valid global shortcut definitions', () => {
    expect(GLOBAL_SHORTCUTS.length).toBeGreaterThan(0);
    expect(new Set(GLOBAL_SHORTCUTS.map((item) => item.labelKey)).size).toBe(GLOBAL_SHORTCUTS.length);
  });

  it('uses unique default page codes and second key codes', () => {
    expect(new Set(DEFAULT_SCREEN_SHORTCUTS.map((item) => item.pageCode)).size).toBe(DEFAULT_SCREEN_SHORTCUTS.length);
    expect(new Set(DEFAULT_SCREEN_SHORTCUTS.map((item) => item.secondKeyCode)).size).toBe(DEFAULT_SCREEN_SHORTCUTS.length);
  });
});
