import { TestBed } from '@angular/core/testing';
import { ApplicationRef } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ShortcutSettingsComponent } from './features/settings/shortcuts/shortcut-settings.component';
import { ScreenShortcutService } from './core/shortcuts/screen-shortcut.service';
import { ScreenShortcutProfile } from './core/shortcuts/screen-shortcut.models';

const MOCK_PROFILE: ScreenShortcutProfile = {
  profileMode: 'DEFAULT', version: 0, updatedAt: '2026-08-06T12:00:00Z',
  shortcuts: [{ id: 'sc-1', pageCode: 'DASHBOARD', menuId: 'dashboard', route: '/dashboard', titleKey: 'nav.dashboard', secondKeyCode: 'KeyD', displayKey: 'D', enabled: true, defaultShortcut: true, availabilityStatus: 'AVAILABLE', unavailableReasonKey: null }],
  availableDestinations: [
    { pageCode: 'DASHBOARD', menuId: 'dashboard', route: '/dashboard', titleKey: 'nav.dashboard', module: 'DASHBOARD', requiredFeature: null },
    { pageCode: 'EMPLOYEES', menuId: 'employees', route: '/employees', titleKey: 'nav.employees', module: 'HR', requiredFeature: null },
  ],
};

describe('debug', () => {
  it('debug select value after addShortcut', async () => {
    await TestBed.configureTestingModule({
      imports: [ShortcutSettingsComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    const svc = TestBed.inject(ScreenShortcutService);
    svc.profile.set(MOCK_PROFILE);
    const fixture = TestBed.createComponent(ShortcutSettingsComponent);
    fixture.detectChanges();
    const c = fixture.componentInstance;
    c.addShortcut();
    fixture.detectChanges();
    const rows = fixture.nativeElement.querySelectorAll('tbody tr') as NodeListOf<HTMLTableRowElement>;
    let out = rows.length + ' rows\n';
    rows.forEach((r, i) => {
      const sel = r.querySelector('.shortcut-destination-select') as HTMLSelectElement;
      out += `row ${i}: value=${JSON.stringify(sel?.value)} opts=${Array.from(sel?.options??[]).map(o=>o.value+'['+(o.disabled?'D':'')+']').join(',')} idx=${sel?.selectedIndex}\n`;
    });
    TestBed.inject(ApplicationRef).tick();
    fixture.detectChanges();
    const rowsB = fixture.nativeElement.querySelectorAll('tbody tr') as NodeListOf<HTMLTableRowElement>;
    out += ' afterTick\n';
    rowsB.forEach((r, i) => {
      const sel = r.querySelector('.shortcut-destination-select') as HTMLSelectElement;
      out += `row ${i}: value=${JSON.stringify(sel?.value)} idx=${sel?.selectedIndex}\n`;
    });
    expect(out).toBe('__FAIL__');
  });
});
