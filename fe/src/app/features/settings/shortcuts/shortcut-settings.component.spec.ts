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

  it('addShortcut appends a draft row with first unassigned destination', () => {
    component.addShortcut();

    expect(component.drafts()).toHaveLength(2);
    expect(component.drafts()[1].pageCode).toBe('EMPLOYEES');
    expect(component.drafts()[1].clientId).toContain('new-');
  });

  it('wires the Add button and gives feedback instead of becoming a silent no-op', () => {
    const addButton = fixture.nativeElement.querySelector(
      '[data-testid="shortcut-add"]',
    ) as HTMLButtonElement;

    expect(addButton.disabled).toBe(false);
    addButton.click();
    fixture.detectChanges();
    expect(component.drafts()).toHaveLength(2);

    const warningSpy = vi.spyOn(component.notification, 'warning').mockReturnValue(
      'toast-test',
    );
    const addButtonAfterAllDestinationsUsed = fixture.nativeElement.querySelector(
      '[data-testid="shortcut-add"]',
    ) as HTMLButtonElement;

    expect(addButtonAfterAllDestinationsUsed.disabled).toBe(false);
    addButtonAfterAllDestinationsUsed.click();

    expect(component.drafts()).toHaveLength(2);
    expect(warningSpy).toHaveBeenCalled();
    expect(component.liveAnnouncement()).not.toBe('');
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
    expect(component.drafts()[0].pageCode).toBe('EMPLOYEES');

    const select = fixture.nativeElement.querySelector(
      '.shortcut-destination-select',
    ) as HTMLSelectElement;
    expect(select.value).toBe('EMPLOYEES');
    expect(select.selectedOptions[0]?.value).toBe('EMPLOYEES');
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

  it('disables destinations already assigned to another shortcut', () => {
    component.addShortcut();
    fixture.detectChanges();

    const selects = fixture.nativeElement.querySelectorAll(
      '.shortcut-destination-select',
    ) as NodeListOf<HTMLSelectElement>;
    const employeesInFirstRow = Array.from(selects[0].options).find(
      (option) => option.value === 'EMPLOYEES',
    );
    const dashboardInSecondRow = Array.from(selects[1].options).find(
      (option) => option.value === 'DASHBOARD',
    );

    expect(employeesInFirstRow?.disabled).toBe(true);
    expect(dashboardInSecondRow?.disabled).toBe(true);
  });

  it('reverts the select when the new destination is already assigned elsewhere', () => {
    component.addShortcut();
    fixture.detectChanges();

    const selects = fixture.nativeElement.querySelectorAll(
      '.shortcut-destination-select',
    ) as NodeListOf<HTMLSelectElement>;
    selects[0].value = 'EMPLOYEES';
    selects[0].dispatchEvent(new Event('change', { bubbles: true }));

    expect(component.drafts()[0].pageCode).toBe('DASHBOARD');
    expect(selects[0].value).toBe('DASHBOARD');
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
