import { ComponentFixture, TestBed } from '@angular/core/testing';
import { vi } from 'vitest';
import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { ShortcutSettingsComponent } from './shortcut-settings.component';
import { ScreenShortcutService } from '../../../core/shortcuts/screen-shortcut.service';
import { ScreenShortcutProfile } from '../../../core/shortcuts/screen-shortcut.models';

const MOCK_PROFILE: ScreenShortcutProfile = {
  profileMode: 'DEFAULT',
  version: 0,
  shortcuts: [
    {
      id: 'sc-1',
      pageCode: 'DASHBOARD',
      menuId: 'dashboard',
      route: '/dashboard',
      titleKey: 'nav.dashboard',
      secondKeyCode: 'KeyD',
      displayKey: 'D',
      enabled: true,
      defaultShortcut: true,
      availabilityStatus: 'AVAILABLE',
      unavailableReasonKey: null,
    },
  ],
  availableDestinations: [
    {
      pageCode: 'DASHBOARD',
      menuId: 'dashboard',
      route: '/dashboard',
      titleKey: 'nav.dashboard',
      module: 'DASHBOARD',
      requiredFeature: null,
    },
    {
      pageCode: 'EMPLOYEES',
      menuId: 'employees',
      route: '/employees',
      titleKey: 'nav.employees',
      module: 'HR',
      requiredFeature: null,
    },
  ],
  updatedAt: '2026-08-06T12:00:00Z',
};

