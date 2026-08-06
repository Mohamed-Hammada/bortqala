import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
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

  it('should create and initialize drafts from profile', () => {
    expect(component).toBeTruthy();
    expect(component.drafts()).toHaveLength(1);
    expect(component.drafts()[0].pageCode).toBe('DASHBOARD');
  });

  it('captureKey stores valid physical KeyboardEvent code', () => {
    const event = new KeyboardEvent('keydown', { code: 'KeyH', key: 'h' });
    component.captureKey(0, event);

    expect(component.drafts()[0].secondKeyCode).toBe('KeyH');
  });

  it('addShortcut appends a draft row with first unassigned destination', () => {
    component.addShortcut();

    expect(component.drafts()).toHaveLength(2);
    expect(component.drafts()[1].pageCode).toBe('EMPLOYEES');
  });

  it('remove deletes a draft row', () => {
    component.remove(0);

    expect(component.drafts()).toHaveLength(0);
  });
});
