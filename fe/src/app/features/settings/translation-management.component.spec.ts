import '@angular/compiler';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { vi } from 'vitest';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TranslationManagementComponent } from './translation-management.component';
import { AuthService } from '../../core/auth/auth.service';
import { I18nService } from '../../core/i18n.service';
import { NotificationService } from '../../core/notification.service';

const APPS = [
  { id: 'app-1', code: 'DEMO', name: 'Demo App', active: true },
  { id: 'app-2', code: 'CORE', name: 'Core ERP', active: true },
];

const DEFAULT_ROWS = [
  { key: 'common.save', defaultValue: 'حفظ', overrideValue: null, effectiveValue: 'حفظ', overridden: false },
  { key: 'nav.title', defaultValue: 'العنوان العام', overrideValue: null, effectiveValue: 'العنوان العام', overridden: false },
];

const APP_ROWS = [
  { key: 'common.save', defaultValue: 'حفظ', overrideValue: null, effectiveValue: 'حفظ', overridden: false },
  { key: 'nav.title', defaultValue: 'العنوان العام', overrideValue: 'عنوان العميل', effectiveValue: 'عنوان العميل', overridden: true },
];

describe('TranslationManagementComponent', () => {
  let component: TranslationManagementComponent;
  let fixture: ComponentFixture<TranslationManagementComponent>;
  let http: HttpTestingController;
  let notification: NotificationService;
  let i18n: I18nService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TranslationManagementComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { app: () => ({ id: 'app-1', code: 'DEMO', name: 'Demo App' }) } },
        {
          provide: I18nService,
          useValue: {
            locale: () => 'ar-EG',
            t: (key: string, _params?: unknown, fallback?: string) => fallback ?? key,
            invalidate: vi.fn(),
            use: vi.fn(() => Promise.resolve()),
          },
        },
        {
          provide: NotificationService,
          useValue: { success: vi.fn(), error: vi.fn() },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(TranslationManagementComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    notification = TestBed.inject(NotificationService);
    i18n = TestBed.inject(I18nService);

    flushInitialLoad();
    fixture.detectChanges();
  });

  afterEach(() => {
    http.verify();
  });

  function flushInitialLoad(): void {
    http.expectOne('/api/v1/i18n/admin/apps').flush(APPS);
    http.expectOne('/api/v1/i18n/admin/translations?locale=ar-EG').flush(DEFAULT_ROWS);
  }

  it('loads applications and rows for the current locale on init', () => {
    expect(component.apps()).toHaveLength(2);
    expect(component.rows().map((row) => row.key)).toEqual(['common.save', 'nav.title']);
    expect(component.rows().every((row) => !row.overridden)).toBe(true);
  });

  it('reloads rows when the language changes', () => {
    void component.changeLocale('en-US');

    http.expectOne('/api/v1/i18n/admin/translations?locale=en-US')
      .flush(DEFAULT_ROWS.map((row) => ({ ...row, effectiveValue: 'Save' })));

    expect(component.locale()).toBe('en-US');
  });

  it('reloads rows with the selected application scope', () => {
    void component.changeScope('app-1');

    http.expectOne('/api/v1/i18n/admin/translations?locale=ar-EG&appId=app-1').flush(APP_ROWS);

    expect(component.appId()).toBe('app-1');
    expect(component.rows().find((row) => row.key === 'nav.title')?.overridden).toBe(true);
  });

  it('filters rows by key or effective text', () => {
    component.search.set('العنوان');

    expect(component.filteredRows().map((row) => row.key)).toEqual(['nav.title']);
  });

  it('saves a key via PUT and refreshes the active bundle', async () => {
    const updated = { ...DEFAULT_ROWS[1], overrideValue: 'عنوان جديد', effectiveValue: 'عنوان جديد', overridden: true };
    const promise = component.save(component.rows()[1]);

    http.expectOne('/api/v1/i18n/admin/translations/nav.title').flush(updated);

    await promise;
    expect(component.rows()[1].effectiveValue).toBe('عنوان جديد');
    expect(notification.success).toHaveBeenCalledWith('translations.saved');
    expect(i18n.invalidate).toHaveBeenCalledWith('ar-EG', null);
    expect(i18n.invalidate).toHaveBeenCalledWith('ar-EG', 'app-1');
    expect(i18n.use).toHaveBeenCalledWith('ar-EG', 'app-1');
  });

  it('restores an application override via DELETE and re-exposes the default', async () => {
    component.appId.set('app-1');
    component.rows.set(APP_ROWS);
    const restored = { key: 'nav.title', defaultValue: 'العنوان العام', overrideValue: null, effectiveValue: 'العنوان العام', overridden: false };
    const promise = component.restore(component.rows()[1]);

    http.expectOne('/api/v1/i18n/admin/translations/nav.title?locale=ar-EG&appId=app-1').flush(restored);

    await promise;
    expect(component.rows()[1].overridden).toBe(false);
    expect(component.rows()[1].effectiveValue).toBe('العنوان العام');
    expect(notification.success).toHaveBeenCalledWith('translations.restored');
  });

  it('adds a new key and value from the form inputs', async () => {
    component.newKey.set('menu.example');
    component.newValue.set('مثال');
    const created = { key: 'menu.example', defaultValue: 'مثال', overrideValue: null, effectiveValue: 'مثال', overridden: false };
    const promise = component.add();

    http.expectOne('/api/v1/i18n/admin/translations/menu.example').flush(created);

    await promise;
    expect(component.newKey()).toBe('');
    expect(component.newValue()).toBe('');
    expect(component.rows().some((row) => row.key === 'menu.example')).toBe(true);
    expect(notification.success).toHaveBeenCalled();
  });

  it('surfaces load errors through the notification service', () => {
    component.search.set('');
    component.apps.set([]);

    void component.changeLocale('en-US');
    http.expectOne('/api/v1/i18n/admin/translations?locale=en-US')
      .error(new ProgressEvent('error'), { status: 500, statusText: 'Server Error' });

    expect(notification.error).toHaveBeenCalled();
  });
});
