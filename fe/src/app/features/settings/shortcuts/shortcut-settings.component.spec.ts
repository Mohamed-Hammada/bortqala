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

    const selects = fixture.nativeElement.querySelectorAll(
      '.shortcut-destination-select',
    ) as NodeListOf<HTMLSelectElement>;

    // The newly added shortcut is the first row and keeps EMPLOYEES selected.
    expect(selects[0].value).toBe('EMPLOYEES');
    expect(
      Array.from(selects[0].options).some((option) => option.value === 'DASHBOARD'),
    ).toBe(false);

    // The existing shortcut keeps DASHBOARD selected and does not offer EMPLOYEES.
    expect(selects[1].value).toBe('DASHBOARD');
    expect(
      Array.from(selects[1].options).some((option) => option.value === 'EMPLOYEES'),
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
});