describe('ShortcutSettingsComponent', () => {
  let component: ShortcutSettingsComponent;
  let fixture: ComponentFixture<ShortcutSettingsComponent>;
  let shortcutService: ScreenShortcutService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ShortcutSettingsComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    shortcutService = TestBed.inject(ScreenShortcutService);
    shortcutService.profile.set(MOCK_PROFILE);

    fixture = TestBed.createComponent(ShortcutSettingsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and initialize drafts from profile with stable client ids', () => {
    expect(component).toBeTruthy();
    expect(component.drafts()).toHaveLength(1);
    expect(component.drafts()[0].pageCode).toBe('DASHBOARD');
    expect(component.drafts()[0].clientId).toBe('saved-sc-1');
  });

  it('captureKey stores valid physical KeyboardEvent code', () => {
    const event = new KeyboardEvent('keydown', { code: 'KeyH', key: 'h' });
    component.captureKey(0, event);

    expect(component.drafts()[0].secondKeyCode).toBe('KeyH');
  });

  it('wires the capture button and keyboard handler', () => {
    component.beginEdit(0);
    fixture.detectChanges();

    const captureButton = fixture.nativeElement.querySelector(
      '[data-shortcut-capture="true"]',
    ) as HTMLButtonElement;

    captureButton.click();
    fixture.detectChanges();
    expect(component.captureIndex()).toBe(0);

    captureButton.dispatchEvent(
      new KeyboardEvent('keydown', { code: 'KeyH', key: 'h', bubbles: true }),
    );
    expect(component.drafts()[0].secondKeyCode).toBe('KeyH');
    expect(component.captureIndex()).toBeNull();
  });

  it('addShortcut prepends a draft row with first unassigned destination', () => {
    component.addShortcut();

    expect(component.drafts()).toHaveLength(2);
    expect(component.drafts()[0].pageCode).toBe('EMPLOYEES');
    expect(component.drafts()[0].clientId).toContain('new-');
    expect(component.drafts()[1].pageCode).toBe('DASHBOARD');

    fixture.detectChanges();
    const firstRowSelect = fixture.nativeElement.querySelector(
      'tbody tr:first-child .shortcut-destination-select',
    ) as HTMLSelectElement;
    expect(firstRowSelect.value).toBe('EMPLOYEES');
  });

  it('exposes only unused pages as remaining destinations for Add', () => {
    expect(component.remainingDestinations().map((item) => item.pageCode)).toEqual([
      'EMPLOYEES',
    ]);

    component.addShortcut();

    expect(component.remainingDestinations()).toEqual([]);
    expect(component.drafts().map((item) => item.pageCode)).toEqual([
      'EMPLOYEES',
      'DASHBOARD',
    ]);
  });

  it('creates a new shortcut only from a remaining menu and remaining key', () => {
    expect(component.remainingDestinations()[0]?.pageCode).toBe('EMPLOYEES');
    expect(component.remainingShortcutKeys()[0]).toBe('KeyA');

    component.addShortcut();

    expect(component.drafts()[0].pageCode).toBe('EMPLOYEES');
    expect(component.drafts()[0].secondKeyCode).toBe('KeyA');
    expect(
      component.drafts().filter((item) => item.pageCode === 'EMPLOYEES'),
    ).toHaveLength(1);
    expect(
      component.drafts().filter((item) => item.secondKeyCode === 'KeyA'),
    ).toHaveLength(1);
  });

  it('checks unsaved UI rows before adding the next shortcut', () => {
    shortcutService.profile.set({
      ...MOCK_PROFILE,
      availableDestinations: [
        ...MOCK_PROFILE.availableDestinations,
        {
          pageCode: 'SUPPLIERS',
          menuId: 'suppliers',
          route: '/suppliers',
          titleKey: 'nav.suppliers',
          module: 'PROCUREMENT',
          requiredFeature: null,
        },
        {
          pageCode: 'PAYROLL',
          menuId: 'payroll',
          route: '/payroll',
          titleKey: 'nav.payroll',
          module: 'PAYROLL',
          requiredFeature: null,
        },
      ],
    });

    // First unsaved row uses EMPLOYEES + KeyA.
    component.addShortcut();
    expect(component.drafts()[0].pageCode).toBe('EMPLOYEES');
    expect(component.drafts()[0].secondKeyCode).toBe('KeyA');

    // Without saving, Add again. It must see the first unsaved UI row and
    // choose different values.
    component.addShortcut();

    expect(component.drafts()[0].pageCode).toBe('SUPPLIERS');
    expect(component.drafts()[0].secondKeyCode).toBe('KeyB');

    const targetCodes = component.drafts().map((item) => item.pageCode);
    const shortcutKeys = component.drafts().map((item) => item.secondKeyCode);

    expect(new Set(targetCodes).size).toBe(targetCodes.length);
    expect(new Set(shortcutKeys).size).toBe(shortcutKeys.length);

    expect(component.usedTargetCodes()).toEqual(
      new Set(['DASHBOARD', 'EMPLOYEES', 'SUPPLIERS']),
    );
    expect(component.usedShortcutKeys()).toEqual(
      new Set(['KeyD', 'KeyA', 'KeyB']),
    );
  });

  it('does not add a duplicate when there are no remaining menus', () => {
    component.addShortcut();
    const pagesAfterFirstAdd = component.drafts().map((item) => item.pageCode);

    component.addShortcut();

    expect(component.drafts().map((item) => item.pageCode)).toEqual(
      pagesAfterFirstAdd,
    );
    expect(new Set(component.drafts().map((item) => item.pageCode)).size).toBe(
      component.drafts().length,
    );
  });

  it('makes a newly added shortcut visible and keeps its target selected', () => {
    component.addShortcut();
    fixture.detectChanges();

    const addedClientId = component.lastAddedClientId();
    expect(addedClientId).toBeTruthy();

    const addedRow = fixture.nativeElement.querySelector(
      `[data-shortcut-client-id="${addedClientId}"]`,
    ) as HTMLTableRowElement;
    expect(addedRow).toBeTruthy();
    expect(addedRow.classList.contains('newly-added')).toBe(true);

    const select = addedRow.querySelector(
      '.shortcut-destination-select',
    ) as HTMLSelectElement;
    expect(select.value).toBe('EMPLOYEES');
    expect(select.selectedOptions[0]?.value).toBe('EMPLOYEES');
  });

  it('disables Add before the user can attempt to create a duplicate destination', () => {
    const addButton = fixture.nativeElement.querySelector(
      '[data-testid="shortcut-add"]',
    ) as HTMLButtonElement;

    expect(component.canAddShortcut()).toBe(true);
    expect(addButton.disabled).toBe(false);

    addButton.click();
    fixture.detectChanges();

    expect(component.drafts()).toHaveLength(2);
    expect(component.remainingDestinations()).toEqual([]);
    expect(component.canAddShortcut()).toBe(false);

    const addButtonAfterAllDestinationsUsed = fixture.nativeElement.querySelector(
      '[data-testid="shortcut-add"]',
    ) as HTMLButtonElement;

    expect(addButtonAfterAllDestinationsUsed.disabled).toBe(true);
  });

  it('remove deletes a draft row', () => {
    component.remove(0);
    expect(component.drafts()).toHaveLength(0);
  });

  it('wires the Remove button', () => {
    const removeButton = fixture.nativeElement.querySelector(
      '[data-testid="shortcut-remove"]',
    ) as HTMLButtonElement;

    removeButton.click();
    expect(component.drafts()).toHaveLength(0);
  });

  it('wires the enabled checkbox', () => {
    const checkbox = fixture.nativeElement.querySelector(
      '.shortcut-enabled-cell input[type="checkbox"]',
    ) as HTMLInputElement;

    checkbox.checked = false;
    checkbox.dispatchEvent(new Event('change', { bubbles: true }));

    expect(component.drafts()[0].enabled).toBe(false);
  });

  it('wires Save and Reset toolbar buttons', () => {
    const saveSpy = vi.spyOn(component, 'save').mockResolvedValue(undefined);
    const resetSpy = vi.spyOn(component, 'reset').mockResolvedValue(undefined);

    const saveButton = fixture.nativeElement.querySelector(
      '[data-testid="shortcut-save"]',
    ) as HTMLButtonElement;
    const resetButton = fixture.nativeElement.querySelector(
      '[data-testid="shortcut-reset"]',
    ) as HTMLButtonElement;

    saveButton.click();
    resetButton.click();

    expect(saveSpy).toHaveBeenCalledTimes(1);
    expect(resetSpy).toHaveBeenCalledTimes(1);
  });

  it('renders the destination select with the draft page selected', () => {
    component.beginEdit(0);
    fixture.detectChanges();

    const select = fixture.nativeElement.querySelector(
      '.shortcut-destination-select',
    ) as HTMLSelectElement;

    expect(select).toBeTruthy();
    expect(select.value).toBe('DASHBOARD');
    expect(select.selectedOptions[0]?.value).toBe('DASHBOARD');
  });

  it('renders the destination select with the draft page selected after async profile load', async () => {
    shortcutService.profile.set(null);
    const http = TestBed.inject(HttpTestingController);
    const fixture2 = TestBed.createComponent(ShortcutSettingsComponent);
    const component2 = fixture2.componentInstance;
    fixture2.detectChanges();

    const req = http.expectOne('/api/v1/auth/preferences/shortcuts');
    req.flush(MOCK_PROFILE);
    await new Promise((resolve) => setTimeout(resolve, 0));
    await fixture2.whenStable();
    fixture2.detectChanges();

    expect(component2.drafts()).toHaveLength(1);
    component2.beginEdit(0);
    fixture2.detectChanges();

    const selects = fixture2.nativeElement.querySelectorAll(
      '.shortcut-destination-select',
    ) as NodeListOf<HTMLSelectElement>;
    expect(selects.length).toBe(1);
    expect(selects[0].value).toBe('DASHBOARD');
    expect(selects[0].selectedOptions[0]?.value).toBe('DASHBOARD');
    http.verify();
    fixture2.destroy();
  });

  it('keeps the select in sync when the user picks a different destination', () => {
    component.beginEdit(0);
    fixture.detectChanges();

    const select = fixture.nativeElement.querySelector(
      '.shortcut-destination-select',
    ) as HTMLSelectElement;
    select.value = 'EMPLOYEES';
    select.dispatchEvent(new Event('change', { bubbles: true }));

    expect(component.drafts()[0].pageCode).toBe('EMPLOYEES');

    fixture.detectChanges();
    const renderedSelect = fixture.nativeElement.querySelector(
      '.shortcut-destination-select',
    ) as HTMLSelectElement;
    expect(renderedSelect.value).toBe('EMPLOYEES');
    expect(renderedSelect.selectedOptions[0]?.value).toBe('EMPLOYEES');
  });

  it('keeps the correct target selected after removing an earlier table row', () => {
    component.addShortcut();
    fixture.detectChanges();

    component.remove(0);
    fixture.detectChanges();

    expect(component.drafts()).toHaveLength(1);
    expect(component.drafts()[0].pageCode).toBe('DASHBOARD');

    component.beginEdit(0);
    fixture.detectChanges();

    const select = fixture.nativeElement.querySelector(
      '.shortcut-destination-select',
    ) as HTMLSelectElement;
    expect(select.value).toBe('DASHBOARD');
    expect(select.selectedOptions[0]?.value).toBe('DASHBOARD');
  });

  it('renders the current destination even when the page is not in the available list', () => {
    const withUnavailable: ScreenShortcutProfile = {
      ...MOCK_PROFILE,
      shortcuts: [
        {
          ...MOCK_PROFILE.shortcuts[0],
          pageCode: 'REMOVED_SCREEN',
          menuId: 'removed-screen',
          route: '',
          titleKey: 'shortcuts.pageRemoved',
          availabilityStatus: 'PAGE_REMOVED',
          unavailableReasonKey: 'shortcuts.pageRemoved',
        },
      ],
      availableDestinations: [
        {
          pageCode: 'DASHBOARD',
          menuId: 'dashboard',
          route: '/dashboard',
          titleKey: 'nav.dashboard',
          module: 'DASHBOARD',
          requiredFeature: null,
        },
      ],
    };
    shortcutService.profile.set(withUnavailable);
    component.loadDraftsFromProfile();
    fixture.detectChanges();

    component.beginEdit(0);
    fixture.detectChanges();

    const select = fixture.nativeElement.querySelector(
      '.shortcut-destination-select',
    ) as HTMLSelectElement;
    expect(select).toBeTruthy();
    expect(select.value).toBe('REMOVED_SCREEN');
    expect(select.selectedOptions[0]?.value).toBe('REMOVED_SCREEN');

    const removedOption = Array.from(select.options).find(
      (option) => option.value === 'REMOVED_SCREEN',
    );
    expect(removedOption?.disabled).toBe(true);
  });

  it('shows only the current destination and destinations not assigned elsewhere', () => {
    component.addShortcut();
    fixture.detectChanges();

    // The newly added shortcut is the only row in edit mode.
    let selects = fixture.nativeElement.querySelectorAll(
      '.shortcut-destination-select',
    ) as NodeListOf<HTMLSelectElement>;
    expect(selects).toHaveLength(1);
    expect(selects[0].value).toBe('EMPLOYEES');
    expect(
      Array.from(selects[0].options).some((option) => option.value === 'DASHBOARD'),
    ).toBe(false);

    // Edit the existing shortcut: the previous row collapses to compact view.
    component.beginEdit(1);
    fixture.detectChanges();
    selects = fixture.nativeElement.querySelectorAll(
      '.shortcut-destination-select',
    ) as NodeListOf<HTMLSelectElement>;
    expect(selects).toHaveLength(1);
    expect(selects[0].value).toBe('DASHBOARD');
    expect(
      Array.from(selects[0].options).some((option) => option.value === 'EMPLOYEES'),
    ).toBe(false);
  });

  it('reverts the select when the new destination is already assigned elsewhere', () => {
    component.addShortcut();
    fixture.detectChanges();

    const selects = fixture.nativeElement.querySelectorAll(
      '.shortcut-destination-select',
    ) as NodeListOf<HTMLSelectElement>;
    selects[0].value = 'DASHBOARD';
    selects[0].dispatchEvent(new Event('change', { bubbles: true }));

    expect(component.drafts()[0].pageCode).toBe('EMPLOYEES');
    expect(selects[0].value).toBe('EMPLOYEES');
  });

  it('reports an error instead of silently ignoring Save when no profile is loaded', async () => {
    const errorSpy = vi.spyOn(component.notification, 'error').mockReturnValue(
      'toast-test',
    );
    shortcutService.profile.set(null);

    await component.save();

    expect(errorSpy).toHaveBeenCalled();
    expect(component.liveAnnouncement()).not.toBe('');
  });

  it('default view is compact: target title is visible without the selector', () => {
    fixture.detectChanges();

    expect(
      fixture.nativeElement.querySelector('.shortcut-destination-select'),
    ).toBeNull();
    const title = fixture.nativeElement.querySelector(
      '.shortcut-dest-title',
    ) as HTMLElement;
    expect(title.textContent).toContain('dashboard');
    expect(
      fixture.nativeElement.querySelector('[data-testid="shortcut-edit"]'),
    ).toBeTruthy();
  });

  it('editing one row does not expand every other row', () => {
    component.addShortcut();
    fixture.detectChanges();

    // Only the newly added row is in edit mode.
    let selects = fixture.nativeElement.querySelectorAll(
      '.shortcut-destination-select',
    ) as NodeListOf<HTMLSelectElement>;
    let compactTitles = fixture.nativeElement.querySelectorAll(
      '.shortcut-dest-title',
    ) as NodeListOf<HTMLElement>;
    expect(selects).toHaveLength(1);
    expect(compactTitles).toHaveLength(1);

    // Switch editing to the other row: it expands while the first collapses.
    component.beginEdit(1);
    fixture.detectChanges();
    selects = fixture.nativeElement.querySelectorAll(
      '.shortcut-destination-select',
    ) as NodeListOf<HTMLSelectElement>;
    compactTitles = fixture.nativeElement.querySelectorAll(
      '.shortcut-dest-title',
    ) as NodeListOf<HTMLElement>;
    expect(selects).toHaveLength(1);
    expect(selects[0].value).toBe('DASHBOARD');
    expect(compactTitles).toHaveLength(1);
  });

  it('row save exits edit mode only after validation passes', () => {
    component.beginEdit(0);
    fixture.detectChanges();

    component.saveEdit(0);
    expect(component.editingClientId()).toBeNull();
    expect(component.liveAnnouncement()).not.toBe('');
  });

  it('row save rejects duplicate keys and stays in edit mode', () => {
    component.addShortcut();
    fixture.detectChanges();
    // Row 0 is EMPLOYEES/KeyA in edit mode; Row 1 is DASHBOARD/KeyD.
    component.drafts.update((items) => [
      { ...items[0], secondKeyCode: 'KeyD' },
      items[1],
    ]);

    component.saveEdit(0);

    expect(component.editingClientId()).toBe(component.drafts()[0].clientId);
    expect(component.drafts()[0].secondKeyCode).toBe('KeyD');
  });

  it('cancel reverts the row to its pre-edit snapshot', () => {
    component.beginEdit(0);
    fixture.detectChanges();

    const select = fixture.nativeElement.querySelector(
      '.shortcut-destination-select',
    ) as HTMLSelectElement;
    select.value = 'EMPLOYEES';
    select.dispatchEvent(new Event('change', { bubbles: true }));

    expect(component.drafts()[0].pageCode).toBe('EMPLOYEES');

    component.cancelEdit();

    expect(component.drafts()[0].pageCode).toBe('DASHBOARD');
    expect(component.editingClientId()).toBeNull();
  });

  it('search filters rows by target and key', () => {
    component.addShortcut();
    fixture.detectChanges();

    component.searchQuery.set('EMPLOYEES');
    expect(component.filteredDrafts()).toHaveLength(1);
    expect(component.filteredDrafts()[0].draft.pageCode).toBe('EMPLOYEES');

    component.searchQuery.set('KeyD');
    expect(component.filteredDrafts()).toHaveLength(1);
    expect(component.filteredDrafts()[0].draft.pageCode).toBe('DASHBOARD');

    component.searchQuery.set('zzz-no-match');
    expect(component.filteredDrafts()).toHaveLength(0);
  });

  it('exits edit mode when the search no longer shows the edited row', () => {
    component.addShortcut();
    fixture.detectChanges();
    component.beginEdit(1);

    component.searchQuery.set('EMPLOYEES');
    component.onSearchChange();

    expect(component.editingClientId()).toBeNull();
  });
});
